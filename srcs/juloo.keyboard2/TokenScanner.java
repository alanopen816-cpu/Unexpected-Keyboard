package juloo.keyboard2;

public final class TokenScanner {
    static final int MAX_LINE = 512;
    private final StringBuilder line = new StringBuilder();
    private boolean dirty;
    private boolean confident = true;

    public void reset() {
        line.setLength(0);
        dirty = false;
        confident = true;
    }

    public void invalidate() {
        line.setLength(0);
        dirty = true;
        confident = false;
    }

    public void anchorToBoundary() {
        int end = line.length();
        while (end > 0 && !isSeparator(line.charAt(end - 1))) end--;
        line.setLength(end);
        confident = false;
    }

    public void loseConfidence() { confident = false; }
    public boolean isDirty() { return dirty; }
    public boolean isConfident() { return confident && !dirty; }

    public void typed(String s) {
        if (s == null) return;
        dirty = false;
        boolean boundary = false;
        for (int i = 0; i < s.length();) {
            int c = s.codePointAt(i);
            i += Character.charCount(c);
            if (c == '\n' || c == '\r') {
                line.setLength(0);
                boundary = true;
            } else {
                if (isSeparator(c)) boundary = true;
                line.appendCodePoint(c);
            }
        }
        if (boundary) confident = true;
        trim();
    }

    public boolean backspace() {
        int len = line.length();
        if (len == 0) return false;
        line.delete(line.offsetByCodePoints(len, -1), len);
        return true;
    }

    public String current() {
        int end = line.length();
        if (end == 0 || isSeparator(line.charAt(end - 1))) return "";
        int start = end;
        while (start > 0 && !isSeparator(line.charAt(start - 1))) start--;
        return line.substring(start, end);
    }

    public String previous() {
        int end = line.length() - current().length();
        while (end > 0 && isSeparator(line.charAt(end - 1))) end--;
        int start = end;
        while (start > 0 && !isSeparator(line.charAt(start - 1))) start--;
        return line.substring(start, end);
    }

    String line() { return line.toString(); }

    private void trim() {
        if (line.length() <= MAX_LINE) return;
        line.delete(0, line.length() - MAX_LINE);
        if (!isSeparator(line.charAt(0))) confident = false;
    }

    public static boolean isSeparator(int c) {
        switch (c) {
            case 9: case 10: case 13: case 32: case 35: case 38: case 40: case 41:
            case 59: case 60: case 62: case 96: case 124:
                return true;
            default:
                return false;
        }
    }
}
