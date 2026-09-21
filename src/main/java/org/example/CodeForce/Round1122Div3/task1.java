package org.example.CodeForce.Round1122Div3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

public class task1 {
    public static void main(String[] args) throws IOException{
        FastScanner in = new FastScanner();
        int t = in.nextInt();
        int[] a = new int[t];
        for (int i = 0; i < t; i++) {
            int n = in.nextInt();
            int a1 = in.nextInt();
            int a2 = in.nextInt();
            int a3 = in.nextInt();
            a[i] = n -Math.min(a1, Math.min(a2, a3));
        }

        for (int i = 0; i < t; i++) {
            System.out.println(a[i]);
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
