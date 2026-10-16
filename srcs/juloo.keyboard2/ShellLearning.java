package juloo.keyboard2;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ShellLearning {
    static final String K_DECAY = "learn_shell_decay_ts";
    static final String K_SEQS = "learn_shell_seqs";
    static final String K_TOKENS = "learn_shell_tokens";
    public static final int MAX_PER_PREV = 12;
    public static final int MAX_SEQS = 500;
    public static final int MAX_TOKENS = 300;
    public static final int MIN_USES_COMPLETION = 2;

    final KvStore _store;
    final Map<String, LearningStore.Entry> _tokens = new HashMap<>();
    final Map<String, Map<String, LearningStore.Entry>> _seqs = new HashMap<>();

    public ShellLearning(KvStore store) {
        _store = store;
        load();
    }

    long now() {
        return System.currentTimeMillis();
    }

    public synchronized void noteSequence(String prev, String token) {
        if (token == null || token.isEmpty()) return;
        long time = now();
        LearningStore.bump(_tokens, token, time);
        String previous = prev == null ? "" : prev;
        Map<String, LearningStore.Entry> entries = _seqs.get(previous);
        if (entries == null) {
            entries = new HashMap<>();
            _seqs.put(previous, entries);
        }
        LearningStore.bump(entries, token, time);
        prune();
        save();
    }

    public synchronized boolean retractSequence(String prev, String token) {
        boolean changed = decrement(_tokens, token);
        String previous = prev == null ? "" : prev;
        Map<String, LearningStore.Entry> entries = _seqs.get(previous);
        if (entries != null) {
            changed |= decrement(entries, token);
            if (entries.isEmpty()) _seqs.remove(previous);
        }
        if (changed) save();
        return changed;
    }

    private boolean decrement(Map<String, LearningStore.Entry> entries, String token) {
        LearningStore.Entry entry = entries.get(token);
        if (entry == null || entry.count <= 0) return false;
        if (entry.count > 1) entry.count--;
        else entries.remove(token);
        return true;
    }

    public synchronized List<String> after(String prev, int limit) {
        Map<String, LearningStore.Entry> entries = _seqs.get(prev == null ? "" : prev);
        return entries == null ? new ArrayList<String>() : LearningStore.rankedKeys(entries, now(), limit);
    }

    public synchronized List<String> completeTokens(String prefix, int limit) {
        Map<String, LearningStore.Entry> pool = new HashMap<>();
        for (Map.Entry<String, LearningStore.Entry> entry : _tokens.entrySet()) {
            if (entry.getKey().startsWith(prefix) && !entry.getKey().equals(prefix)
                    && entry.getValue().count >= MIN_USES_COMPLETION) {
                pool.put(entry.getKey(), entry.getValue());
            }
        }
        return LearningStore.rankedKeys(pool, now(), limit);
    }

    public synchronized void clear() {
        _tokens.clear();
        _seqs.clear();
        _store.remove(K_TOKENS);
        _store.remove(K_SEQS);
        _store.put(K_DECAY, String.valueOf(now()));
    }

    synchronized void prune() {
        pruneMap(_tokens, MAX_TOKENS);
        int total = 0;
        for (Map<String, LearningStore.Entry> entries : _seqs.values()) {
            pruneMap(entries, MAX_PER_PREV);
            total += entries.size();
        }
        if (total <= MAX_SEQS) return;
        List<String[]> all = new ArrayList<>();
        for (Map.Entry<String, Map<String, LearningStore.Entry>> entry : _seqs.entrySet()) {
            for (String token : entry.getValue().keySet()) all.add(new String[]{entry.getKey(), token});
        }
        final long time = now();
        all.sort(new Comparator<String[]>() {
            @Override
            public int compare(String[] a, String[] b) {
                return Double.compare(score(b), score(a));
            }
            private double score(String[] pair) {
                return LearningStore.score(_seqs.get(pair[0]).get(pair[1]), time);
            }
        });
        for (int i = MAX_SEQS; i < all.size(); i++) {
            String[] pair = all.get(i);
            Map<String, LearningStore.Entry> entries = _seqs.get(pair[0]);
            if (entries != null) {
                entries.remove(pair[1]);
                if (entries.isEmpty()) _seqs.remove(pair[0]);
            }
        }
    }

    void pruneMap(Map<String, LearningStore.Entry> entries, int max) {
        if (entries.size() <= max) return;
        List<String> ranked = LearningStore.rankedKeys(entries, now(), entries.size());
        for (int i = max; i < ranked.size(); i++) entries.remove(ranked.get(i));
    }

    synchronized void save() {
        _store.put(K_TOKENS, LearningStore.encodeEntries(_tokens));
        _store.put(K_SEQS, encodeSeqs());
    }

    synchronized void load() {
        LearningStore.decodeEntries(_store.get(K_TOKENS, ""), _tokens);
        decodeSeqs(_store.get(K_SEQS, ""));
        maybeDecay();
    }

    synchronized void maybeDecay() {
        long last = 0;
        try {
            last = Long.parseLong(_store.get(K_DECAY, "0"));
        } catch (Exception ignored) { }
        long time = now();
        if (last <= 0) {
            _store.put(K_DECAY, String.valueOf(time));
            return;
        }
        if (time - last < 2592000000L) return;
        for (LearningStore.Entry entry : _tokens.values()) entry.count = Math.max(1, entry.count / 2);
        for (Map<String, LearningStore.Entry> entries : _seqs.values()) {
            for (LearningStore.Entry entry : entries.values()) entry.count = Math.max(1, entry.count / 2);
        }
        _store.put(K_DECAY, String.valueOf(time));
        save();
    }

    String encodeSeqs() {
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, Map<String, LearningStore.Entry>> entry : _seqs.entrySet()) {
            for (Map.Entry<String, LearningStore.Entry> token : entry.getValue().entrySet()) {
                if (out.length() > 0) out.append(';');
                out.append(LearningStore.esc(entry.getKey())).append('>').append(LearningStore.esc(token.getKey()))
                        .append('=').append(token.getValue().count).append(',').append(token.getValue().lastSeen);
            }
        }
        return out.toString();
    }

    void decodeSeqs(String value) {
        _seqs.clear();
        if (value == null || value.isEmpty()) return;
        for (String part : LearningStore.split(value, ';')) {
            List<String> keyValue = LearningStore.split(part, '=');
            if (keyValue.size() != 2) continue;
            List<String> pair = LearningStore.split(keyValue.get(0), '>');
            List<String> countTime = LearningStore.split(keyValue.get(1), ',');
            if (pair.size() != 2 || countTime.size() != 2) continue;
            try {
                String previous = LearningStore.unesc(pair.get(0));
                Map<String, LearningStore.Entry> entries = _seqs.get(previous);
                if (entries == null) {
                    entries = new HashMap<>();
                    _seqs.put(previous, entries);
                }
                entries.put(LearningStore.unesc(pair.get(1)), new LearningStore.Entry(
                        Integer.parseInt(countTime.get(0)), Long.parseLong(countTime.get(1))));
            } catch (Exception ignored) { }
        }
    }
}
