package org.example.Java.minimums;

import java.util.LinkedList;
import java.util.List;

public class MinStackImpl implements MinStack<Integer> {
    private final int MAX_SIZE = 100_000;

    private final List<MinStackNode> data = new LinkedList<>();

    private class MinStackNode {
        private final int value;
        private final int min;

        MinStackNode(int value, int min) {
            this.value = value;
            this.min = Math.min(value, min);
        }

        public int getMin() {
            return min;
        }

        public int getValue() {
            return value;
        }
    }

    @Override
    public void push(Integer value) throws Exception {
        if (data.size() > MAX_SIZE) {
            throw new Exception("So many elements");
        }
        if (data.isEmpty()) {
            data.add(new MinStackNode(value, value));
        } else {
            data.add(new MinStackNode(value, data.getLast().getMin()));
        }
    }

    @Override
    public Integer pop() throws Exception {
        if (data.isEmpty()) {
            throw new Exception("Stack null");
        }
        return data.removeLast().getValue();
    }

    @Override
    public Integer top() throws Exception {
        if (data.isEmpty()) {
            throw new Exception("Stack null");
        }
        return data.getLast().getValue();
    }

    @Override
    public Integer min() throws Exception {
        if (data.isEmpty()) {
            throw new Exception("Stack null");
        }
        return data.getLast().getMin();
    }
}