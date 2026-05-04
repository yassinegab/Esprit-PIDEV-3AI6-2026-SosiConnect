package org.example.utils;

import java.util.regex.Pattern;

public class ValidationUtil {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[0-9]{8,15}$");

    public static boolean isTextValid(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public static boolean isEmailValid(String email) {
        return isTextValid(email) && EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isPhoneValid(String phone) {
        return isTextValid(phone) && PHONE_PATTERN.matcher(phone).matches();
    }

    public static boolean isPasswordStrong(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        return hasUpper && hasLower && hasDigit;
    }

    public static boolean isPositiveInt(String value) {
        try {
            int n = Integer.parseInt(value);
            return n > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isPositiveDouble(String value) {
        try {
            double n = Double.parseDouble(value);
            return n >= 0;
        } catch (Exception e) {
            return false;
        }
    }
}
