package juloo.keyboard2;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Shared text helpers and word-source contract for local suggestion providers. */
public final class TextEngine {
    public static final int MAX_COUNT = 3;
    static final int MAX_LEN = 64;
    static final int MIN_LEN = 2;

    private TextEngine() {}

    public interface WordSource {
        List<String> completions(String prefix, int limit);
        boolean contains(String word);
        List<String> similar(String word, int limit);
    }

    public static final WordSource EMPTY = new WordSource() {
        @Override public boolean contains(String word) { return false; }
        @Override public List<String> completions(String prefix, int limit) { return new ArrayList<>(); }
        @Override public List<String> similar(String word, int limit) { return new ArrayList<>(); }
    };

    public static final class Result {
        public final List<String> suggestions = new ArrayList<>();
        public String autoCorrect;
    }

    public static String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    /** Optimal-string-alignment Damerau-Levenshtein distance, bounded to short words. */
    public static int damerau(String first, String second, int maxDistance) {
        if (first.equals(second)) return 0;
        int n = first.length();
        int m = second.length();
        if (n > MAX_LEN || m > MAX_LEN || Math.abs(n - m) > maxDistance) return maxDistance + 1;
        int[][] distances = new int[n + 1][m + 1];
        for (int i = 0; i <= n; i++) distances[i][0] = i;
        for (int j = 0; j <= m; j++) distances[0][j] = j;
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                int cost = first.charAt(i - 1) == second.charAt(j - 1) ? 0 : 1;
                int best = Math.min(distances[i - 1][j - 1] + cost,
                        Math.min(distances[i - 1][j] + 1, distances[i][j - 1] + 1));
                if (i > 1 && j > 1 && first.charAt(i - 1) == second.charAt(j - 2)
                        && first.charAt(i - 2) == second.charAt(j - 1)) {
                    best = Math.min(best, distances[i - 2][j - 2] + 1);
                }
                distances[i][j] = best;
            }
        }
        return distances[n][m];
    }
}
