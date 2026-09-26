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
import java.util.StringTokenizer;
import java.util.Comparator;
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
public class task3 {
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
            int k = in.nextInt();
            ArrayList<Integer> a = new ArrayList<>();
            for (int j = 0; j < n; j++) {
                a.add(in.nextInt());
            }
            int sum = 0;
            ArrayList<Integer> b = new ArrayList<>();
            b.add(a.get(0));
            for (int j = 1; j < n; j++) {
                b.add(a.get(j) + a.get(j - 1));
            }
            for (int j = 0; a.size() >= k; j++) {
                int left = k - 1;
                int right = a.size() - k;
                if (b.get(left) > b.get(right)) {
                    sum += a.get(left);
                    a.remove(left);
                } else {
                    sum += a.get(right);
                    a.remove(right);
                }
                b.clear();
                if (!a.isEmpty()) {
                    b.add(a.get(0));
                    for (int p = 1; p < a.size(); p++) {
                        b.add(a.get(p) + a.get(p - 1));
                    }
                }
            }
            out.println(sum);
        }
        out.close();
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

        /** Классический простой модуль (10^9+7). */
        static final int MOD = 1_000_000_007;

        /** Альтернативный простой модуль (998244353), часто в комбинаторике. */
        static final int MOD2 = 998_244_353;

        /** «Бесконечность» для int: с запасом под сложение двух INF. */
        static final int INF = 1_000_000_000;

        /** «Бесконечность» для long (пути, суммы, DP). */
        static final long LINF = 4_000_000_000_000_000_000L;

        /** Погрешность сравнения double. */
        static final double EPS = 1e-9;

        /** Размер английского алфавита / цифр. */
        static final int ALPHABET = 26;
        static final int DIGITS = 10;

        /** Смещения по 4 направлениям: вверх, вправо, вниз, влево. */
        static final int[] DX4 = { -1, 0, 1, 0 };
        static final int[] DY4 = { 0, 1, 0, -1 };

        /** Смещения по 8 направлениям (включая диагонали). */
        static final int[] DX8 = { -1, -1, -1, 0, 0, 1, 1, 1 };
        static final int[] DY8 = { -1, 0, 1, -1, 1, -1, 0, 1 };

        /**
         * Безопасное сложение по модулю MOD (оба аргумента в [0, MOD)).
         */
        static int addMod(int a, int b) {
            int sum = a + b;
            return sum >= MOD ? sum - MOD : sum;
        }

        /**
         * Безопасное умножение по модулю MOD.
         */
        static int mulMod(long a, long b) {
            return (int) ((a * b) % MOD);
        }

        /**
         * Наибольший общий делитель (алгоритм Евклида). Работает и для отрицательных:
         * берём модуль.
         */
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

        /**
         * Наименьшее общее кратное. Деление идёт до умножения, чтобы реже переполнять
         * long.
         */
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

        /**
         * Предикат «можно ли взять значение mid». Должен быть монотонным: false…false,
         * true…true (или наоборот — см. методы).
         */
        @FunctionalInterface
        interface Check {
            boolean ok(long mid);
        }

        /**
         * Первый индекс i в [0, n), где a[i] >= target (массив отсортирован по
         * возрастанию). Если все меньше target — возвращает n.
         */
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

        /**
         * Первый индекс i в [0, n), где a[i] > target (массив отсортирован по
         * возрастанию). Если все <= target — возвращает n.
         */
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

        /**
         * Наименьшее mid в [left, right], для которого check.ok(mid) == true.
         * Предполагается: при малых mid — false, при больших — true. Если true нигде
         * нет — возвращает right + 1.
         */
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

        /**
         * Наибольшее mid в [left, right], для которого check.ok(mid) == true.
         * Предполагается: при малых mid — true, при больших — false. Если true нигде
         * нет — возвращает left - 1.
         */
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

        /**
         * Бинарный поиск по вещественному отрезку (фиксированное число итераций). Ищем
         * наименьший mid, где check.ok(mid) == true.
         */
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
