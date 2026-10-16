package juloo.keyboard2;

public enum EditorProfile {
    NORMAL,
    CODING;

    public static final String TERMUX_PACKAGE = "com.termux";

    public static EditorProfile resolve(String packageName, boolean codingModeEnabled) {
        return codingModeEnabled && TERMUX_PACKAGE.equals(packageName) ? CODING : NORMAL;
    }
}
