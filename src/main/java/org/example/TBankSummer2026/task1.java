package org.example.TBankSummer2026;

import java.util.Scanner;

public class task1 {
    public static void main(String[] args) {
        @SuppressWarnings("resource")
        Scanner in = new Scanner(System.in);
        int n = in.nextInt();
        int[] a = new int[n];
        int max = 0;
        for (int i = 0; i < n; i++) {
            a[i] = in.nextInt();
            if (a[i] > max) {
                max = a[i];
            }
        }

        int first = -1;
        int second = -1;
        int smaller = 0;
        for (int x : a) {
            if (x != max) {
                smaller++;
                if (first == -1) {
                    first = x;
                } else {
                    second = x;
                }
            }
        }

        if (smaller == 2 && first + second == max) {
            System.out.println("YES");
        } else {
            System.out.println("NO");
        }
    }
}
