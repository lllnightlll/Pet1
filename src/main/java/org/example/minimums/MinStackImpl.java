package org.example.minimums;

import org.example.Bot;

import java.util.LinkedList;
import java.util.List;

public class MinStackImpl implements MinStack<Integer> {
    private int n = 100000;

    private List<MinStackNode> data = new LinkedList<>();

    private class MinStackNode {
        private int value;
        private int min;

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
    public void push(Integer value) {
        try {
            if (data.size() > n) {
                throw new Exception("So many elements");
            }
            if (data.isEmpty()) {
                data.add(new MinStackNode(value, value));
            } else {
                data.add(new MinStackNode(value, data.getLast().getMin()));
            }

        } catch (Exception ex) {
            System.out.println(ex);
        }
    }

    @Override
    public Integer pop() {
        try {
            if (data.isEmpty()) {
                throw new Exception("Stack null");
            }
            data.removeLast();
        } catch (Exception ex) {
            System.out.println(ex);
        }
        return null;
    }

    @Override
    public Integer top() {
        try {
            if (data.isEmpty()) {
                throw new Exception("Stack null");
            }
            return data.getLast().getValue();
        } catch (Exception ex) {
            System.out.println(ex);
            return null;
        }
    }

    @Override
    public Integer min() {
        try {
            if (data.isEmpty()) {
                throw new Exception("Stack null");
            }
            return data.getLast().getMin();
        } catch (Exception ex) {
            System.out.println(ex);
            return null;
        }
    }
}