package org.example.Dinamyc;

public class NOP {
    private char[] x;
    private char[] y;

    public static void nop(char[] x, char[] y) {
        int z = 0;

        for (int k = 0; k < x.length; k++) {
            int j = k;
            int sum = 0;
            for (int i = 0; i < y.length; i++) {
                if (x[j] == y[i]) {
                    j++;
                    sum++;
                } else {
                    if (z < sum) {
                        z = sum;
                    }
                    j = k;
                    sum = 0;
                }
            }
        }
        System.out.println(z);
    }
}
