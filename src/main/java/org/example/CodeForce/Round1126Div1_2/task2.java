package org.example.CodeForce.Round1126Div1_2;

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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
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
            int k = in.nextInt();
            int[] a = new int[n];
            int max = -1;
            for (int j = 0; j < n; j++) {
                a[j] = in.nextInt();
                if (a[j] > max) {
                    max = a[j];
                }
            }
            
            int[] b = new int[max+2];
            for (int j = 0; j < n; j++) {
                b[a[j]]++;
            }
            int x = 0;
            while (b[x] >= 2 * k) x++;
            if (b[x] == 2 * k - 1) out.println("YES");
            else out.println("NO");
            
        }
        out.close();
    }

    /**
     * Простые делители x без повторов, по возрастанию. O(sqrt(x)).
     * Пример: 12 -> [2, 3],  7 -> [7],  1 -> [].
     */
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


    /**
     * Все простые числа до max без повторов, по возрастанию. O(max / log(max)).
     */
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

        /**
         * Все целые положительные делители n, по возрастанию.
         * Для n &lt;= 0 — пустой список. O(sqrt(n)).
         */
        static ArrayList<Integer> divisors(int n) {
            ArrayList<Integer> result = new ArrayList<>();
            if (n <= 0) {
                return result;
            }
            for (int i = 1; (long) i * i <= n; i++) {
                if (n % i != 0) {
                    continue;
                }
                result.add(i);
                int pair = n / i;
                if (pair != i) {
                    result.add(pair);
                }
            }
            result.sort(null);
            return result;
        }

        /**
         * Все целые положительные делители n (long), по возрастанию.
         */
        static ArrayList<Long> divisors(long n) {
            ArrayList<Long> result = new ArrayList<>();
            if (n <= 0) {
                return result;
            }
            for (long i = 1; i <= n / i; i++) {
                if (n % i != 0) {
                    continue;
                }
                result.add(i);
                long pair = n / i;
                if (pair != i) {
                    result.add(pair);
                }
            }
            result.sort(null);
            return result;
        }
    }

    /**
     * Универсальный граф для CF: вершины 1..n, списки смежности O(n+m),
     * без чтения stdin и без рекурсии.
     *
     * Не выделяет n² ячеек и не вызывает DFS стеком JVM — обход идёт явным
     * ArrayDeque, так что n = 10⁵ не даёт ни OOM, ни StackOverflow.
     *
     * Компоненты, MST и циклы без ориентации опираются на {@link DisjointSetUnion}:
     * сжатие путей и объединение по размеру, O(α(n)) на запрос.
     */
    @SuppressWarnings("unchecked")
    static final class Graph {
        static final class Edge {
            final int from;
            final int to;
            final long weight;

            Edge(int from, int to) {
                this(from, to, 1L);
            }

            Edge(int from, int to, long weight) {
                this.from = from;
                this.to = to;
                this.weight = weight;
            }
        }

        /**
         * СНМ. Вершины 1..n (индекс 0 не используется).
         */
        static final class DisjointSetUnion {
            private final int[] parent;
            private final int[] size;
            private int componentCount;

            DisjointSetUnion(int vertexCount) {
                parent = new int[vertexCount + 1];
                size = new int[vertexCount + 1];
                reset();
            }

            void reset() {
                componentCount = parent.length - 1;
                for (int vertex = 0; vertex < parent.length; vertex++) {
                    parent[vertex] = vertex;
                    size[vertex] = 1;
                }
            }

            int find(int vertex) {
                while (parent[vertex] != vertex) {
                    parent[vertex] = parent[parent[vertex]];
                    vertex = parent[vertex];
                }
                return vertex;
            }

            /** true, если вершины были в разных компонентах и их объединили. */
            boolean union(int first, int second) {
                int firstRoot = find(first);
                int secondRoot = find(second);
                if (firstRoot == secondRoot) {
                    return false;
                }
                if (size[firstRoot] < size[secondRoot]) {
                    int swap = firstRoot;
                    firstRoot = secondRoot;
                    secondRoot = swap;
                }
                parent[secondRoot] = firstRoot;
                size[firstRoot] += size[secondRoot];
                componentCount--;
                return true;
            }

            boolean connected(int first, int second) {
                return find(first) == find(second);
            }

            int sizeOf(int vertex) {
                return size[find(vertex)];
            }

            int componentCount() {
                return componentCount;
            }
        }

        static final class Forest {
            final ArrayList<Edge> edges;
            final long totalWeight;
            final int treeEdgeCount;

            private Forest(ArrayList<Edge> edges, long totalWeight) {
                this.edges = edges;
                this.totalWeight = totalWeight;
                this.treeEdgeCount = edges.size();
            }

            boolean isSpanningTree(int vertexCount) {
                return treeEdgeCount == vertexCount - 1;
            }
        }

        private final int vertexCount;
        private final boolean directed;
        private final ArrayList<Edge>[] outgoing;
        private final ArrayList<Edge> edges;

        static Graph undirected(int vertexCount) {
            return new Graph(vertexCount, false);
        }

        static Graph directed(int vertexCount) {
            return new Graph(vertexCount, true);
        }

        private Graph(int vertexCount, boolean directed) {
            this.vertexCount = vertexCount;
            this.directed = directed;
            this.outgoing = new ArrayList[vertexCount + 1];
            for (int vertex = 1; vertex <= vertexCount; vertex++) {
                outgoing[vertex] = new ArrayList<>();
            }
            this.edges = new ArrayList<>();
        }

        int vertexCount() {
            return vertexCount;
        }

        boolean isDirected() {
            return directed;
        }

        ArrayList<Edge> edges() {
            return edges;
        }

        ArrayList<Edge> neighbors(int vertex) {
            return outgoing[vertex];
        }

        void addEdge(int from, int to) {
            addEdge(from, to, 1L);
        }

        void addEdge(int from, int to, long weight) {
            Edge edge = new Edge(from, to, weight);
            edges.add(edge);
            outgoing[from].add(edge);
            if (!directed) {
                outgoing[to].add(new Edge(to, from, weight));
            }
        }

        /**
         * Итеративный BFS. Расстояния в рёбрах, недостижимые вершины = -1.
         */
        long[] bfsDistances(int start) {
            long[] distance = new long[vertexCount + 1];
            Arrays.fill(distance, -1L);
            ArrayDeque<Integer> queue = new ArrayDeque<>();
            distance[start] = 0;
            queue.addLast(start);
            while (!queue.isEmpty()) {
                int vertex = queue.removeFirst();
                for (Edge edge : outgoing[vertex]) {
                    if (distance[edge.to] != -1L) {
                        continue;
                    }
                    distance[edge.to] = distance[vertex] + 1;
                    queue.addLast(edge.to);
                }
            }
            return distance;
        }

        /**
         * Итеративный DFS: порядок входа в вершины. Стек явный, не кадры JVM.
         */
        ArrayList<Integer> dfsOrder(int start) {
            boolean[] visited = new boolean[vertexCount + 1];
            ArrayList<Integer> order = new ArrayList<>();
            fillDfsFrom(start, visited, order);
            return order;
        }

        /**
         * Компоненты связности. Для ориентированного графа — слабые
         * (направление рёбер игнорируется), иначе обычные неориентированные.
         */
        ArrayList<ArrayList<Integer>> components() {
            boolean[] visited = new boolean[vertexCount + 1];
            ArrayList<Edge>[] walk = directed ? undirectedView() : outgoing;
            ArrayList<ArrayList<Integer>> result = new ArrayList<>();
            for (int start = 1; start <= vertexCount; start++) {
                if (visited[start]) {
                    continue;
                }
                ArrayList<Integer> component = new ArrayList<>();
                ArrayDeque<Integer> stack = new ArrayDeque<>();
                visited[start] = true;
                stack.addLast(start);
                while (!stack.isEmpty()) {
                    int vertex = stack.removeLast();
                    component.add(vertex);
                    for (Edge edge : walk[vertex]) {
                        if (visited[edge.to]) {
                            continue;
                        }
                        visited[edge.to] = true;
                        stack.addLast(edge.to);
                    }
                }
                result.add(component);
            }
            return result;
        }

        /**
         * Топологический порядок (Кан). null, если есть цикл.
         * Имеет смысл для ориентированного графа.
         */
        ArrayList<Integer> topologicalOrder() {
            int[] indegree = new int[vertexCount + 1];
            for (Edge edge : edges) {
                indegree[edge.to]++;
            }
            ArrayDeque<Integer> queue = new ArrayDeque<>();
            for (int vertex = 1; vertex <= vertexCount; vertex++) {
                if (indegree[vertex] == 0) {
                    queue.addLast(vertex);
                }
            }
            ArrayList<Integer> order = new ArrayList<>(vertexCount);
            while (!queue.isEmpty()) {
                int vertex = queue.removeFirst();
                order.add(vertex);
                for (Edge edge : outgoing[vertex]) {
                    indegree[edge.to]--;
                    if (indegree[edge.to] == 0) {
                        queue.addLast(edge.to);
                    }
                }
            }
            if (order.size() != vertexCount) {
                return null;
            }
            return order;
        }

        /**
         * Минимальный остовный лес Крускалом (по возрастанию веса).
         * Для связного неориентированного графа это MST.
         */
        Forest kruskal() {
            Edge[] sorted = edges.toArray(new Edge[0]);
            Arrays.sort(sorted, Comparator.comparingLong(edge -> edge.weight));
            DisjointSetUnion dsu = new DisjointSetUnion(vertexCount);
            ArrayList<Edge> forest = new ArrayList<>();
            long totalWeight = 0;
            for (Edge edge : sorted) {
                if (dsu.union(edge.from, edge.to)) {
                    forest.add(edge);
                    totalWeight += edge.weight;
                }
            }
            return new Forest(forest, totalWeight);
        }

        /** Компоненты через DSU без обхода списков смежности. */
        DisjointSetUnion disjointSetUnion() {
            DisjointSetUnion dsu = new DisjointSetUnion(vertexCount);
            for (Edge edge : edges) {
                dsu.union(edge.from, edge.to);
            }
            return dsu;
        }

        boolean hasUndirectedCycle() {
            DisjointSetUnion dsu = new DisjointSetUnion(vertexCount);
            for (Edge edge : edges) {
                if (!dsu.union(edge.from, edge.to)) {
                    return true;
                }
            }
            return false;
        }

        private void fillDfsFrom(int start, boolean[] visited, ArrayList<Integer> order) {
            ArrayDeque<Integer> stack = new ArrayDeque<>();
            visited[start] = true;
            stack.addLast(start);
            while (!stack.isEmpty()) {
                int vertex = stack.removeLast();
                order.add(vertex);
                ArrayList<Edge> neighbors = outgoing[vertex];
                for (int i = neighbors.size() - 1; i >= 0; i--) {
                    int next = neighbors.get(i).to;
                    if (visited[next]) {
                        continue;
                    }
                    visited[next] = true;
                    stack.addLast(next);
                }
            }
        }

        /** Неориентированный вид: каждое ребро в обе стороны, O(n+m). */
        private ArrayList<Edge>[] undirectedView() {
            ArrayList<Edge>[] both = new ArrayList[vertexCount + 1];
            for (int vertex = 1; vertex <= vertexCount; vertex++) {
                both[vertex] = new ArrayList<>();
            }
            for (Edge edge : edges) {
                both[edge.from].add(edge);
                both[edge.to].add(new Edge(edge.to, edge.from, edge.weight));
            }
            return both;
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
