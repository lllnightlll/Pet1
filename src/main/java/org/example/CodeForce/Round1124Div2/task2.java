package org.example.CodeForce.Round1124Div2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

/*
The sause flexing
No ketchup
None
Just sauce
Saucy
raw sauce
Bah
Yo
Boom
Ah
The thing goes...
*/

@SuppressWarnings("unused")
public class task2 {
    public static void main(String[] args) throws IOException {
        FastScanner in = FastScanner.fromStdIn();
        FastWriter out = FastWriter.fromStdOut();

        // FastScanner in = FastScanner.fromFile("input.txt");
        // FastWriter out = FastWriter.fromFile("output.txt");
        // TreeMap<Key, Value> map = new TreeMap<>();
        // TreeSet<Value> values = new TreeSet<>();

        int t = in.nextInt();
        for (int i = 0; i < t; i++) {
            int n = in.nextInt();
            ArrayList<Integer> a = new ArrayList<>();
            for (int j = 0; j < n; j++) {
                a.add(in.nextInt());
            }
            ArrayList<Integer> b = new ArrayList<>();
            for (int j = 0; j < n; j++) {
                b.add(ref(a.get(j)));
            }
            int sum = 0;
            for (int j = n - 1; j >= 0; j--) {
                for (int k = j - 1; k >= 0; k--) {
                    if (b.get(j).equals(b.get(k))) {
                        sum++;
                    }
                }
            }
            out.println(sum);
        }
        out.close();
    }

    static final int[] UNHAPPY_CYCLE = {4, 16, 37, 58, 89, 145, 42, 20};

    static int ref(int x) {
        int a = x;
        if (a == 1) {
            return 1;
        }
        if (a == 4) {
            return 4;
        }
        if (a == 16) {
            return 16;
        }
        if (a == 37) {
            return 37;
        }
        if (a == 58) {
            return 58;
        }
        if (a == 89) {
            return 89;
        }
        if (a == 145) {
            return 145;
        }
        if (a == 42) {
            return 42;
        }
        String s = Integer.toString(a);
        if (s.charAt(0) == '2') {
            boolean onlyZeros = true;
            for (int i = 1; i < s.length(); i++) {
                if (s.charAt(i) != '0') {
                    onlyZeros = false;
                    break;
                }
            }
            if (onlyZeros) {
                return 20;
            }
        }

        int tail = 0;
        ArrayList<Integer> digits = new ArrayList<>();
        while (true) {
            int sumSq = 0;
            for (char c : Integer.toString(a).toCharArray()) {
                int digit = Character.getNumericValue(c);
                sumSq += digit * digit;
            }
            a = sumSq;
            tail++;
            if (a == 1) {
                return 1;
            }
            int pos = cyclePos(a);
            if (pos >= 0) {
                int len = UNHAPPY_CYCLE.length;
                int index = (pos - tail) % len;
                if (index < 0) {
                    index += len;
                }
                return UNHAPPY_CYCLE[index];
            }
            if (digits.contains(a)) {
                break;
            }
            digits.add(a);
        }
        return a;
    }

    static int cyclePos(int value) {
        for (int i = 0; i < UNHAPPY_CYCLE.length; i++) {
            if (UNHAPPY_CYCLE[i] == value) {
                return i;
            }
        }
        return -1;
    }

    @SuppressWarnings("unused")
    private static ArrayList<Integer> uniquePrimeFactors(int x) {
        ArrayList<Integer> primes = new ArrayList<>();
        for (int p = 2; (long) p * p <= x; p++) {
            if (x % p != 0) {
                continue;
            }
            primes.add(p);
            while (x % p == 0) {
                x /= p;
            }
        }
        if (x > 1) {
            primes.add(x);
        }
        return primes;
    }

    @SuppressWarnings("unused")
    private static ArrayList<Integer> buildPrimes(int max) {
        ArrayList<Integer> primes = new ArrayList<>();
        for (int j = 2; j <= max; j++) {
            if (Prime.isPrime(j)) {
                primes.add(j);
            }
        }
        return primes;
    }

    static final class Prime {
        private static final long[] BASES = { 2L, 325L, 9375L, 28178L, 450775L, 9780504L, 1795265022L };

        private Prime() {
        }

        static boolean isPrime(long n) {
            if (n < 2) {
                return false;
            }
            if (n <= 3) {
                return true;
            }
            if ((n & 1) == 0 || n % 3 == 0) {
                return false;
            }
            long d = n - 1;
            int s = 0;
            while ((d & 1) == 0) {
                d >>= 1;
                s++;
            }
            for (long base : BASES) {
                if (base % n == 0) {
                    continue;
                }
                if (!millerRabinWitness(base % n, d, s, n)) {
                    return false;
                }
            }
            return true;
        }

        private static boolean millerRabinWitness(long a, long d, int s, long n) {
            long x = powMod(a, d, n);
            if (x == 1 || x == n - 1) {
                return true;
            }
            for (int i = 1; i < s; i++) {
                x = mulMod(x, x, n);
                if (x == n - 1) {
                    return true;
                }
            }
            return false;
        }

        static long powMod(long a, long e, long mod) {
            long result = 1 % mod;
            a %= mod;
            while (e > 0) {
                if ((e & 1) == 1) {
                    result = mulMod(result, a, mod);
                }
                a = mulMod(a, a, mod);
                e >>= 1;
            }
            return result;
        }

        static long mulMod(long a, long b, long mod) {
            return java.math.BigInteger.valueOf(a).multiply(java.math.BigInteger.valueOf(b))
                    .mod(java.math.BigInteger.valueOf(mod)).longValue();
        }
    }

    static final class Const {
        private Const() {
        }

        /** 10^9+7 */
        static final int MOD = 1_000_000_007;

        /** 998244353 */
        static final int MOD2 = 998_244_353;

        static final int INF = 1_000_000_000;
        static final long LINF = 4_000_000_000_000_000_000L;
        static final double EPS = 1e-9;
        static final int ALPHABET = 26;
        static final int DIGITS = 10;

        static final int[] DX4 = { -1, 0, 1, 0 };
        static final int[] DY4 = { 0, 1, 0, -1 };
        static final int[] DX8 = { -1, -1, -1, 0, 0, 1, 1, 1 };
        static final int[] DY8 = { -1, 0, 1, -1, 1, -1, 0, 1 };

        static int addMod(int a, int b) {
            int sum = a + b;
            return sum >= MOD ? sum - MOD : sum;
        }

        static int mulMod(long a, long b) {
            return (int) ((a * b) % MOD);
        }

        static long gcd(long a, long x) {
            a = Math.abs(a);
            x = Math.abs(x);
            while (x != 0) {
                long next = a % x;
                a = x;
                x = next;
            }
            return a;
        }

        static int gcd(int a, int x) {
            return (int) gcd((long) a, (long) x);
        }

        static long lcm(long a, long x) {
            if (a == 0 || x == 0) {
                return 0;
            }
            return Math.abs(a / gcd(a, x) * x);
        }
    }

    static final class BinarySearch {
        private BinarySearch() {
        }

        @FunctionalInterface
        interface Check {
            boolean ok(long mid);
        }

        static int lowerBound(int[] a, int target) {
            int left = 0;
            int right = a.length;
            while (left < right) {
                int mid = left + (right - left) / 2;
                if (a[mid] >= target) {
                    right = mid;
                } else {
                    left = mid + 1;
                }
            }
            return left;
        }

        static int upperBound(int[] a, int target) {
            int left = 0;
            int right = a.length;
            while (left < right) {
                int mid = left + (right - left) / 2;
                if (a[mid] > target) {
                    right = mid;
                } else {
                    left = mid + 1;
                }
            }
            return left;
        }

        static long minTrue(long left, long right, Check check) {
            long answer = right + 1;
            while (left <= right) {
                long mid = left + (right - left) / 2;
                if (check.ok(mid)) {
                    answer = mid;
                    right = mid - 1;
                } else {
                    left = mid + 1;
                }
            }
            return answer;
        }

        static long maxTrue(long left, long right, Check check) {
            long answer = left - 1;
            while (left <= right) {
                long mid = left + (right - left) / 2;
                if (check.ok(mid)) {
                    answer = mid;
                    left = mid + 1;
                } else {
                    right = mid - 1;
                }
            }
            return answer;
        }

        static double minTrueDouble(double left, double right, int iterations, CheckDouble check) {
            for (int i = 0; i < iterations; i++) {
                double mid = (left + right) / 2.0;
                if (check.ok(mid)) {
                    right = mid;
                } else {
                    left = mid;
                }
            }
            return right;
        }

        @FunctionalInterface
        interface CheckDouble {
            boolean ok(double mid);
        }
    }

    static class FastScanner {
        private final BufferedReader reader;
        private StringTokenizer tokenizer;

        FastScanner(InputStream input) {
            this.reader = new BufferedReader(new InputStreamReader(input));
            this.tokenizer = null;
        }

        static FastScanner fromStdIn() {
            return new FastScanner(System.in);
        }

        static FastScanner fromFile(String path) throws IOException {
            return new FastScanner(new FileInputStream(path));
        }

        String next() throws IOException {
            while (tokenizer == null || !tokenizer.hasMoreTokens()) {
                String line = reader.readLine();
                if (line == null) {
                    return null;
                }
                tokenizer = new StringTokenizer(line);
            }
            return tokenizer.nextToken();
        }

        int nextInt() throws IOException {
            return Integer.parseInt(next());
        }

        long nextLong() throws IOException {
            return Long.parseLong(next());
        }

        double nextDouble() throws IOException {
            return Double.parseDouble(next());
        }

        String nextLine() throws IOException {
            tokenizer = null;
            return reader.readLine();
        }

        void close() throws IOException {
            reader.close();
        }
    }

    static class FastWriter {
        private final PrintWriter writer;

        FastWriter(OutputStream output) {
            this.writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(output)));
        }

        static FastWriter fromStdOut() {
            return new FastWriter(System.out);
        }

        static FastWriter fromFile(String path) throws IOException {
            return new FastWriter(new FileOutputStream(path));
        }

        void print(Object value) {
            writer.print(value);
        }

        void println(Object value) {
            writer.println(value);
        }

        void println() {
            writer.println();
        }

        void flush() {
            writer.flush();
        }

        void close() {
            writer.flush();
            writer.close();
        }
    }
}
