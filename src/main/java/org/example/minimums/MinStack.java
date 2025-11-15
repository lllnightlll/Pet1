package org.example.minimums;

public interface MinStack<T extends Number> {
    void push(T value) throws Exception;

    T pop() throws Exception;

    T top() throws Exception;

    T min() throws Exception;
}