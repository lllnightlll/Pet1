package org.example.TBankSummer2026.Prakticum;

import java.util.Scanner;

public class task2 {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        int t = in.nextInt();
        StringBuilder out = new StringBuilder();
        while (t-- > 0) {
            int n = in.nextInt();
            int[] p = new int[n];
            for (int i = 0; i < n; i++) {
                p[i] = in.nextInt();
            }
            out.append(canSort(p) ? "YES\n" : "NO\n");
        }
        System.out.print(out);
    }

    static boolean canSort(int[] p) {
        int n = p.length;
        if (n < 4) {
            return isSorted(p);
        }
        if (n == 4) {
            return isSorted(p) || (p[0] == 3 && p[1] == 4 && p[2] == 1 && p[3] == 2);
        }
        return inversionParity(p) == 0;
    }

    static boolean isSorted(int[] p) {
        for (int i = 0; i < p.length; i++) {
            if (p[i] != i + 1) {
                return false;
            }
        }
        return true;
    }

    static int inversionParity(int[] p) {
        int inv = 0;
        for (int i = 0; i < p.length; i++) {
            for (int j = i + 1; j < p.length; j++) {
                if (p[i] > p[j]) {
                    inv++;
                }
            }
        }
        return inv & 1;
    }
}
