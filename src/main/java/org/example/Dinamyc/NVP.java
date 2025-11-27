package org.example.Dinamyc;

public class NVP {
    private int n;
    private int[] x;

    public static void nvp(int n, int[] x) {
        int max = 0;
        int pos = 0;
        for (int i = 0; i < n; i++) {
            int sum = 0;
            for (int j = i; j < n - 1; j++) {
                if (x[j+1] > x[j]) {
                    sum++;
                }
            }
            if (sum > max) {
                max = sum;
                pos = i;
            }
        }
        System.out.println(max);
        System.out.printf(x[pos] + " ");
        for (int i = pos; i < n - 1; i++) {
            if (x[i + 1] < x[i]) {
                System.out.printf(x[i+1] + " ");
            }
        }
    }
}
