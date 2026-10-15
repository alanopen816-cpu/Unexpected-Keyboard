package juloo.keyboard2;

public interface KvStore {
    String get(String key, String defaultValue);
    void put(String key, String value);
    void remove(String key);
}
