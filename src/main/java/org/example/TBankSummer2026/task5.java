package org.example.TBankSummer2026;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.StringTokenizer;

public class task5 {
    public static void main(String[] args) throws IOException {
        FastScanner in = new FastScanner();
        int n = in.nextInt();
        long k = in.nextLong();
        long[] a = new long[n];
        for (int i = 0; i < n; i++) {
            a[i] = in.nextLong();
        }
        Arrays.sort(a);

        int nNeg = 0;
        int nZero = 0;
        int nPos = 0;
        for (long v : a) {
            if (v < 0) {
                nNeg++;
            } else if (v == 0) {
                nZero++;
            } else {
                nPos++;
            }
        }

        long[] neg = new long[nNeg];
        long[] pos = new long[nPos];
        long[] negAbs = new long[nNeg];
        int ineg = 0;
        int ipos = 0;
        for (long v : a) {
            if (v < 0) {
                neg[ineg++] = v;
            } else if (v > 0) {
                pos[ipos++] = v;
            }
        }
        for (int i = 0; i < nNeg; i++) {
            negAbs[i] = -neg[nNeg - 1 - i];
        }

        long zeroPairs = (long) nZero * (n - nZero) + (long) nZero * (nZero - 1) / 2;

        long lo = a[0] * a[1];
        lo = Math.min(lo, a[0] * a[n - 1]);
        lo = Math.min(lo, a[n - 2] * a[n - 1]);
        long hi = a[0] * a[1];
        hi = Math.max(hi, a[0] * a[n - 1]);
        hi = Math.max(hi, a[n - 2] * a[n - 1]);

        while (lo < hi) {
            long mid = lo + (hi - lo) / 2;
            if (count(neg, pos, negAbs, zeroPairs, mid) >= k) {
                hi = mid;
            } else {
                lo = mid + 1;
            }
        }
        System.out.println(lo);
    }

    static long count(long[] neg, long[] pos, long[] negAbs, long zeroPairs, long x) {
        long cnt = countOpposite(neg, pos, x);
        if (x >= 0) {
            cnt += zeroPairs;
            cnt += countSameSign(pos, x);
            cnt += countSameSign(negAbs, x);
        }
        return cnt;
    }

    static long countOpposite(long[] neg, long[] pos, long x) {
        int nNeg = neg.length;
        int nPos = pos.length;
        if (nNeg == 0 || nPos == 0) {
            return 0;
        }
        if (x >= 0) {
            return (long) nNeg * nPos;
        }
        long cnt = 0;
        int j = 0;
        for (int i = 0; i < nNeg; i++) {
            while (j < nPos && neg[i] * pos[j] > x) {
                j++;
            }
            cnt += nPos - j;
        }
        return cnt;
    }

    static long countSameSign(long[] b, long x) {
        int m = b.length;
        long cnt = 0;
        int r = m - 1;
        for (int l = 0; l < m; l++) {
            while (r >= 0 && b[l] * b[r] > x) {
                r--;
            }
            if (r > l) {
                cnt += r - l;
            }
        }
        return cnt;
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
