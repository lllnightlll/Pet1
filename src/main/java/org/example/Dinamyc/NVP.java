package org.example.Dinamyc;

public class NVP {
    private int n;
    private int[] x;

    public static void nvp(int n, int[] x) {
        int max = 1;
        int pos = 0;
        int[] data = new int[n];
        for (int i = 0; i < n; i++) {
            int sum = 1;
            int k = i;
            for (int j = i; j < n; j++) {
                if (x[j] > x[k]) {
                    sum++;
                    k = j;
                    data[i] = sum;
                }
            }
            /*if (sum > max) {
                max = sum;
                pos = i;
            }*/
        }
        //System.out.println(max);
        //System.out.printf(x[n-1] + " ");
        int k = n - 1;
        int j = 1;
        data[0] = x[k];
        for (int i = n - 1; i >= pos; i--) {
            if (x[i] < x[k]) {
                k = i;
                data[j++] = x[i];
                //System.out.printf(x[i] + " ");
            }
        }
        System.out.println(j);
        for (int i = j - 1; i > -1; i--) {
            System.out.printf(data[i] + " ");
        }
    }
}
