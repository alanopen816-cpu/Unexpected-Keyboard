package juloo.keyboard2;

import android.content.SharedPreferences;

public final class PrefKvStore implements KvStore {
    private final SharedPreferences prefs;

    public PrefKvStore(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    @Override
    public String get(String key, String defaultValue) {
        try {
            String value = prefs.getString(key, defaultValue);
            return value == null ? defaultValue : value;
        } catch (Throwable ignored) {
            return defaultValue;
        }
    }

    @Override
    public void put(String key, String value) {
        try {
            prefs.edit().putString(key, value).apply();
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void remove(String key) {
        try {
            prefs.edit().remove(key).apply();
        } catch (Throwable ignored) {
        }
    }
}
