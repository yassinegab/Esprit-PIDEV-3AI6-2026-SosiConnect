package org.example.aideEtdon.service;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class GeoLocationService {

    public static class Coordinate {
        public double lat;
        public double lng;
        
        public Coordinate(double lat, double lng) {
            this.lat = lat;
            this.lng = lng;
        }
    }

    /**
     * Fetches real-world geographic coordinates.
     * 1) Tries Windows Location API (WiFi/GPS — high accuracy)
     * 2) Falls back to IP geolocation providers
     * 3) Defaults to Tunis center if all fail
     */
    public static Coordinate fetchUserLocation() {
        // 1. Try Windows Location API (accurate — uses WiFi/GPS)
        Coordinate result = tryWindowsLocationApi();
        if (result != null) return result;

        // 2. Fallback: IP geolocation
        result = tryProvider("https://ipwho.is/", "latitude", "longitude", "success");
        if (result != null) return result;

        result = tryProvider("http://ip-api.com/json/", "lat", "lon", null);
        if (result != null) return result;

        System.err.println("All geolocation providers failed, using default coordinates.");
        return new Coordinate(36.8065, 10.1815);
    }

    /**
     * Uses the Windows Location API via PowerShell to get accurate GPS/WiFi coordinates.
     * Requires Windows 10/11 with Location Services enabled.
     */
    private static Coordinate tryWindowsLocationApi() {
        File scriptFile = null;
        try {
            // Write PowerShell script to a temp file to avoid $ escaping issues
            scriptFile = File.createTempFile("sosiconnect_geo", ".ps1");
            scriptFile.deleteOnExit();
            try (FileWriter fw = new FileWriter(scriptFile)) {
                fw.write("Add-Type -AssemblyName System.Device\n");
                fw.write("$w = New-Object System.Device.Location.GeoCoordinateWatcher(1)\n");
                fw.write("$w.Start()\n");
                fw.write("$t = 0\n");
                fw.write("while (($w.Status -ne 'Ready') -and ($t -lt 30)) { Start-Sleep -Milliseconds 500; $t++ }\n");
                fw.write("if (-not $w.Position.Location.IsUnknown) {\n");
                fw.write("    Write-Output ('' + $w.Position.Location.Latitude + ',' + $w.Position.Location.Longitude)\n");
                fw.write("} else {\n");
                fw.write("    Write-Output 'UNKNOWN'\n");
                fw.write("}\n");
                fw.write("$w.Stop()\n");
            }

            ProcessBuilder pb = new ProcessBuilder(
                "powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", scriptFile.getAbsolutePath());
            pb.redirectErrorStream(true);
            Process proc = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line.trim());
            }
            proc.waitFor();
            reader.close();

            String result = output.toString().trim();
            System.out.println("Windows Location API raw result: " + result);

            if (result != null && !result.isEmpty() && !result.equals("UNKNOWN") && result.contains(",")) {
                String[] parts = result.split(",");
                double lat = Double.parseDouble(parts[0].trim());
                double lng = Double.parseDouble(parts[1].trim());
                System.out.println("Windows Location API resolved: " + lat + ", " + lng);
                return new Coordinate(lat, lng);
            }
        } catch (Exception e) {
            System.err.println("Windows Location API failed: " + e.getMessage());
        } finally {
            if (scriptFile != null) scriptFile.delete();
        }
        return null;
    }

    private static Coordinate tryProvider(String apiUrl, String latField, String lngField, String successField) {
        try {
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "SosiConnect/1.0");

            if (conn.getResponseCode() == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                br.close();

                JSONObject json = new JSONObject(sb.toString());

                // Check success field if provided
                if (successField != null) {
                    Object status = json.opt(successField);
                    if (status instanceof Boolean && !(Boolean) status) return null;
                    if (status instanceof String && !"success".equals(status)) return null;
                }

                if (json.has(latField) && json.has(lngField)) {
                    double lat = json.getDouble(latField);
                    double lng = json.getDouble(lngField);
                    System.out.println("GeoLocation resolved: " + lat + ", " + lng + " via " + apiUrl);
                    return new Coordinate(lat, lng);
                }
            }
        } catch (Exception e) {
            System.err.println("GeoLocation provider " + apiUrl + " failed: " + e.getMessage());
        }
        return null;
    }
}
