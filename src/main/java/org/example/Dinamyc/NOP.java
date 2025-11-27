package org.example.Dinamyc;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class NOP {
    private char[] x;
    private char[] y;

    public NOP() {
        try (BufferedReader file = new BufferedReader(new InputStreamReader(new FileInputStream("lcs-input.txt"), "UTF-8"))) {
            String line;
            int i = 0;
            while ((line = file.readLine()) != null) {
                if (i == 0) {
                    x = line.toCharArray();
                    i++;
                }
                else {
                    y = line.toCharArray();
                }
            }
            nop(x, y);
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

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
