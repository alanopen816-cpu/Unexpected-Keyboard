package juloo.keyboard2;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.provider.UserDictionary;
import java.util.ArrayList;
import java.util.List;

/** Word source backed by Android's user dictionary. */
public final class UserDictionarySource implements TextEngine.WordSource {
    private final ContentResolver resolver;

    public UserDictionarySource(ContentResolver resolver) { this.resolver = resolver; }

    @Override public List<String> completions(String prefix, int limit) {
        List<String> words = new ArrayList<>();
        Cursor cursor = resolver.query(UserDictionary.Words.CONTENT_URI,
                new String[] { UserDictionary.Words.WORD },
                UserDictionary.Words.WORD + " LIKE ?", new String[] { prefix + "%" },
                UserDictionary.Words.FREQUENCY + " DESC");
        if (cursor == null) return words;
        try {
            while (cursor.moveToNext() && words.size() < limit)
                words.add(cursor.getString(0));
        } finally { cursor.close(); }
        return words;
    }

    @Override public boolean contains(String word) {
        Cursor cursor = resolver.query(UserDictionary.Words.CONTENT_URI,
                new String[] { UserDictionary.Words._ID }, UserDictionary.Words.WORD + "=?",
                new String[] { word }, null);
        if (cursor == null) return false;
        try { return cursor.moveToFirst(); }
        finally { cursor.close(); }
    }

    @Override public List<String> similar(String word, int limit) { return new ArrayList<>(); }

    public void addWord(String word) {
        ContentValues values = new ContentValues();
        values.put(UserDictionary.Words.WORD, word);
        values.put(UserDictionary.Words.FREQUENCY, 1);
        resolver.insert(UserDictionary.Words.CONTENT_URI, values);
    }
}
