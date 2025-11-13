package org.example.Hash;

public interface HashMap<K, V> {
    /**
     * Сохраняет ключ key и значение value таблицу.
     * Если ключ key уже существует, то перезаписать значение на новое value.
     *
     * @param key   ключ для сохранения
     * @param value значение для сохранения
     */
    void put(K key, V value);

    /**
     * Вернуть значение по ключу.
     *
     * @param key ключ для поиска
     * @return найденное значение или null, если ничего не найдено
     */
    V getValue(K key);

    /**
     * Удалить значение по ключу.
     *
     * @param key ключ для поиска
     * @return удалённое значение или null, если ничего не найдено
     */
    V delete(K key);
}