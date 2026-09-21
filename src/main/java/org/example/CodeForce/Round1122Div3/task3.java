package org.example.CodeForce.Round1122Div3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

public class task3 {
    public static void main(String[] args) throws IOException {
        FastScanner in = new FastScanner();
        int t = in.nextInt();
        StringBuilder out = new StringBuilder();

        for (int test = 0; test < t; test++) {
            int n = in.nextInt();
            String s = in.next();
            out.append(minOperations(s, n)).append('\n');
        }

        System.out.print(out);
    }

    private static int minOperations(String s, int n) {
        if (s.charAt(0) == '1') {
            int zeros = 0;
            for (int i = 0; i < n; i++) {
                if (s.charAt(i) == '0') {
                    zeros++;
                }
            }
            return zeros;
        }

        int firstOne = -1;
        int totalOnes = 0;
        for (int i = 0; i < n; i++) {
            if (s.charAt(i) == '1') {
                totalOnes++;
                if (firstOne < 0) {
                    firstOne = i;
                }
            }
        }

        if (firstOne < 0) {
            return 0;
        }

        int prefOnes = 0;
        int answer = (1<<31)-1;
        for (int k = 0; k <= n; k++) {
            if (k >= firstOne) {
                int cost = 2 * prefOnes + (n - k) - totalOnes;
                if (cost < answer) {
                    answer = cost;
                }
            }
            if (k < n && s.charAt(k) == '1') {
                prefOnes++;
            }
        }

        return answer;
    }

    static class FastScanner {
        private final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        private StringTokenizer st;

        String next() throws IOException {
            while (st == null || !st.hasMoreTokens()) {
                st = new StringTokenizer(br.readLine());
            }
            return st.nextToken();
        }

        int nextInt() throws IOException {
            return Integer.parseInt(next());
        }
    }
}
