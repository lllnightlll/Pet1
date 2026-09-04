package org.example.TBankSummer2026;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

public class task3 {
    public static void main(String[] args) throws IOException {
        FastScanner in = new FastScanner();
        int n = in.nextInt();
        int t = in.nextInt();

        int[] row = new int[n + 1];
        int[] col = new int[n + 1];
        int diag = 0;
        int anti = 0;

        for (int i = 1; i <= t; i++) {
            int a = in.nextInt();
            int x = (a - 1) / n + 1;
            int y = (a - 1) % n + 1;

            row[x]++;
            col[y]++;
            if (x == y) {
                diag++;
            }
            if (x + y == n + 1) {
                anti++;
            }

            if (row[x] == n || col[y] == n || diag == n || anti == n) {
                System.out.println(i);
                return;
            }
        }

        System.out.println(-1);
    }

    static class FastScanner {
        private final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        private StringTokenizer st;

        int nextInt() throws IOException {
            while (st == null || !st.hasMoreTokens()) {
                st = new StringTokenizer(br.readLine());
            }
            return Integer.parseInt(st.nextToken());
        }
    }
}
