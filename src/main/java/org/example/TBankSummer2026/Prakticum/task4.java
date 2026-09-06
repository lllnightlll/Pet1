package org.example.TBankSummer2026.Prakticum;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

public class task4 {
    static final int MOD = 1_000_000_007;

    public static void main(String[] args) throws IOException {
        FastScanner in = new FastScanner();
        int n = in.nextInt();

        Map<String, Integer> count = new HashMap<>();
        for (int i = 0; i < n; i++) {
            String name = in.next();
            in.next();
            count.merge(name, 1, Integer::sum);
        }

        long[] fact = new long[n + 1];
        long[] invFact = new long[n + 1];
        fact[0] = 1;
        for (int i = 1; i <= n; i++) {
            fact[i] = fact[i - 1] * i % MOD;
        }
        invFact[n] = modPow(fact[n], MOD - 2);
        for (int i = n; i >= 1; i--) {
            invFact[i - 1] = invFact[i] * i % MOD;
        }

        long ans = fact[n];
        for (int c : count.values()) {
            ans = ans * invFact[c] % MOD;
        }
        System.out.println(ans);
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
