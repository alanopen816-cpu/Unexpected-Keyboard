package juloo.keyboard2;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

    private static final class Scored {
        final String word;
        double dictionary;
        double personal;
        double context;
        double edit;

        Scored(String word) { this.word = word; }
        double total() { return dictionary + personal + context + edit; }
    }

    private static Scored scored(Map<String, Scored> scores, String word) {
        Scored result = scores.get(word);
        if (result == null) {
            result = new Scored(word);
            scores.put(word, result);
        }
        return result;
    }

    public static Result suggest(String prev, String current, WordSource dict, LearningStore learned) {
        Result result = new Result();
        if (current == null || current.isEmpty()) {
            if (learned != null && prev != null) result.suggestions.addAll(learned.after(prev, MAX_COUNT));
            return result;
        }
        if (current.length() < MIN_LEN) return result;

        WordSource source = dict == null ? EMPTY : dict;
        String normalized = normalize(current);
        if (learned != null && learned.isPersonal(current)) return result;
        boolean valid = source.contains(current) || source.contains(normalized);
        String correction = valid || learned == null ? null : learned.correctionFor(current);
        Map<String, Scored> scores = new HashMap<>();

        List<String> completions = source.completions(normalized, 6);
        List<String> similar = source.similar(normalized, 6);
        for (String word : completions) {
            if (!word.equals(current)) scored(scores, word).dictionary = 2.0;
        }
        int bestDistance = Integer.MAX_VALUE;
        String best = null;
        boolean incomplete = false;
        for (String word : source.completions(normalized, 1)) {
            if (!word.equals(current)) { incomplete = true; break; }
        }
        for (String word : similar) {
            if (word.equals(current)) continue;
            String candidate = normalize(word);
            int distance = damerau(normalized, candidate, 2);
            if (distance > allowedDist(normalized.length())) continue;
            Scored s = scored(scores, word);
            s.dictionary = 5.0;
            s.edit = -distance * 1.5;
            if (distance >= 1 && !incomplete && candidate.length() <= normalized.length()
                    && isAutoCorrectCaseCompatible(current, word)
                    && (distance < bestDistance || (distance == bestDistance && betterTieBreak(word, best, normalized)))) {
                best = word;
                bestDistance = distance;
            }
        }

        if (learned != null) {
            for (String word : learned.completeLearned(normalized, 6))
                scored(scores, word).personal = Math.max(scored(scores, word).personal, learned.wordScore(word) * 0.05 + 3.0);
            if (prev != null) {
                int rank = 0;
                for (String word : learned.after(prev, 6)) {
                    if (normalize(word).startsWith(normalized) && !word.equals(current))
                        scored(scores, word).context = Math.max(scored(scores, word).context, 4.0 - rank * 0.3);
                    rank++;
                }
            }
        }
        List<Scored> ranked = new ArrayList<>(scores.values());
        Collections.sort(ranked, new Comparator<Scored>() {
            @Override public int compare(Scored a, Scored b) {
                int order = Double.compare(b.total(), a.total());
                return order != 0 ? order : a.word.compareTo(b.word);
            }
        });
        for (Scored candidate : ranked) {
            if (result.suggestions.size() == MAX_COUNT) break;
            if (!candidate.word.equals(current)) result.suggestions.add(candidate.word);
        }
        if (!valid) {
            if (correction != null) {
                result.autoCorrect = correction;
                if (!result.suggestions.contains(correction)) result.suggestions.add(0, correction);
                while (result.suggestions.size() > MAX_COUNT) result.suggestions.remove(result.suggestions.size() - 1);
            } else if (learned == null || !learned.isProtected(current)) {
                result.autoCorrect = best;
            }
        }
        return result;
    }

    public static int allowedDist(int length) { return length >= 8 ? 2 : 1; }

    private static boolean isAutoCorrectCaseCompatible(String typed, String candidate) {
        int n = Math.min(typed.length(), candidate.length());
        for (int i = 0; i < n; i++) {
            if (Character.isLowerCase(typed.charAt(i)) && Character.isUpperCase(candidate.charAt(i))) return false;
            if (i > 0 && Character.isUpperCase(candidate.charAt(i)) && !Character.isUpperCase(typed.charAt(i))) return false;
        }
        for (int i = n; i < candidate.length(); i++) if (Character.isUpperCase(candidate.charAt(i))) return false;
        return true;
    }

    private static boolean betterTieBreak(String candidate, String best, String normalized) {
        if (best == null) return true;
        boolean sameInitial = normalize(candidate).startsWith(normalized.substring(0, 1));
        boolean bestSameInitial = normalize(best).startsWith(normalized.substring(0, 1));
        if (sameInitial != bestSameInitial) return sameInitial;
        int candidatePrefix = commonPrefix(normalize(candidate), normalized);
        int bestPrefix = commonPrefix(normalize(best), normalized);
        return candidatePrefix != bestPrefix ? candidatePrefix > bestPrefix : candidate.compareTo(best) < 0;
    }

    private static int commonPrefix(String first, String second) {
        int i = 0;
        while (i < first.length() && i < second.length() && first.charAt(i) == second.charAt(i)) i++;
        return i;
    }
}
