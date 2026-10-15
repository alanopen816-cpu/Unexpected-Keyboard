package juloo.keyboard2;

public final class StripPolicy {
    private StripPolicy() {}

    public static boolean shouldShowStrip(boolean suggestionsEnabled, boolean shouldShowCandidates,
            boolean splitLayout, boolean sensitive) {
        return !splitLayout && !sensitive;
    }

    public static boolean shouldShowSuggestions(boolean suggestionsEnabled, boolean shouldShowCandidates,
            boolean splitLayout, boolean sensitive) {
        return suggestionsEnabled && shouldShowCandidates && !splitLayout && !sensitive;
    }
}
