package org.example;
import org.example.Hash.HashMap;
import org.example.Hash.HashMapImpl;
import org.junit.jupiter.api.Test;

public class HashMapTest {
    private final HashMap<String, String> x = new HashMapImpl<String>();

    @Test
    void test() {
        x.put("hello", "world");
        x.put("name", "ilya");
        System.out.println(x.getValue("hello"));
        System.out.println(x.getValue("ilya"));
        x.delete("hello");
        System.out.println(x.getValue("hello"));
        System.out.println(x.getValue("name"));
        x.put("name", "vasya");
        System.out.println(x.getValue("name"));
        x.delete("name");
        System.out.println(x.getValue("name"));


    }
}
