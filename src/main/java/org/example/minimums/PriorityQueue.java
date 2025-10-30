package org.example.minimums;

public interface PriorityQueue<T extends Number> {
    void enqueue(T value);

    T dequeueMax();

    void increment(long operation, T addition);
}