package org.example.CodeForce.Round1122Div3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.StringTokenizer;

public class task6 {
    public static void main(String[] args) throws IOException {
        FastScanner in = new FastScanner();
        int t = in.nextInt();
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < t; i++) {
            HashMap<Long, Long> set = new HashMap<>();
            int n = in.nextInt();
            long max = 0, total = 0;
            for (int j = 0; j < n; j++) {
                long x = in.nextLong(), y = in.nextLong();
                set.put(x, y);
                max = Math.max(max, x);
                total += y;
            }

            long mexoramax = max;
            for (long m = max + 1; m <= max + 60; m++) {
                if (!can(m, set, total)) break;
                mexoramax = m;
            }
            out.append(mexoramax).append('\n');
        }
        System.out.print(out);
    }

    static boolean can(long m, HashMap<Long, Long> set, long total) {
        long need = 1, extra = 0, prev = m;
        ArrayList<Long> keys = new ArrayList<>();
        for (var e : set.entrySet()) {
            if (e.getKey() >= m) extra += e.getValue();
            else if (e.getKey() > 0) keys.add(e.getKey());
        }
        keys.sort(Collections.reverseOrder());
        for (long x : keys) {
            need = mul2(need, prev - x - 1, total);
            if (need < 0) return false;
            long have = set.get(x);
            if (have >= need) extra += have - need;
            else if ((need = need * 2 - have) > total + 1) return false;
            prev = x;
        }
        need = mul2(need, prev - 1, total);
        return need >= 0 && set.getOrDefault(0L, 0L) + extra >= need;
    }

    static long mul2(long need, long gap, long total) {
        if (gap >= 63) return -1;
        while (gap-- > 0) {
            if (need > total + 1 || need > Long.MAX_VALUE / 2) return -1;
            need *= 2;
        }
        return need;
    }

    static class FastScanner {
        private final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        private StringTokenizer st;

        String next() throws IOException {
            while (st == null || !st.hasMoreTokens()) st = new StringTokenizer(br.readLine());
            return st.nextToken();
        }

        int nextInt() throws IOException { return Integer.parseInt(next()); }
        long nextLong() throws IOException { return Long.parseLong(next()); }
    }
}
