package org.example.TBankSummer2026.Prakticum;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

public class task5 {
    static final int MOD = 1_000_000_007;
    static final int MAX = 2000;

    static int[][] euler;
    static long[] fact;
    static long[] invFact;

    public static void main(String[] args) throws IOException {
        precompute();
        FastScanner in = new FastScanner();
        int t = in.nextInt();
        StringBuilder out = new StringBuilder();
        while (t-- > 0) {
            int n = in.nextInt();
            int a = in.nextInt();
            int b = in.nextInt();
            out.append(solve(n, a, b)).append('\n');
        }
        System.out.print(out);
    }

    static int solve(int n, int a, int b) {
        int k = n - a - b;
        if (k < 0) {
            return 0;
        }
        int m = a + b;
        long der = derangementsWithExcedances(m, b);
        long ans = fact[n] * binom(n, k) % MOD * der % MOD;
        return (int) ans;
    }

    static long derangementsWithExcedances(int m, int b) {
        if (m == 0) {
            return b == 0 ? 1 : 0;
        }
        if (b < 0 || b >= m) {
            return 0;
        }
        long d = 0;
        for (int j = 0; j <= m; j++) {
            int rem = m - j;
            int waysA = rem == 0 ? (b == 0 ? 1 : 0) : (b < rem ? euler[rem][b] : 0);
            long ways = binom(m, j) * waysA % MOD;
            if ((j & 1) == 1) {
                d -= ways;
            } else {
                d += ways;
            }
        }
        d %= MOD;
        if (d < 0) {
            d += MOD;
        }
        return d;
    }

    static void precompute() {
        euler = new int[MAX + 1][MAX + 1];
        euler[0][0] = 1;
        for (int n = 1; n <= MAX; n++) {
            euler[n][0] = 1;
            for (int k = 1; k < n; k++) {
                euler[n][k] = (int) (((long) (k + 1) * euler[n - 1][k]
                        + (long) (n - k) * euler[n - 1][k - 1]) % MOD);
            }
        }

        fact = new long[MAX + 1];
        invFact = new long[MAX + 1];
        fact[0] = 1;
        for (int i = 1; i <= MAX; i++) {
            fact[i] = fact[i - 1] * i % MOD;
        }
        invFact[MAX] = modPow(fact[MAX], MOD - 2);
        for (int i = MAX; i >= 1; i--) {
            invFact[i - 1] = invFact[i] * i % MOD;
        }
    }

    static long binom(int n, int k) {
        if (k < 0 || k > n) {
            return 0;
        }
        return fact[n] * invFact[k] % MOD * invFact[n - k] % MOD;
    }

    static long modPow(long a, long e) {
        long r = 1;
        while (e > 0) {
            if ((e & 1) == 1) {
                r = r * a % MOD;
            }
            a = a * a % MOD;
            e >>= 1;
        }
        return r;
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
