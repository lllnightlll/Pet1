package org.example.minimums;

import org.example.Bot;

public class MinStackImpl implements MinStack<Integer> {
    private int n = 99999;
    private static int max = 0;
    int[] stack;

    MinStackImpl() {
        stack = new int[n];
    }

    MinStackImpl(int k) {
        if (k > 0) {
            stack = new int[k];
            n = k;
        }

    }

    @Override
    public void push(Integer value) {
        try {
            if (max > n) {
                throw new Exception("So many elements");
            }
            stack[max++] = value;
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }

    @Override
    public Integer pop() {
        try {
            if (max == 0) {
                throw new Exception("Stack null");
            }
            max--;
        } catch (Exception ex) {
            System.out.println(ex);
        }
        return null;
    }

    @Override
    public Integer top() {
        try {
            if (max == 0) {
                throw new Exception("Stack null");
            }
            return stack[max-1];
        } catch (Exception ex) {
            System.out.println(ex);
            return null;
        }
    }

    @Override
    public Integer min() {
        try {
            if (max == 0) {
                throw new Exception("Stack null");
            }
            int min = stack[0];
            for (int i = 1; i < max; i++) {
                if (stack[i] < min) {
                    min = stack[i];
                }
            }
            return min;
        } catch (Exception ex) {
            System.out.println(ex);
            return null;
        }
    }
}