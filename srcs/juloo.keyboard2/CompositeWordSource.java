package juloo.keyboard2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CompositeWordSource implements TextEngine.WordSource {
    public static final int PRIO_BASE = 100, PRIO_FALLBACK = 0, PRIO_REGIONAL = 60, PRIO_TECHNICAL = 40;
    final List<Source> _sources;

    public static final class Source {
        public final String locale, name;
        public final boolean personal, technical;
        public final int priority;
        public final TextEngine.WordSource words;
        public Source(TextEngine.WordSource words, String name, int priority, String locale, boolean personal, boolean technical) {
            this.words = words; this.name = name; this.priority = priority; this.locale = locale;
            this.personal = personal; this.technical = technical;
        }
    }

    public static final class WordInfo {
        public final String locale, source, word;
        public final boolean personal, technical;
        public final int priority;
        WordInfo(String word, Source src) {
            this.word = word; source = src.name; priority = src.priority; locale = src.locale;
            personal = src.personal; technical = src.technical;
        }
    }

    public CompositeWordSource(List<Source> sources) { _sources = new ArrayList<>(sources); }

    public static CompositeWordSource withFallback(TextEngine.WordSource primary, String primaryName) {
        List<Source> sources = new ArrayList<>();
        sources.add(new Source(primary, primaryName, PRIO_BASE, null, false, false));
        sources.add(new Source(FallbackWords.INSTANCE, "fallback", PRIO_FALLBACK, null, false, false));
        return new CompositeWordSource(sources);
    }

    @Override public boolean contains(String word) {
        for (Source source : _sources) if (source.words.contains(word)) return true;
        return false;
    }
    @Override public List<String> completions(String prefix, int limit) {
        List<String> out = new ArrayList<>();
        for (WordInfo info : completionsInfo(prefix, limit)) out.add(info.word);
        return out;
    }
    @Override public List<String> similar(String word, int limit) {
        List<String> out = new ArrayList<>();
        for (WordInfo info : similarInfo(word, limit)) out.add(info.word);
        return out;
    }
    public List<WordInfo> completionsInfo(String prefix, int limit) { return collect(prefix, limit, true); }
    public List<WordInfo> similarInfo(String word, int limit) { return collect(word, limit, false); }

    private List<WordInfo> collect(String query, int limit, boolean completions) {
        Map<String, WordInfo> best = new HashMap<>();
        Map<String, Integer> order = new HashMap<>();
        for (Source source : _sources) {
            List<String> words = completions ? source.words.completions(query, limit) : source.words.similar(query, limit);
            for (String word : words) if (word != null) {
                String normalized = TextEngine.normalize(word);
                WordInfo current = best.get(normalized);
                if (current == null || source.priority > current.priority) best.put(normalized, new WordInfo(word, source));
                if (!order.containsKey(normalized)) order.put(normalized, order.size());
            }
        }
        List<WordInfo> result = new ArrayList<>(best.values());
        Collections.sort(result, new Comparator<WordInfo>() {
            @Override public int compare(WordInfo a, WordInfo b) {
                int c = Integer.compare(b.priority, a.priority);
                if (c != 0) return c;
                c = Integer.compare(order.get(TextEngine.normalize(a.word)), order.get(TextEngine.normalize(b.word)));
                return c != 0 ? c : a.word.compareTo(b.word);
            }
        });
        if (result.size() > limit) return new ArrayList<>(result.subList(0, limit));
        return result;
    }
}
