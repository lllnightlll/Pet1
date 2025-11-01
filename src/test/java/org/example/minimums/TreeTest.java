package org.example.minimums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TreeTest {
    private final PriorityQueue<Integer> tree = new PriorityQueueImpl(100000);

    @Test
    void test() {
        tree.enqueue(10);
        tree.enqueue(42);
        tree.enqueue(9);
        assertEquals(42, tree.dequeueMax());
        tree.increment(3, 10);
        assertEquals(19, tree.dequeueMax());
        assertEquals(10, tree.dequeueMax());
        assertNull(tree.dequeueMax());
    }
}
