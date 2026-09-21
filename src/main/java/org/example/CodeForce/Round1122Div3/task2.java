package org.example.CodeForce.Round1122Div3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

public class task2 {
    public static void main(String[] args) throws IOException {
        FastScanner in = new FastScanner();
        int t = in.nextInt();
        long[] k = new long[t];
        for (int i = 0; i < t; i++) {
            long a = in.nextLong();
            long b = in.nextLong();
            long c = in.nextLong();
            k[i] = Math.max(Math.abs(a - b), Math.abs(a + c - b));
        }

        for (int i = 0; i < t; i++) {
            System.out.println(k[i]);
        }
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
