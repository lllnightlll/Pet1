package org.example.Hash;

public class HashMapImpl<V> implements HashMap<String, V> {
    private static final int TABLE_SIZE = 300007;
    private static final int A = 31;
    private MAP<V>[] HashMAP = new MAP[TABLE_SIZE];

    private static class MAP<V> {
        private final V VALUE;

        MAP(V VALUE) {
            this.VALUE = VALUE;
        }

        V getVALUE() {
            return VALUE;
        }
    }

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
        this.HashMAP[(int) hashByString(key)] = new MAP<V>(value);
    }

    @Override
    public V getValue(String key) {
        int hash = (int) hashByString(key);
        return this.HashMAP[hash] != null ? HashMAP[hash].getVALUE() : null;
    }

    @Override
    public V delete(String key) {
        this.HashMAP[(int) hashByString(key)] = null;
        return null;
    }
}