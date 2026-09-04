package org.example.TBankSummer2026;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

public class task4 {
    public static void main(String[] args) throws IOException {
        FastScanner in = new FastScanner();
        int n = in.nextInt();
        long k = in.nextLong();

        int[] p = new int[n];
        int pos = 0;
        int m = n;

        while (m > 0) {
            if (k >= m - 1) {
                p[pos++] = m;
                k -= m - 1;
                m--;
            } else {
                int need = (int) k;
                for (int i = 1; i <= m - need - 1; i++) {
                    p[pos++] = i;
                }
                p[pos++] = m;
                for (int i = m - need; i < m; i++) {
                    p[pos++] = i;
                }
                break;
            }
        }

        StringBuilder out = new StringBuilder(n * 8);
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                out.append(' ');
            }
            out.append(p[i]);
        }
        System.out.println(out);
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

        long nextLong() throws IOException {
            return Long.parseLong(next());
        }
    }
}
