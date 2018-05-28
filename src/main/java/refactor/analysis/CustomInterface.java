package refactor.analysis;

public interface CustomInterface<K, V> {
    V apply(K key);
    V apply(K key1, K key2);
}
