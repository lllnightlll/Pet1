package org.example.TBankSummer2026.Prakticum;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class task3 {
    static final int MOD = 1_000_000_007;

    public static void main(String[] args) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String s = in.readLine().trim();
        int n = s.length();

        long[] pref = new long[n + 2];
        pref[1] = 1;

        int[] cnt = new int[26];
        int left = 0;
        long dp = 1;

        for (int i = 1; i <= n; i++) {
            int c = s.charAt(i - 1) - 'a';
            cnt[c]++;
            while (cnt[c] > 1) {
                cnt[s.charAt(left) - 'a']--;
                left++;
            }
            dp = pref[i] - pref[left];
            if (dp < 0) {
                dp += MOD;
            }
            pref[i + 1] = pref[i] + dp;
            if (pref[i + 1] >= MOD) {
                pref[i + 1] -= MOD;
            }
        }

        System.out.println(dp);
    }
}
