package juloo.keyboard2;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class CodingSuggestions {
    public static final int MAX_COUNT = 3;
    static final int MIN_TYPO_LEN = 2;
    static final Pattern ELIGIBLE = Pattern.compile("^[a-z][a-z0-9+_]*$");
    static final String[] TECH_WORDS = {"main", "debug", "release", "localhost", "build", "config", "token", "error", "warning", "import", "package", "version", "stable", "async", "return", "source", "status"};
    private CodingSuggestions() {}
    public static boolean isEligible(String token) { return token != null && ELIGIBLE.matcher(token).matches(); }

    public static List<String> suggest(String prev, String current, boolean suggestCommands, boolean suggestTech) {
        List<String> out = new ArrayList<>(MAX_COUNT);
        if (current == null) return out;
        if (current.isEmpty()) {
            if (suggestCommands && CommandContext.isKnownCommand(prev))
                for (String sub : CommandContext.subcommands(prev)) { if (out.size() == MAX_COUNT) break; out.add(sub); }
            return out;
        }
        if (!suggestCommands || !isEligible(current)) return out;
        String[] pool = CommandContext.isKnownCommand(prev) ? CommandContext.subcommands(prev) : CommandContext.TOP_COMMANDS;
        for (String candidate : pool) if (out.size() < MAX_COUNT && !candidate.equals(current) && candidate.startsWith(current)) out.add(candidate);
        if (current.length() >= MIN_TYPO_LEN)
            for (String candidate : pool) if (out.size() < MAX_COUNT && !candidate.equals(current) && !out.contains(candidate) && isSingleEditTypo(current, candidate)) out.add(candidate);
        if (suggestTech)
            for (String candidate : TECH_WORDS) if (out.size() < MAX_COUNT && !candidate.equals(current) && candidate.startsWith(current) && !out.contains(candidate)) out.add(candidate);
        return out;
    }

    static boolean isSingleEditTypo(String typed, String candidate) {
        int n = typed.length(), m = candidate.length();
        if (Math.abs(n - m) > 1) return false;
        int i = 0, j = 0, edits = 0;
        while (i < n && j < m) {
            if (typed.charAt(i) == candidate.charAt(j)) { i++; j++; }
            else if (n == m && i + 1 < n && j + 1 < m && typed.charAt(i) == candidate.charAt(j + 1) && typed.charAt(i + 1) == candidate.charAt(j)) {
                if (++edits > 1) return false; i += 2; j += 2;
            } else {
                if (++edits > 1) return false;
                if (n == m) { i++; j++; } else if (n > m) i++; else j++;
            }
        }
        return edits + (n - i) + (m - j) == 1;
    }
}
