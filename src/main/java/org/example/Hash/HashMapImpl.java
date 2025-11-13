package org.example.Hash;

public class HashMapImpl<V> implements HashMap<String, V> {
    private static final int TABLE_SIZE = 300007;
    private static final int A = 31;

    /**
     * Алгоритм хеширования строк.
     *
     * @param value ключ, по которому сохранится значение в хэш-таблицу
     * @return значение хэша
     */
    private long hashByString(String value) {
        long hash = 0;
        for (Character ch : value.toCharArray()) {
            hash = (hash * A + ch) % TABLE_SIZE;
        }
        return hash;
    }

    @Override
    public void put(String key, V value) {
        // TODO
    }

    @Override
    public V getValue(String key) {
        // TODO
        return null;
    }

    @Override
    public V delete(String key) {
        // TODO
        return null;
    }
}