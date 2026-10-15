package juloo.keyboard2;

public enum KeyboardMode {
    TEXT,
    SHELL;

    public static final String PREF_KEY = "keyboard_mode";

    public KeyboardMode toggle() {
        return this == TEXT ? SHELL : TEXT;
    }

    public static KeyboardMode parse(String value) {
        return "SHELL".equalsIgnoreCase(value) ? SHELL : TEXT;
    }

    public static KeyboardMode effective(String stored, String packageName) {
        return stored == null ? TEXT : parse(stored);
    }

    public String label() {
        return "[ " + name() + " ]";
    }
}
