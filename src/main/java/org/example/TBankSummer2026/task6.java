package org.example.TBankSummer2026;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

public class task6 {
    public static void main(String[] args) throws IOException {
        FastScanner in = new FastScanner();
        int n = in.nextInt();
        int m = 1 << n;
        int[][] c = new int[m][n + 1];
        for (int i = 0; i < m; i++) {
            for (int j = 1; j <= n; j++) {
                c[i][j] = in.nextInt();
            }
        }

        long[] dp = new long[m];
        long[] next = new long[m];

        for (int k = 1; k <= n; k++) {
            int size = 1 << k;
            int half = size >> 1;
            for (int start = 0; start < m; start += size) {
                int mid = start + half;
                int end = start + size;
                long bestLeft = Long.MIN_VALUE;
                long bestRight = Long.MIN_VALUE;
                for (int i = start; i < mid; i++) {
                    long val = dp[i];
                    if (k > 1) {
                        val += c[i][k - 1];
                    }
                    if (val > bestLeft) {
                        bestLeft = val;
                    }
                }
                for (int i = mid; i < end; i++) {
                    long val = dp[i];
                    if (k > 1) {
                        val += c[i][k - 1];
                    }
                    if (val > bestRight) {
                        bestRight = val;
                    }
                }
                for (int i = start; i < mid; i++) {
                    next[i] = dp[i] + bestRight;
                }
                for (int i = mid; i < end; i++) {
                    next[i] = dp[i] + bestLeft;
                }
            }
            long[] tmp = dp;
            dp = next;
            next = tmp;
        }

        long ans = 0;
        for (int i = 0; i < m; i++) {
            ans = Math.max(ans, dp[i] + c[i][n]);
        }
        System.out.println(ans);
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
