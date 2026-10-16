package juloo.keyboard2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;


public final class LearningStore {
    public static final int CORRECTION_CONFIDENT_AFTER = 2;
    static final long DECAY_PERIOD_MS = 2592000000L;
    static final String K_BIGRAMS = "learn_text_bigrams";
    static final String K_CORR = "learn_text_corrections";
    static final String K_DECAY = "learn_text_decay_ts";
    static final String K_KEPT = "learn_text_kept";
    static final String K_PERSONAL = "learn_text_personal";
    static final String K_PROT = "learn_text_protected";
    static final String K_SURF = "learn_text_surface";
    static final String K_TRIGRAMS = "learn_text_trigrams";
    static final String K_WORDS = "learn_text_words";
    public static final int MAX_BIGRAMS = 800;
    public static final int MAX_CORRECTIONS = 200;
    public static final int MAX_PERSONAL = 200;
    public static final int MAX_PER_PAIR = 12;
    public static final int MAX_PER_PREV = 12;
    public static final int MAX_PROTECTED = 200;
    public static final int MAX_TRIGRAMS = 800;
    public static final int MAX_WORDS = 500;
    public static final int PERSONAL_AFTER_KEPT = 3;
    public static final int REJECT_BOOST = 2;
    final KvStore _store;
    final LinkedHashSet<String> _protected = new LinkedHashSet<>();
    final Map<String, Entry> _words = new HashMap();
    final Map<String, Map<String, Entry>> _bigrams = new HashMap();
    final Map<Pair, Map<String, Entry>> _trigrams = new HashMap();
    final Map<String, Correction> _corrections = new HashMap();
    final Map<String, Entry> _personal = new HashMap();
    final Map<String, String> _surface = new HashMap();
    private final Map<String, Entry> _kept = new HashMap();

    static final class Entry {
        int count;
        long lastSeen;

        Entry(int c, long t) {
            this.count = c;
            this.lastSeen = t;
        }
    }

    static final class Correction {
        int count;
        long lastSeen;
        String to;

        Correction(String t, int c, long s) {
            this.to = t;
            this.count = c;
            this.lastSeen = s;
        }
    }

    static final class Pair {
        final String first;
        final String second;

        Pair(String a, String b) {
            this.first = a;
            this.second = b;
        }

        public boolean equals(Object o) {
            if (!(o instanceof Pair)) {
                return false;
            }
            Pair p = (Pair) o;
            return this.first.equals(p.first) && this.second.equals(p.second);
        }

        public int hashCode() {
            return (this.first.hashCode() * 31) + this.second.hashCode();
        }
    }

    public LearningStore(KvStore store) {
        this._store = store;
        load();
    }

    long now() {
        return System.currentTimeMillis();
    }

    static String key(String w) {
        return w == null ? "" : TextEngine.normalize(w);
    }

    private void learnSurface(String norm, String original, boolean explicit) {
        if (norm == null || norm.isEmpty() || original == null || original.isEmpty()) {
            return;
        }
        String cur = this._surface.get(norm);
        if (cur == null || (explicit && !cur.equals(original))) {
            this._surface.put(norm, original);
        }
    }

    private String display(String norm) {
        String s = this._surface.get(norm);
        return s == null ? norm : s;
    }

    public synchronized void noteWord(String word, String prev, String prev2, boolean inDictionary) {
        if (word != null) {
            if (word.length() >= 2) {
                String k = key(word);
                if (k.length() < 2) {
                    return;
                }
                String p2 = null;
                String p = (prev == null || prev.length() < 2) ? null : key(prev);
                if (p != null && p.length() < 2) {
                    p = null;
                }
                if (prev2 != null && prev2.length() >= 2) {
                    p2 = key(prev2);
                }
                if (p2 != null && p2.length() < 2) {
                    p2 = null;
                }
                long t = now();
                learnSurface(k, word, false);
                boolean known = inDictionary || isPersonal(word);
                if (!inDictionary) {
                    Entry kept = _keptStore(k, t, 1);
                    if (kept.count >= 3) {
                        bump(this._personal, k, t);
                        known = true;
                    }
                }
                if (known) {
                    bump(this._words, k, t);
                    if (p != null) {
                        Map<String, Entry> m = this._bigrams.get(p);
                        if (m == null) {
                            m = new HashMap();
                            this._bigrams.put(p, m);
                        }
                        bump(m, k, t);
                        if (p2 != null) {
                            Pair pair = new Pair(p2, p);
                            Map<String, Entry> tm = this._trigrams.get(pair);
                            if (tm == null) {
                                tm = new HashMap();
                                this._trigrams.put(pair, tm);
                            }
                            bump(tm, k, t);
                        }
                    }
                }
                prune();
                save();
            }
        }
    }

    private Entry _keptStore(String word, long t, int delta) {
        Entry e = this._kept.get(word);
        if (e == null) {
            e = new Entry(0, t);
            this._kept.put(word, e);
        }
        e.count += delta;
        e.lastSeen = t;
        return e;
    }

    public synchronized void noteCorrection(String typed, String chosen) {
        if (typed != null && chosen != null) {
            if (!typed.equals(chosen) && typed.length() >= 2 && chosen.length() >= 2) {
                String tk = key(typed);
                String ck = key(chosen);
                if (tk.length() >= 2 && ck.length() >= 2) {
                    long t = now();
                    Correction c = this._corrections.get(tk);
                    if (c == null || !c.to.equals(chosen)) {
                        c = new Correction(chosen, 0, t);
                    }
                    c.count++;
                    c.lastSeen = t;
                    this._corrections.put(tk, c);
                    learnSurface(ck, chosen, true);
                    bump(this._words, ck, t);
                    prune();
                    save();
                }
            }
        }
    }

    public synchronized boolean retractCorrection(String typed, String chosen) {
        Correction c = this._corrections.get(key(typed));
        if (c != null && c.to.equals(chosen) && c.count > 0) {
            if (c.count <= 1) {
                this._corrections.remove(typed);
            } else {
                c.count--;
            }
            save();
            return true;
        }
        return false;
    }

    public synchronized void forgetWord(String word) {
        Entry e = this._words.get(key(word));
        if (e != null && e.count > 0) {
            if (e.count <= 1) {
                this._words.remove(word);
            } else {
                e.count--;
            }
            save();
        }
    }

    public synchronized void noteRejectedAutocorrection(String word) {
        if (word != null) {
            if (word.length() >= 2) {
                String k = key(word);
                if (k.length() < 2) {
                    return;
                }
                long t = now();
                noteProtected(word);
                Entry kept = _keptStore(k, t, 2);
                if (kept.count >= 3) {
                    bump(this._personal, k, t);
                }
                bump(this._words, k, t);
                prune();
                save();
            }
        }
    }

    public synchronized boolean noteAcceptedCorrection(String typed, String chosen) {
        if (typed != null && chosen != null) {
            if (!typed.equals(chosen) && typed.length() >= 2 && chosen.length() >= 2) {
                String tk = key(typed);
                if (tk.length() < 2) {
                    return false;
                }
                Correction c = this._corrections.get(tk);
                if (c != null && c.to.equals(chosen)) {
                    c.count++;
                    c.lastSeen = now();
                    save();
                    return true;
                }
                return false;
            }
        }
        return false;
    }

    public synchronized void noteProtected(String word) {
        if (word != null) {
            if (word.length() >= 2) {
                String k = key(word);
                if (k.length() < 2) {
                    return;
                }
                this._protected.add(k);
                learnSurface(k, word, true);
                while (this._protected.size() > 200) {
                    Iterator<String> it = this._protected.iterator();
                    it.next();
                    it.remove();
                }
                save();
            }
        }
    }

    public synchronized boolean isProtected(String word) {
        if (word == null) {
            return false;
        }
        return this._protected.contains(key(word));
    }

    public synchronized String correctionFor(String typed) {
        Correction c = this._corrections.get(key(typed));
        if (c == null || c.count < 2) {
            return null;
        }
        return c.to;
    }

    public synchronized boolean isPersonal(String word) {
        if (word == null) {
            return false;
        }
        return this._personal.containsKey(key(word));
    }

    public synchronized int wordCount(String word) {
        Entry e;
        e = this._words.get(key(word));
        return e == null ? 0 : e.count;
    }

    public synchronized double wordScore(String word) {
        Entry e;
        e = this._words.get(key(word));
        return e == null ? 0.0d : score(e, now());
    }

    public synchronized List<String> after(String prev, int limit) {
        List<String> out = new ArrayList<>();
        Map<String, Entry> m = this._bigrams.get(key(prev));
        if (m == null) {
            return out;
        }
        for (String k : rankedKeys(m, now(), limit)) {
            out.add(display(k));
        }
        return out;
    }

    public synchronized List<String> after2(String prev2, String prev, int limit) {
        List<String> out = new ArrayList<>();
        Map<String, Entry> m = this._trigrams.get(new Pair(key(prev2), key(prev)));
        if (m == null) {
            return out;
        }
        for (String k : rankedKeys(m, now(), limit)) {
            out.add(display(k));
        }
        return out;
    }

    public synchronized List<String> completeLearned(String prefix, int limit) {
        List<String> out = new ArrayList<>();
        if (prefix != null && prefix.length() >= 2) {
            String pk = key(prefix);
            if (pk.length() < 2) {
                return out;
            }
            Map<String, Entry> pool = new HashMap<>();
            for (Map.Entry<String, Entry> e : this._words.entrySet()) {
                if (e.getKey().startsWith(pk) && !e.getKey().equals(pk)) {
                    pool.put(e.getKey(), e.getValue());
                }
            }
            for (Map.Entry<String, Entry> e2 : this._personal.entrySet()) {
                if (e2.getKey().startsWith(pk) && !e2.getKey().equals(pk) && e2.getValue().count >= 3) {
                    pool.put(e2.getKey(), e2.getValue());
                }
            }
            for (String k : rankedKeys(pool, now(), limit)) {
                out.add(display(k));
            }
            return out;
        }
        return out;
    }

    public synchronized List<String> personalWords(int limit) {
        List<String> out;
        out = new ArrayList<>();
        for (String k : rankedKeys(this._personal, now(), limit)) {
            out.add(display(k));
        }
        return out;
    }

    public synchronized int personalCount() {
        return this._personal.size();
    }

    public synchronized boolean removePersonal(String word) {
        if (word == null) {
            return false;
        }
        String k = key(word);
        if (k.length() < 2) {
            return false;
        }
        boolean changed = false | (this._personal.remove(k) != null);
        boolean changed2 = (this._words.remove(k) != null) | changed | (this._kept.remove(k) != null) | this._protected.remove(k);
        if (this._bigrams.remove(k) != null) {
            changed2 = true;
        }
        List<String> dropOuter = new ArrayList<>();
        for (Map.Entry<String, Map<String, Entry>> e : this._bigrams.entrySet()) {
            if (e.getValue().remove(k) != null) {
                changed2 = true;
            }
            if (e.getValue().isEmpty()) {
                dropOuter.add(e.getKey());
            }
        }
        for (String o : dropOuter) {
            this._bigrams.remove(o);
        }
        List<Pair> dropPair = new ArrayList<>();
        for (Map.Entry<Pair, Map<String, Entry>> e2 : this._trigrams.entrySet()) {
            if (e2.getKey().first.equals(k) || e2.getKey().second.equals(k)) {
                dropPair.add(e2.getKey());
            } else {
                if (e2.getValue().remove(k) != null) {
                    changed2 = true;
                }
                if (e2.getValue().isEmpty()) {
                    dropPair.add(e2.getKey());
                }
            }
        }
        for (Pair p : dropPair) {
            if (this._trigrams.remove(p) != null) {
                changed2 = true;
            }
        }
        List<String> dropCorr = new ArrayList<>();
        for (Map.Entry<String, Correction> e3 : this._corrections.entrySet()) {
            if (e3.getKey().equals(k) || key(e3.getValue().to).equals(k)) {
                dropCorr.add(e3.getKey());
            }
        }
        for (String t : dropCorr) {
            if (this._corrections.remove(t) != null) {
                changed2 = true;
            }
        }
        if (changed2) {
            save();
        }
        return changed2;
    }

    public synchronized void addPersonal(String word) {
        if (word == null) {
            return;
        }
        String core = TextAutocorrect.learnCore(word);
        String k = key(core);
        if (k.length() < 2) {
            return;
        }
        long t = now();
        learnSurface(k, core, true);
        Entry e = this._personal.get(k);
        if (e == null) {
            this._personal.put(k, new Entry(3, t));
        } else {
            e.count = Math.max(e.count, 3);
            e.lastSeen = t;
        }
        prune();
        save();
    }

    public synchronized void clear() {
        this._words.clear();
        this._bigrams.clear();
        this._trigrams.clear();
        this._corrections.clear();
        this._personal.clear();
        this._protected.clear();
        this._kept.clear();
        this._surface.clear();
        this._store.remove(K_WORDS);
        this._store.remove(K_BIGRAMS);
        this._store.remove(K_TRIGRAMS);
        this._store.remove(K_CORR);
        this._store.remove(K_PERSONAL);
        this._store.remove(K_KEPT);
        this._store.remove(K_SURF);
        this._store.remove(K_PROT);
        this._store.put(K_DECAY, String.valueOf(now()));
    }

    static Entry bump(Map<String, Entry> m, String k, long t) {
        Entry e = m.get(k);
        if (e == null) {
            e = new Entry(0, t);
            m.put(k, e);
        }
        e.count++;
        e.lastSeen = t;
        return e;
    }

    static double score(Entry e, long now) {
        long ageDays = Math.max(0L, (now - e.lastSeen) / 86400000);
        return ((double) e.count) / ((ageDays / 7.0d) + 1.0d);
    }

    static List<String> rankedKeys(Map<String, Entry> m, final long now, int limit) {
        List<Map.Entry<String, Entry>> l = new ArrayList<>(m.entrySet());
        Collections.sort(l, new Comparator<Map.Entry<String, Entry>>() { // from class: juloo.keyboard2.LearningStore.1
            @Override // java.util.Comparator
            public int compare(Map.Entry<String, Entry> a, Map.Entry<String, Entry> b) {
                return Double.compare(LearningStore.score(b.getValue(), now), LearningStore.score(a.getValue(), now));
            }
        });
        List<String> out = new ArrayList<>();
        for (int i = 0; i < l.size() && out.size() < limit; i++) {
            out.add(l.get(i).getKey());
        }
        return out;
    }

    synchronized void prune() {
        pruneMap(this._words, 500);
        pruneMap(this._personal, 200);
        if (this._corrections.size() > 200) {
            pruneCorrections();
        }
        pruneSecondOrder(this._bigrams, 12, 800);
        pruneSecondOrder(this._trigrams, 12, 800);
        if (this._kept.size() > 400) {
            pruneMap(this._kept, 400);
        }
        pruneSurfaces();
    }

    void pruneSecondOrder(final Map<?, Map<String, Entry>> table, int perKey, int totalMax) {
        int total = 0;
        for (Map<String, Entry> m : table.values()) {
            pruneMap(m, perKey);
            total += m.size();
        }
        if (total <= totalMax) {
            return;
        }
        List<Object[]> all = new ArrayList<>();
        for (Map.Entry<?, Map<String, Entry>> e : table.entrySet()) {
            for (String k : e.getValue().keySet()) {
                all.add(new Object[]{e.getKey(), k});
            }
        }
        final long n = now();
        Collections.sort(all, new Comparator<Object[]>() { // from class: juloo.keyboard2.LearningStore.2
            @Override // java.util.Comparator
            public int compare(Object[] a, Object[] b) {
                return Double.compare(LearningStore.score((Entry) ((Map) table.get(b[0])).get(b[1]), n), LearningStore.score((Entry) ((Map) table.get(a[0])).get(a[1]), n));
            }
        });
        for (int i = totalMax; i < all.size(); i++) {
            Map<String, Entry> m2 = table.get(all.get(i)[0]);
            if (m2 != null) {
                m2.remove(all.get(i)[1]);
                if (m2.isEmpty()) {
                    table.remove(all.get(i)[0]);
                }
            }
        }
    }

    void pruneSurfaces() {
        Iterator<Map.Entry<String, String>> it = this._surface.entrySet().iterator();
        while (it.hasNext()) {
            String k = it.next().getKey();
            if (!this._words.containsKey(k) && !this._kept.containsKey(k) && !this._personal.containsKey(k) && !this._protected.contains(k) && !this._corrections.containsKey(k)) {
                it.remove();
            }
        }
    }

    void pruneMap(Map<String, Entry> m, int max) {
        if (m.size() <= max) {
            return;
        }
        List<String> ranked = rankedKeys(m, now(), m.size());
        for (int i = max; i < ranked.size(); i++) {
            m.remove(ranked.get(i));
        }
    }

    void pruneCorrections() {
        List<Map.Entry<String, Correction>> l = new ArrayList<>(this._corrections.entrySet());
        final long n = now();
        Collections.sort(l, new Comparator<Map.Entry<String, Correction>>() { // from class: juloo.keyboard2.LearningStore.3
            @Override // java.util.Comparator
            public int compare(Map.Entry<String, Correction> a, Map.Entry<String, Correction> b) {
                long ageA = Math.max(0L, (n - a.getValue().lastSeen) / 86400000);
                long ageB = Math.max(0L, (n - b.getValue().lastSeen) / 86400000);
                double sa = ((double) a.getValue().count) / ((ageA / 7.0d) + 1.0d);
                double sb = ((double) b.getValue().count) / ((ageB / 7.0d) + 1.0d);
                return Double.compare(sb, sa);
            }
        });
        for (int i = 200; i < l.size(); i++) {
            this._corrections.remove(l.get(i).getKey());
        }
    }

    synchronized void save() {
        this._store.put(K_WORDS, encodeEntries(this._words));
        this._store.put(K_BIGRAMS, encodeBigrams());
        this._store.put(K_TRIGRAMS, encodeTrigrams());
        this._store.put(K_CORR, encodeCorrections());
        this._store.put(K_PERSONAL, encodeEntries(this._personal));
        this._store.put(K_KEPT, encodeEntries(this._kept));
        this._store.put(K_SURF, encodeSurface());
        StringBuilder b = new StringBuilder();
        for (String w : this._protected) {
            if (b.length() > 0) {
                b.append(';');
            }
            b.append(esc(w));
        }
        this._store.put(K_PROT, b.toString());
    }

    synchronized void load() {
        decodeEntries(this._store.get(K_WORDS, ""), this._words);
        decodeBigrams(this._store.get(K_BIGRAMS, ""));
        decodeTrigrams(this._store.get(K_TRIGRAMS, ""));
        decodeCorrections(this._store.get(K_CORR, ""));
        decodeEntries(this._store.get(K_PERSONAL, ""), this._personal);
        decodeEntries(this._store.get(K_KEPT, ""), this._kept);
        decodeSurface(this._store.get(K_SURF, ""));
        this._protected.clear();
        String ps = this._store.get(K_PROT, "");
        if (ps != null && !ps.isEmpty()) {
            for (String part : split(ps, ';')) {
                String w = unesc(part);
                if (w.length() >= 2) {
                    this._protected.add(w);
                }
            }
        }
        normalizeAll();
        maybeDecay();
    }

    synchronized void normalizeAll() {
        rekeyEntries(this._words);
        rekeyEntries(this._kept);
        rekeyEntries(this._personal);
        List<String> outers = new ArrayList<>(this._bigrams.keySet());
        Collections.sort(outers);
        Map<String, Map<String, Entry>> nb = new HashMap<>();
        for (String o : outers) {
            Map<String, Entry> m = this._bigrams.get(o);
            rekeyEntries(m);
            String no = key(o);
            if (no.length() >= 2) {
                Map<String, Entry> t = nb.get(no);
                if (t == null) {
                    nb.put(no, m);
                } else {
                    mergeEntryMap(m, t);
                }
            }
        }
        this._bigrams.clear();
        this._bigrams.putAll(nb);
        List<Pair> tps = new ArrayList<>(this._trigrams.keySet());
        Collections.sort(tps, new Comparator<Pair>() { // from class: juloo.keyboard2.LearningStore.4
            @Override // java.util.Comparator
            public int compare(Pair a, Pair b) {
                int c = a.first.compareTo(b.first);
                return c != 0 ? c : a.second.compareTo(b.second);
            }
        });
        Map<Pair, Map<String, Entry>> nt = new HashMap<>();
        for (Pair p : tps) {
            Map<String, Entry> m2 = this._trigrams.get(p);
            rekeyEntries(m2);
            Pair np = new Pair(key(p.first), key(p.second));
            if (np.first.length() >= 2 && np.second.length() >= 2) {
                Map<String, Entry> t2 = nt.get(np);
                if (t2 == null) {
                    nt.put(np, m2);
                } else {
                    mergeEntryMap(m2, t2);
                }
            }
        }
        this._trigrams.clear();
        this._trigrams.putAll(nt);
        List<String> tks = new ArrayList<>(this._corrections.keySet());
        Collections.sort(tks);
        Map<String, Correction> nc = new HashMap<>();
        for (String tk : tks) {
            Correction c = this._corrections.get(tk);
            String nk = key(tk);
            if (nk.length() >= 2) {
                Correction old = nc.get(nk);
                if (old == null || c.count > old.count || (c.count == old.count && c.to.compareTo(old.to) < 0)) {
                    nc.put(nk, c);
                    learnSurface(key(c.to), c.to, true);
                }
            }
        }
        this._corrections.clear();
        this._corrections.putAll(nc);
        List<String> ps = new ArrayList<>(this._protected);
        this._protected.clear();
        Collections.sort(ps);
        for (String w : ps) {
            String nk2 = key(w);
            if (nk2.length() >= 2) {
                this._protected.add(nk2);
                learnSurface(nk2, w, false);
            }
        }
    }

    private void rekeyEntries(Map<String, Entry> m) {
        List<Map.Entry<String, Entry>> l = new ArrayList<>(m.entrySet());
        Collections.sort(l, new Comparator<Map.Entry<String, Entry>>() { // from class: juloo.keyboard2.LearningStore.5
            @Override // java.util.Comparator
            public int compare(Map.Entry<String, Entry> a, Map.Entry<String, Entry> b) {
                int c = Integer.compare(b.getValue().count, a.getValue().count);
                return c != 0 ? c : a.getKey().compareTo(b.getKey());
            }
        });
        m.clear();
        for (Map.Entry<String, Entry> e : l) {
            String nk = key(e.getKey());
            if (nk.length() >= 2) {
                learnSurface(nk, e.getKey(), false);
                Entry old = m.get(nk);
                if (old == null) {
                    m.put(nk, new Entry(e.getValue().count, e.getValue().lastSeen));
                } else {
                    old.count += e.getValue().count;
                    old.lastSeen = Math.max(old.lastSeen, e.getValue().lastSeen);
                }
            }
        }
    }

    private static void mergeEntryMap(Map<String, Entry> src, Map<String, Entry> dst) {
        for (Map.Entry<String, Entry> e : src.entrySet()) {
            Entry old = dst.get(e.getKey());
            if (old == null) {
                dst.put(e.getKey(), new Entry(e.getValue().count, e.getValue().lastSeen));
            } else {
                old.count += e.getValue().count;
                old.lastSeen = Math.max(old.lastSeen, e.getValue().lastSeen);
            }
        }
    }

    synchronized void maybeDecay() {
        long last = 0;
        try {
            last = Long.parseLong(this._store.get(K_DECAY, "0"));
        } catch (Exception e) {
        }
        long t = now();
        if (last <= 0) {
            this._store.put(K_DECAY, String.valueOf(t));
            return;
        }
        if (t - last < DECAY_PERIOD_MS) {
            return;
        }
        for (Entry e2 : this._words.values()) {
            e2.count = Math.max(1, e2.count / 2);
        }
        for (Map<String, Entry> m : this._bigrams.values()) {
            for (Entry e3 : m.values()) {
                e3.count = Math.max(1, e3.count / 2);
            }
        }
        for (Map<String, Entry> m2 : this._trigrams.values()) {
            for (Entry e4 : m2.values()) {
                e4.count = Math.max(1, e4.count / 2);
            }
        }
        for (Correction c : this._corrections.values()) {
            c.count = Math.max(1, c.count / 2);
        }
        for (Entry e5 : this._personal.values()) {
            e5.count = Math.max(1, e5.count / 2);
        }
        for (Entry e6 : this._kept.values()) {
            e6.count = Math.max(1, e6.count / 2);
        }
        this._store.put(K_DECAY, String.valueOf(t));
        save();
    }

    static String esc(String s) {
        return s.replace("\\", "\\\\").replace(";", "\\;").replace("=", "\\=").replace(">", "\\>").replace(",", "\\,");
    }

    static String unesc(String s) {
        StringBuilder b = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                b.append(s.charAt(i + 1));
                i++;
            } else {
                b.append(c);
            }
            i++;
        }
        return b.toString();
    }

    static List<String> split(String s, char sep) {
        List<String> out = new ArrayList<>();
        StringBuilder b = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                b.append(c);
                b.append(s.charAt(i + 1));
                i++;
            } else if (c == sep) {
                out.add(b.toString());
                b.setLength(0);
            } else {
                b.append(c);
            }
            i++;
        }
        out.add(b.toString());
        return out;
    }

    String encodeSurface() {
        StringBuilder b = new StringBuilder();
        List<String> ks = new ArrayList<>(this._surface.keySet());
        Collections.sort(ks);
        for (String k : ks) {
            if (b.length() > 0) {
                b.append(';');
            }
            b.append(esc(k)).append('=').append(esc(this._surface.get(k)));
        }
        return b.toString();
    }

    void decodeSurface(String s) {
        this._surface.clear();
        if (s == null || s.isEmpty()) {
            return;
        }
        for (String part : split(s, ';')) {
            List<String> kv = split(part, '=');
            if (kv.size() == 2) {
                this._surface.put(unesc(kv.get(0)), unesc(kv.get(1)));
            }
        }
    }

    static String encodeEntries(Map<String, Entry> m) {
        StringBuilder b = new StringBuilder();
        for (Map.Entry<String, Entry> e : m.entrySet()) {
            if (b.length() > 0) {
                b.append(';');
            }
            b.append(esc(e.getKey())).append('=').append(e.getValue().count).append(',').append(e.getValue().lastSeen);
        }
        return b.toString();
    }

    static void decodeEntries(String s, Map<String, Entry> m) {
        m.clear();
        if (s == null || s.isEmpty()) {
            return;
        }
        for (String part : split(s, ';')) {
            List<String> kv = split(part, '=');
            if (kv.size() == 2) {
                List<String> ct = split(kv.get(1), ',');
                if (ct.size() == 2) {
                    try {
                        m.put(unesc(kv.get(0)), new Entry(Integer.parseInt(ct.get(0)), Long.parseLong(ct.get(1))));
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    String encodeBigrams() {
        StringBuilder b = new StringBuilder();
        for (Map.Entry<String, Map<String, Entry>> e : this._bigrams.entrySet()) {
            for (Map.Entry<String, Entry> f : e.getValue().entrySet()) {
                if (b.length() > 0) {
                    b.append(';');
                }
                b.append(esc(e.getKey())).append('>').append(esc(f.getKey())).append('=').append(f.getValue().count).append(',').append(f.getValue().lastSeen);
            }
        }
        return b.toString();
    }

    String encodeTrigrams() {
        StringBuilder b = new StringBuilder();
        List<Pair> ps = new ArrayList<>(this._trigrams.keySet());
        Collections.sort(ps, new Comparator<Pair>() { // from class: juloo.keyboard2.LearningStore.6
            @Override // java.util.Comparator
            public int compare(Pair a, Pair b2) {
                int c = a.first.compareTo(b2.first);
                return c != 0 ? c : a.second.compareTo(b2.second);
            }
        });
        for (Pair p : ps) {
            Map<String, Entry> m = this._trigrams.get(p);
            List<String> ws = new ArrayList<>(m.keySet());
            Collections.sort(ws);
            for (String w : ws) {
                if (b.length() > 0) {
                    b.append(';');
                }
                b.append(esc(p.first)).append('>').append(esc(p.second)).append('>').append(esc(w)).append('=').append(m.get(w).count).append(',').append(m.get(w).lastSeen);
            }
        }
        return b.toString();
    }

    void decodeTrigrams(String s) {
        this._trigrams.clear();
        if (s == null || s.isEmpty()) {
            return;
        }
        for (String part : split(s, ';')) {
            List<String> kv = split(part, '=');
            if (kv.size() == 2) {
                List<String> trio = split(kv.get(0), '>');
                if (trio.size() == 3) {
                    List<String> ct = split(kv.get(1), ',');
                    if (ct.size() == 2) {
                        try {
                            Pair p = new Pair(unesc(trio.get(0)), unesc(trio.get(1)));
                            Map<String, Entry> m = this._trigrams.get(p);
                            if (m == null) {
                                m = new HashMap();
                                this._trigrams.put(p, m);
                            }
                            m.put(unesc(trio.get(2)), new Entry(Integer.parseInt(ct.get(0)), Long.parseLong(ct.get(1))));
                        } catch (Exception e) {
                        }
                    }
                }
            }
        }
    }

    void decodeBigrams(String s) {
        this._bigrams.clear();
        if (s == null || s.isEmpty()) {
            return;
        }
        for (String part : split(s, ';')) {
            List<String> kv = split(part, '=');
            if (kv.size() == 2) {
                List<String> pair = split(kv.get(0), '>');
                if (pair.size() == 2) {
                    List<String> ct = split(kv.get(1), ',');
                    if (ct.size() == 2) {
                        try {
                            String prev = unesc(pair.get(0));
                            Map<String, Entry> m = this._bigrams.get(prev);
                            if (m == null) {
                                m = new HashMap();
                                this._bigrams.put(prev, m);
                            }
                            m.put(unesc(pair.get(1)), new Entry(Integer.parseInt(ct.get(0)), Long.parseLong(ct.get(1))));
                        } catch (Exception e) {
                        }
                    }
                }
            }
        }
    }

    String encodeCorrections() {
        StringBuilder b = new StringBuilder();
        for (Map.Entry<String, Correction> e : this._corrections.entrySet()) {
            if (b.length() > 0) {
                b.append(';');
            }
            b.append(esc(e.getKey())).append('=').append(esc(e.getValue().to)).append(',').append(e.getValue().count).append(',').append(e.getValue().lastSeen);
        }
        return b.toString();
    }

    void decodeCorrections(String s) {
        this._corrections.clear();
        if (s == null || s.isEmpty()) {
            return;
        }
        for (String part : split(s, ';')) {
            List<String> kv = split(part, '=');
            if (kv.size() == 2) {
                List<String> tv = split(kv.get(1), ',');
                if (tv.size() == 3) {
                    try {
                        this._corrections.put(unesc(kv.get(0)), new Correction(unesc(tv.get(0)), Integer.parseInt(tv.get(1)), Long.parseLong(tv.get(2))));
                    } catch (Exception e) {
                    }
                }
            }
        }
    }
}
