package org.example.Java.Dinamyc;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class NOP {
    private char[] x;
    private char[] y;

    public NOP(char[] x, char[] y) {
        System.out.println(nop(x, y));
    }

    public NOP(String inputFile) {
        writeByFile(inputFile);
    }

    private void writeByFile(String inputFile) {
        readFile(inputFile);
        System.out.println(nop(x, y));
    }

    private void readFile(String inputFile) {
        try (BufferedReader file = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), "UTF-8"))) {
            String line;
            int i = 0;
            while ((line = file.readLine()) != null) {
                if (i == 0) {
                    this.x = line.toCharArray();
                    i++;
                }
                else {
                    this.y = line.toCharArray();
                }
            }
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public static int nop(char[] x, char[] y) {
        int maxLength = 0;
        for (int k = 0; k < x.length; k++) {
            int j = k;
            int sum = 0;
            for (int i = 0; i < y.length; i++) {
                if (x[j] == y[i]) {
                    j++;
                    sum++;
                } else {
                    if (maxLength < sum) {
                        maxLength = sum;
                    }
                    j = k;
                    sum = 0;
                }
            }
        }
        return maxLength;
    }
}
