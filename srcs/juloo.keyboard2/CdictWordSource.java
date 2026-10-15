package juloo.keyboard2;

import java.util.ArrayList;
import java.util.List;
import juloo.cdict.Cdict;

public final class CdictWordSource implements TextEngine.WordSource {
    final Cdict _dict;

    public CdictWordSource(Cdict dict) {
        _dict = dict;
    }

    @Override
    public boolean contains(String word) {
        if (_dict == null || word == null) return false;
        try {
            return _dict.find(TextEngine.normalize(word)).found;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public List<String> completions(String prefix, int limit) {
        List<String> out = new ArrayList<>();
        if (_dict == null || prefix == null || prefix.isEmpty()) return out;
        try {
            Cdict.Result result = _dict.find(prefix);
            int[] indexes = _dict.suffixes(result, limit);
            for (int index : indexes) {
                if (out.size() >= limit) break;
                String word = _dict.word(index);
                if (word != null && !word.equals(prefix)) out.add(word);
            }
        } catch (Throwable ignored) { }
        return out;
    }

    @Override
    public List<String> similar(String word, int limit) {
        List<String> out = new ArrayList<>();
        if (_dict == null || word == null || word.length() < 2) return out;
        try {
            int[] indexes = _dict.distance(word, TextEngine.allowedDist(word.length()), limit);
            for (int index : indexes) {
                if (out.size() >= limit) break;
                String candidate = _dict.word(index);
                if (candidate != null && !candidate.equals(word)) out.add(candidate);
            }
        } catch (Throwable ignored) { }
        return out;
    }
}
