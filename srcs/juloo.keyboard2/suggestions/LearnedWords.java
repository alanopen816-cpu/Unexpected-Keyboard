package juloo.keyboard2.suggestions;

import java.util.LinkedHashSet;

public final class LearnedWords
{
  private static final int MAX_WORDS = 500;
  private final LinkedHashSet<String> words = new LinkedHashSet<>();

  public void add(String word)
  {
    if (word == null)
      return;
    word = word.trim();
    if (word.length() < 2)
      return;
    words.remove(word);
    words.add(word);
    if (words.size() > MAX_WORDS)
      words.remove(words.iterator().next());
  }

  public boolean contains(String word)
  {
    return words.contains(word);
  }

  public boolean remove(String word)
  {
    return words.remove(word);
  }

  public void clear()
  {
    words.clear();
  }

  public int size()
  {
    return words.size();
  }

  public String toPersistedString()
  {
    StringBuilder result = new StringBuilder();
    for (String word : words)
    {
      if (result.length() > 0)
        result.append('\n');
      result.append(word);
    }
    return result.toString();
  }

  public static LearnedWords fromPersistedString(String value)
  {
    LearnedWords result = new LearnedWords();
    if (value != null && !value.isEmpty())
      for (String word : value.split("\\n", -1))
        result.add(word);
    return result;
  }
}
