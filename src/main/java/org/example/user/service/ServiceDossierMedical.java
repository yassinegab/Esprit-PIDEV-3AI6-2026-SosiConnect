package org.example.user.service;

import org.example.user.model.DossierMedical;
import org.example.utils.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class ServiceDossierMedical {

    public DossierMedical findByUserId(int userId) throws SQLException {
        String sql = "SELECT * FROM dossier_medical WHERE user_id = ?";

        try (Connection cnx = MyConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToDossier(rs);
            }
        }

        return null;
    }

    public void add(DossierMedical dossier) throws SQLException {
        String sql = "INSERT INTO dossier_medical " +
                "(user_id, antecedentsMedicaux, maladiesChroniques, allergies, traitementsEnCours, diagnostics, notesMedecin, objectifSante, niveauActivite) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection cnx = MyConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setInt(1, dossier.getUserId());
            ps.setString(2, dossier.getAntecedentsMedicaux());
            ps.setString(3, dossier.getMaladiesChroniques());
            ps.setString(4, dossier.getAllergies());
            ps.setString(5, dossier.getTraitementsEnCours());
            ps.setString(6, dossier.getDiagnostics());
            ps.setString(7, dossier.getNotesMedecin());
            ps.setString(8, dossier.getObjectifSante());
            ps.setString(9, dossier.getNiveauActivite());

            ps.executeUpdate();
        }
    }

    public void update(DossierMedical dossier) throws SQLException {
        String sql = "UPDATE dossier_medical SET " +
                "antecedentsMedicaux = ?, " +
                "maladiesChroniques = ?, " +
                "allergies = ?, " +
                "traitementsEnCours = ?, " +
                "diagnostics = ?, " +
                "notesMedecin = ?, " +
                "objectifSante = ?, " +
                "niveauActivite = ? " +
                "WHERE id = ?";

        try (Connection cnx = MyConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setString(1, dossier.getAntecedentsMedicaux());
            ps.setString(2, dossier.getMaladiesChroniques());
            ps.setString(3, dossier.getAllergies());
            ps.setString(4, dossier.getTraitementsEnCours());
            ps.setString(5, dossier.getDiagnostics());
            ps.setString(6, dossier.getNotesMedecin());
            ps.setString(7, dossier.getObjectifSante());
            ps.setString(8, dossier.getNiveauActivite());
            ps.setInt(9, dossier.getId());

            ps.executeUpdate();
        }
    }

    public void delete(int dossierId) throws SQLException {
        String sql = "DELETE FROM dossier_medical WHERE id = ?";

        try (Connection cnx = MyConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setInt(1, dossierId);
            ps.executeUpdate();
        }
    }

    public boolean existsForUser(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM dossier_medical WHERE user_id = ?";

        try (Connection cnx = MyConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }

        return false;
    }

    public int countDossiers() throws SQLException {
        String sql = "SELECT COUNT(*) FROM dossier_medical";
        try (Connection cnx = MyConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public int countPatientsWithChronicDiseases() throws SQLException {
        String sql = "SELECT COUNT(*) FROM dossier_medical WHERE maladiesChroniques IS NOT NULL AND TRIM(maladiesChroniques) <> '' AND LOWER(TRIM(maladiesChroniques)) <> 'aucune'";
        try (Connection cnx = MyConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public int countPatientsWithAllergies() throws SQLException {
        String sql = "SELECT COUNT(*) FROM dossier_medical WHERE allergies IS NOT NULL AND TRIM(allergies) <> '' AND LOWER(TRIM(allergies)) <> 'aucune'";
        try (Connection cnx = MyConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public Map<String, Integer> getActiviteStats() throws SQLException {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("Sédentaire", 0);
        stats.put("Léger", 0);
        stats.put("Modéré", 0);
        stats.put("Actif", 0);
        stats.put("Très actif", 0);

        String sql = "SELECT niveauActivite, COUNT(*) AS total FROM dossier_medical GROUP BY niveauActivite";

        try (Connection cnx = MyConnection.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String niveau = rs.getString("niveauActivite");
                int total = rs.getInt("total");

                if (niveau == null || niveau.isBlank()) {
                    stats.put("Sédentaire", stats.get("Sédentaire") + total);
                } else {
                    String n = niveau.trim().toLowerCase();
                    if (n.contains("faible") || n.contains("sédentaire") || n.contains("sedentaire")) {
                        stats.put("Sédentaire", stats.get("Sédentaire") + total);
                    } else if (n.contains("léger") || n.contains("leger")) {
                        stats.put("Léger", stats.get("Léger") + total);
                    } else if (n.contains("modéré") || n.contains("modere")) {
                        stats.put("Modéré", stats.get("Modéré") + total);
                    } else if (n.contains("élevé") || n.contains("eleve") || n.contains("actif")) {
                        stats.put("Actif", stats.get("Actif") + total);
                    } else {
                        stats.put("Très actif", stats.get("Très actif") + total);
                    }
                }
            }
        }

        return stats;
    }

    private DossierMedical mapResultSetToDossier(ResultSet rs) throws SQLException {
        return new DossierMedical(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getString("antecedentsMedicaux"),
                rs.getString("maladiesChroniques"),
                rs.getString("allergies"),
                rs.getString("traitementsEnCours"),
                rs.getString("diagnostics"),
                rs.getString("notesMedecin"),
                rs.getString("objectifSante"),
                rs.getString("niveauActivite"),
                rs.getTimestamp("dateCreation"),
                rs.getTimestamp("derniereMiseAJour")
        );
    }
}