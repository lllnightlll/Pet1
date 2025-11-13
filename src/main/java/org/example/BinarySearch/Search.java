package org.example.BinarySearch;

import java.util.Scanner;

public class Search {
    private static Scanner in = new Scanner(System.in);
    private static int n;
    private static int m;
    private int[] N;
    private int[] M;

    static void add(int i, int x[]) {
        for (int j = 0; j < i; j++) {
            x[j] = in.nextInt();
        }
    }

    static int searchElement(int serched, int x[]) {
        int sum = 0;
        for (int i = x.length / 2; i != 0; i /= 2) {
            if (x[i + sum] == serched)
                return i;
            else if (x[i] < serched)
                sum += i;
        }
        return sum;
    }

    public Search() {
        n = in.nextInt();
        N = new int[n];
        add(n, N);
        m = in.nextInt();
        M = new int[m];
        add(m, M);
        Print();
    }

    void Print() {
        for (int i = 0; i < m; i++) {
            System.out.println(N[searchElement(M[i], N)]);
        }
    }
}
