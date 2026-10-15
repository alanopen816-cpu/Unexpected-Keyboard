package juloo.keyboard2.suggestions;

public final class LearnedWordsCheck
{
  public static void main(String[] args)
  {
    LearnedWords words = new LearnedWords();
    words.add(" hello ");
    check(words.contains("hello"), "add and trim");
    words.add("hello");
    check(words.size() == 1, "duplicates");
    words.add(null);
    words.add("");
    words.add(" ");
    words.add("a");
    check(words.size() == 1, "invalid words");
    words.remove("hello");
    check(!words.contains("hello") && words.size() == 0, "remove");

    for (int i = 0; i < 505; i++)
      words.add("word" + i);
    check(words.size() == 500, "capacity");
    check(!words.contains("word0") && words.contains("word504"), "oldest evicted");

    LearnedWords restored = LearnedWords.fromPersistedString(words.toPersistedString());
    check(restored.size() == 500 && restored.contains("word5") && restored.contains("word504"), "round trip");
    restored.clear();
    check(restored.size() == 0, "clear");
  }

  private static void check(boolean condition, String message)
  {
    if (!condition)
      throw new AssertionError(message);
  }
}
