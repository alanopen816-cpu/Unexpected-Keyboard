package juloo.keyboard2;

public final class TextAutocorrect {
    private TextAutocorrect() {}

    public static boolean isCorrectableToken(String token) {
        if (token == null || token.length() < 2) return false;
        boolean first = true;
        for (int i = 0; i < token.length();) {
            int c = token.codePointAt(i);
            i += Character.charCount(c);
            if (c == '\'') first = false;
            else {
                if (!Character.isLetter(c) || (!first && Character.isUpperCase(c))) return false;
                first = false;
            }
        }
        return true;
    }

    public static String trailingNonLetters(String token) {
        if (token == null || token.isEmpty()) return "";
        int end = token.length();
        while (end > 0 && !Character.isLetter(token.codePointBefore(end)))
            end -= Character.charCount(token.codePointBefore(end));
        return token.substring(end);
    }

    public static String learnCore(String token) {
        if (token == null) return "";
        int start = 0, end = token.length();
        while (start < end && !Character.isLetter(token.codePointAt(start)))
            start += Character.charCount(token.codePointAt(start));
        while (end > start && !Character.isLetter(token.codePointBefore(end)))
            end -= Character.charCount(token.codePointBefore(end));
        return token.substring(start, end);
    }

    public static String autocorrectFor(String prev, String token, TextEngine.WordSource dict, LearningStore learned) {
        if (!isCorrectableToken(token)) return null;
        TextEngine.WordSource source = dict == null ? TextEngine.EMPTY : dict;
        TextEngine.Result result = TextEngine.suggest(prev, token, source, learned);
        String correction = result.autoCorrect;
        if (correction == null || correction.equals(token)) return null;
        if (Character.isUpperCase(token.charAt(0)) && !Character.isUpperCase(correction.charAt(0)))
            return Character.toUpperCase(correction.charAt(0)) + correction.substring(1);
        return correction;
    }
}
