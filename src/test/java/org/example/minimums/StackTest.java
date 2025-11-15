package org.example.minimums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StackTest {
    private final MinStack<Integer> stack = new MinStackImpl();

    @Test
    void test() throws Exception {
        stack.push(42);
        System.out.println(stack.top());
        System.out.println(stack.min());
        stack.push(12);
        System.out.println(stack.min());
        stack.push(13);
        System.out.println(stack.top());
        System.out.println(stack.min());
        stack.pop();
        stack.pop();
        System.out.println(stack.min());
    }
}