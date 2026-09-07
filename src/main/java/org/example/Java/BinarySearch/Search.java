package org.example.Java.BinarySearch;

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
                return sum + i;
            else if (x[i + sum] < serched)
                sum += i;
            // что-то не то со второй половиной
        }

        if (serched < x[x.length - 1]) {
            System.out.println((x[sum] - serched) + " " + (x[sum + 1] - serched));
            if ((x[sum] - serched) > (x[sum + 1] - serched)) System.out.println("true");
            if ((serched - x[sum]) > (x[sum + 1] - serched)) return (sum + 1);
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
