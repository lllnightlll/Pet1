package org.example.TBankSummer2026.Prakticum;

import java.util.Scanner;

public class task1 {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        int t = in.nextInt();
        StringBuilder out = new StringBuilder();
        for (int test = 0; test < t; test++) {
            String s = in.next();
            int a = 0;
            for (int i = 0; i < s.length(); i++) {
                if (s.charAt(i) == 'A') {
                    a++;
                }
            }
            int swaps = 0;
            for (int i = 0; i < a; i++) {
                if (s.charAt(i) == 'B') {
                    swaps++;
                }
            }
            out.append(swaps).append('\n');
        }
        System.out.print(out);
    }
}
