package org.example.Hash;

import java.util.ArrayList;
import java.util.List;

public class HashMapImpl<V> implements HashMap<String, V> {
    private static final int TABLE_SIZE = 300007;
    private static final int A = 31;
    private final List<MAP<V>>[] map = new List[TABLE_SIZE];

    private static class MAP<V> {
        private final String KEY;
        private final V VALUE;

        MAP(String KEY, V VALUE) {
            this.KEY = KEY;
            this.VALUE = VALUE;
        }
        String getKEY() { return  KEY;}
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
    private static int hashByString(String value) {
        int hash = 0;
        for (Character ch : value.toCharArray()) {
            hash = (hash * A + ch) % TABLE_SIZE;
        }
        return hash;
    }

    @Override
    public void put(String key, V value) {
        int hash = hashByString(key);
        if (this.map[hash] == null) {
            this.map[hash] = new ArrayList<>();
        }
        this.map[hash].addFirst(new MAP<V>(key, value));
    }

    @Override
    public V getValue(String key) {
        int hash = hashByString(key);
        if(this.map[hash] != null)
            for(int i = 0; i < this.map[hash].size(); i++) {
                if(this.map[hash].get(i).getKEY().equals(key))
                    return this.map[hash].get(i).getVALUE(); }
        return null;
    }

    @Override
    public V delete(String key) {
        int hash = hashByString(key);
        if(this.map[hash] != null)
            for(int i = 0; i < this.map[hash].size(); i++) {
                if(this.map[hash].get(i).getKEY().equals(key))
                    this.map[hash].remove(i); i--; }
        return null;
    }
}