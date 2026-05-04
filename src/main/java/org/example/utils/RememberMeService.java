package org.example.utils;

import java.util.prefs.Preferences;

public class RememberMeService {

    private static final Preferences PREFS =
            Preferences.userRoot().node("sosi_project_user_login");

    private static final String KEY_REMEMBER = "remember_me";
    private static final String KEY_EMAIL = "email";

    public void save(String email) {
        PREFS.putBoolean(KEY_REMEMBER, true);
        PREFS.put(KEY_EMAIL, email == null ? "" : email.trim());
    }

    public void clear() {
        PREFS.putBoolean(KEY_REMEMBER, false);
        PREFS.remove(KEY_EMAIL);
    }

    public boolean isRemembered() {
        return PREFS.getBoolean(KEY_REMEMBER, false);
    }

    public String getEmail() {
        return PREFS.get(KEY_EMAIL, "");
    }
}