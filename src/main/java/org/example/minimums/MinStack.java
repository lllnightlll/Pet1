package org.example.minimums;

public interface MinStack<T extends Number> {
    void push(T value);

    T pop();

    T top();

    T min();
}