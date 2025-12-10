package org.example.Requests;

import java.util.Scanner;

public class PrefixAmounts {
    private int[] A;
    private int[] S;
    private int[] K;
    private final Scanner in = new Scanner(System.in);

    public PrefixAmounts () {
        elements();
        amountA();
        requests();
        cout();
    }

    private void elements () {
        int x = 0;
        while (x < 1 || x > 100000) {
            x = in.nextInt();
        }
        A = new int[x];
        x = 0xFF_FF_FF_FF >>> 1;
        for (int i = 0; i < A.length; i++) {
            while (x < -128 || x > 127) {
                x = in.nextInt();
            }
            A[i] = x;
            x = 0xFF_FF_FF_FF >>> 1;
        }
    }

    private void requests () {
        int x = 0;
        int y = 0xFF_FF_FF_FF >>> 1;
        while (x < 1 || x > 10000000) {
            x = in.nextInt();
        }
        K = new int[x];
        x = 0xFF_FF_FF_FF >>> 1;

        for (int i = 0; i < K.length; i++) {
            while (x < 0 || y < 0 || x > y || y >= A.length) {
                x = in.nextInt();
                y = in.nextInt();
            }
            K[i] = returnAmount(x, y);
            x = 0xFF_FF_FF_FF >>> 1;
            y = 0xFF_FF_FF_FF >>> 1;
        }
    }

    private void amountA () {
        S = new int[A.length];
        S[0] = A[0];
        for (int i = 1; i < A.length; i++) {
            S[i] = A[i] + S[i-1];
        }
    }

    private int returnAmount (int i, int j) {
        if (i == 0) return S[j];
        return S[j] - S[i-1];
    }

    private void cout() {
        for (int j : K) {
            System.out.println(j);
        }
    }
}
