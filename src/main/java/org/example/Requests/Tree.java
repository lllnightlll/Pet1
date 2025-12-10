package org.example.Requests;

import java.util.Scanner;

import static java.lang.Math.min;

public class Tree {
    private int[] A;
    private BinaryTree dataTree;
    private final Scanner in = new Scanner(System.in);
    private int[] K;

    private class BinaryTree {
        int data, left, right;
        BinaryTree L, R;

        BinaryTree(int left, int right) {
            this.left = left;
            this.right = right;
            if (left == right) {
                this.data = A[left];
            } else {
                int mid = (right - left) / 2 + left;
                this.L = new BinaryTree(left, mid);
                this.R = new BinaryTree(mid + 1, right);
                this.data = min(L.data, R.data);
            }
        }

        private int find(int l, int r) {
            if (r < left || right < l) {
                return 0xFF_FF_FF_FF >>> 1;
            }
            if (l <= left && right <= r) {
                return data;
            }
            int leftMin = L != null ? L.find(l, r) : 0xFF_FF_FF_FF >>> 1;
            int rightMin = R != null ? R.find(l, r) : 0xFF_FF_FF_FF >>> 1;
            return min(leftMin, rightMin);
        }
    }


    public Tree() {
        elements();
        dataTree = new BinaryTree(0, A.length - 1);
        requests();
        cout();
    }

    private void elements() {
        int x = 0;
        while (x < 1 || x > 100000) {
            x = in.nextInt();
        }
        A = new int[x];
        x = 0xFF_FF_FF_FF >>> 1;
        for (int i = 0; i < A.length; i++) {
            while (x < -10000000 || x > 10000000) {
                x = in.nextInt();
            }
            A[i] = x;
            x = 0xFF_FF_FF_FF >>> 1;
        }
    }

    private void requests() {
        int x = 0;
        int y = 0xFF_FF_FF_FF >>> 1;
        while (x < 1 || x > 10000000) {
            x = in.nextInt();
        }
        K = new int[x];
        x = 0xFF_FF_FF_FF >>> 1;

        for (int i = 0; i < K.length; i++) {
            while (x < 0 || y < 0 || x > y || y > A.length) {
                x = in.nextInt();
                y = in.nextInt();
            }
            K[i] = find(x, y-1);
            x = 0xFF_FF_FF_FF >>> 1;
            y = 0xFF_FF_FF_FF >>> 1;
        }
    }

    private int find(int l, int r) {
        return dataTree.find(l, r);
    }

    private void cout() {
        for (int j : K) {
            System.out.println(j);
        }
    }
}
