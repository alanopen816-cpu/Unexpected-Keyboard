package juloo.keyboard2;

public final class SensitiveFields {
    static final int CLASS_DATETIME = 4;
    static final int CLASS_MASK = 15;
    static final int CLASS_NUMBER = 2;
    static final int CLASS_PHONE = 3;
    static final int CLASS_TEXT = 1;
    static final int IME_FLAG_NO_PERSONALIZED_LEARNING = 16777216;
    static final int NUMBER_VARIATION_PASSWORD = 16;
    static final int TEXT_VARIATION_PASSWORD = 128;
    static final int TEXT_VARIATION_VISIBLE_PASSWORD = 144;
    static final int TEXT_VARIATION_WEB_PASSWORD = 224;
    static final int VARIATION_MASK = 4080;

    private SensitiveFields() {}

    public static boolean isSensitive(int inputType) {
        int cls = inputType & CLASS_MASK;
        int variation = inputType & VARIATION_MASK;
        switch (cls) {
            case CLASS_TEXT:
                return variation == TEXT_VARIATION_PASSWORD
                        || variation == TEXT_VARIATION_VISIBLE_PASSWORD
                        || variation == TEXT_VARIATION_WEB_PASSWORD;
            case CLASS_NUMBER:
            case CLASS_PHONE:
            case CLASS_DATETIME:
                return true;
            default:
                return false;
        }
    }

    public static boolean allowLearning(int inputType, int imeOptions) {
        return !isSensitive(inputType) && (imeOptions & IME_FLAG_NO_PERSONALIZED_LEARNING) == 0;
    }
}
