package juloo.keyboard2;

/** Events that update the local learning store. */
public final class LearnEvents {
    private LearnEvents() {}

    public static final class Selected {
        public final String from;
        public final String to;

        Selected(String from, String to) {
            this.from = from;
            this.to = to;
        }
    }

    public static boolean wordCommitted(LearningStore store, TextEngine.WordSource dict,
            String rawWord, String prev, String prev2) {
        if (store == null) return false;
        String core = TextAutocorrect.learnCore(rawWord);
        if (core.length() < 2) return false;
        store.noteWord(core, contextOf(prev), contextOf(prev2), inDictionary(dict, core));
        return true;
    }

    public static Selected suggestionSelected(LearningStore store, TextEngine.WordSource dict,
            String rawTyped, String rawChosen, String prev) {
        if (store == null || rawChosen == null) return null;
        String chosen = rawChosen;
        if (chosen.endsWith(" ")) chosen = chosen.substring(0, chosen.length() - 1);
        if (chosen.length() < 2) return null;
        String typed = TextAutocorrect.learnCore(rawTyped);
        store.noteWord(chosen, contextOf(prev), null, inDictionary(dict, chosen));
        if (typed.length() >= 2 && !typed.equals(chosen)) {
            store.noteCorrection(typed, chosen);
            return new Selected(typed, chosen);
        }
        return new Selected(null, chosen);
    }

    public static boolean autocorrectionRejected(LearningStore store, String rawRestored) {
        if (store == null) return false;
        String core = TextAutocorrect.learnCore(rawRestored);
        if (core.length() < 2) return false;
        store.noteRejectedAutocorrection(core);
        return true;
    }

    public static boolean autocorrectionAccepted(LearningStore store, String rawTyped,
            String rawApplied) {
        if (store == null) return false;
        String typed = TextAutocorrect.learnCore(rawTyped);
        String applied = TextAutocorrect.learnCore(rawApplied);
        if (typed.length() < 2 || applied.length() < 2) return false;
        return store.noteAcceptedCorrection(typed, applied);
    }

    static boolean inDictionary(TextEngine.WordSource dict, String core) {
        if (dict == null) return false;
        try {
            return dict.contains(core);
        } catch (Throwable ignored) {
            return false;
        }
    }

    static String contextOf(String raw) {
        String core = TextAutocorrect.learnCore(raw);
        return core.length() >= 2 ? core : null;
    }
}
