package juloo.keyboard2.suggestions;

import java.util.Arrays;
import java.util.List;
import juloo.cdict.Cdict;
import juloo.keyboard2.dict.Dictionaries;
import juloo.keyboard2.Config;
import juloo.keyboard2.ComposeKey;
import juloo.keyboard2.ComposeKeyData;
import juloo.keyboard2.LearningStore;
import juloo.keyboard2.TextEngine;

/** Keep track of the word being typed and provide suggestions for
    [CandidatesView]. */
public final class Suggestions
{
  Callback _callback;
  Config _config;
  boolean _enabled;
  LearningStore _learned;
  TextEngine.WordSource _source;

  /** Current suggestions. The best suggestion is at index [0]. */
  public String[] suggestions = new String[MAX_COUNT];
  /** Number of suggestions at the beginning of the [suggestions] array that
      are not [null]. */
  public int count = 0;
  public String emoji_suggestion = null;
  /** Number of suggestions in [suggestions]. */
  public static final int MAX_COUNT = 3;

  public Suggestions(Callback c, Config conf)
  {
    _callback = c;
    _config = conf;
  }

  public void set_sources(TextEngine.WordSource src, LearningStore learned)
  {
    _source = src;
    _learned = learned;
  }

  public void started()
  {
    _enabled = _config.editor_config.should_show_candidates_view;
    clear();
  }

  public void currently_typed_word(String word)
  {
    if (!_enabled || !_config.suggestions_enabled)
      return;
    if (word.length() < 2)
      clear();
    else
      query_suggestions(word);
    _callback.set_suggestions(this);
  }

  void clear()
  {
    count = 0;
    for (int i = 0; i < MAX_COUNT; i++)
      suggestions[i] = null;
    emoji_suggestion = null;
  }

  int query_suggestions(String word)
  {
    TextEngine.WordSource src = _source == null ? TextEngine.EMPTY : _source;
    boolean first_char_upper = Character.isUpperCase(word.charAt(0));
    word = apply_substitutions(word);
    TextEngine.Result r = TextEngine.suggest(null, word, src, _learned);
    int i = fill_strip(suggestions, word, r, src, _learned);
    if (first_char_upper)
      capitalize_results(suggestions, i);
    emoji_suggestion = query_emoji(word); // word with substitutions applied
    count = i;
    return i;
  }

  static int fill_strip(String[] out, String word, TextEngine.Result r,
      TextEngine.WordSource src, LearningStore learned)
  {
    int n = Math.min(out.length, MAX_COUNT);
    int i = 0;
    if (src.contains(word) || src.contains(TextEngine.normalize(word))
        || (learned != null && (learned.isPersonal(word) || learned.isProtected(word))))
    {
      out[i++] = word;
    }
    for (String candidate : r.suggestions)
    {
      if (i >= n)
        break;
      if (!candidate.equals(word))
        out[i++] = candidate;
    }
    return i;
  }

  static void capitalize_results(String[] s, int count)
  {
    for (int i = 0; i < count; i++)
      s[i] = s[i].substring(0, 1).toUpperCase() + s[i].substring(1);
  }

  String query_emoji(String word)
  {
    Cdict dict = _config.emoji_dictionary;
    // Disable emoji suggestion for short words
    if (dict == null || word.length() < 3)
      return null;
    Cdict.Result r = dict.find(word);
    if (r.found)
      return dict.word(r.index);
    int[] s = dict.suffixes(r, 1);
    if (s.length > 0)
      return dict.word(s[0]);
    return null;
  }

  /** Apply the same substitutions that were used when building the
      dictionaries to find word aliases. This catches missing diacritics for
      example. */
  String apply_substitutions(String w)
  {
    StringBuilder b = new StringBuilder(w);
    int len = w.length();
    for (int i = 0; i < len; i++)
    {
      char r =
        ComposeKey.transform_char(ComposeKeyData.substitutions, b.charAt(i));
      if (r != 0) b.setCharAt(i, r);
    }
    return b.toString();
  }

  public static interface Callback
  {
    public void set_suggestions(Suggestions suggestions);
  }
}
