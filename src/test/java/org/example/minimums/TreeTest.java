package org.example.minimums;

import org.junit.jupiter.api.Test;

public class TreeTest {
    private final PriorityQueue<Integer> tree = new PriorityQueueImpl(100000);

    @Test
    void test() {
        tree.enqueue(10);
        tree.enqueue(42);
        tree.enqueue(9);
        tree.dequeueMax();
        tree.increment(3, 10);
        tree.dequeueMax();
        tree.dequeueMax();
        tree.dequeueMax();
    }
}
