package org.example.hackatones.TechArena2026;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Эвристический солвер TechArena 2026: открытие сервисных офисов,
 * назначение объектов и специалистов.
 *
 * Целевая функция (максимизация):
 * reward − distance − cost − alpha * (maxTasks − minTasks)
 *
 * Ограничения повторяют validate_solution.py: индексы, ёмкость, баланс числа
 * объектов, покрытие навыков, непересекающиеся выпуклые оболочки (офис + объекты).
 */
public class Main {

    /** Запас по времени относительно лимита NSUTS (300 с). */
    private static final long DEFAULT_TIME_LIMIT_MS = 280_000L;

    public static void main(String[] args) throws Exception {
        Path input = Path.of(args.length > 0 ? args[0] : "instance.json");
        Path output = Path.of(args.length > 1 ? args[1] : "solution.json");
        long timeLimitMs = args.length > 2 ? Long.parseLong(args[2]) : DEFAULT_TIME_LIMIT_MS;

        String text = Files.readString(input, StandardCharsets.UTF_8);
        if (text.startsWith("\uFEFF")) {
            text = text.substring(1);
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> raw = (Map<String, Object>) Json.parse(text);
        Instance instance = Instance.fromJson(raw);

        Solver solver = new Solver(instance, System.nanoTime() + timeLimitMs * 1_000_000L);
        Solution solution = solver.solve();
        Files.writeString(output, solution.toJson(), StandardCharsets.UTF_8);
    }

    /* ============================ JSON ============================ */

    /** Минимальный JSON-парсер (объекты, массивы, строки, числа). */
    static final class Json {
        static Object parse(String text) {
            return new Parser(text).parseValue();
        }

        static final class Parser {
            final String s;
            int i;

            Parser(String s) {
                this.s = s;
            }

            void skipWs() {
                while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
                    i++;
                }
            }

            char peek() {
                skipWs();
                return i < s.length() ? s.charAt(i) : 0;
            }

            Object parseValue() {
                char c = peek();
                if (c == '{') {
                    return parseObject();
                }
                if (c == '[') {
                    return parseArray();
                }
                if (c == '"') {
                    return parseString();
                }
                if (c == '-' || Character.isDigit(c)) {
                    return parseNumber();
                }
                throw new RuntimeException("bad json at " + i);
            }

            String parseString() {
                expect('"');
                StringBuilder out = new StringBuilder();
                while (i < s.length()) {
                    char c = s.charAt(i++);
                    if (c == '"') {
                        return out.toString();
                    }
                    if (c == '\\') {
                        out.append(s.charAt(i++));
                    } else {
                        out.append(c);
                    }
                }
                throw new RuntimeException("unterminated string");
            }

            Number parseNumber() {
                int start = i;
                if (s.charAt(i) == '-') {
                    i++;
                }
                while (i < s.length()) {
                    char c = s.charAt(i);
                    if (Character.isDigit(c) || c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') {
                        i++;
                    } else {
                        break;
                    }
                }
                String token = s.substring(start, i);
                if (token.indexOf('.') >= 0 || token.indexOf('e') >= 0 || token.indexOf('E') >= 0) {
                    return Double.parseDouble(token);
                }
                long v = Long.parseLong(token);
                if (v >= Integer.MIN_VALUE && v <= Integer.MAX_VALUE) {
                    return (int) v;
                }
                return v;
            }

            Map<String, Object> parseObject() {
                expect('{');
                Map<String, Object> map = new LinkedHashMap<>();
                skipWs();
                if (peek() == '}') {
                    i++;
                    return map;
                }
                while (true) {
                    String key = parseString();
                    skipWs();
                    expect(':');
                    map.put(key, parseValue());
                    skipWs();
                    char c = s.charAt(i++);
                    if (c == '}') {
                        return map;
                    }
                    if (c != ',') {
                        throw new RuntimeException("expected , or }");
                    }
                }
            }

            List<Object> parseArray() {
                expect('[');
                List<Object> list = new ArrayList<>();
                skipWs();
                if (peek() == ']') {
                    i++;
                    return list;
                }
                while (true) {
                    list.add(parseValue());
                    skipWs();
                    char c = s.charAt(i++);
                    if (c == ']') {
                        return list;
                    }
                    if (c != ',') {
                        throw new RuntimeException("expected , or ]");
                    }
                }
            }

            void expect(char ch) {
                skipWs();
                if (i >= s.length() || s.charAt(i) != ch) {
                    throw new RuntimeException("expected " + ch);
                }
                i++;
            }
        }
    }

    /* ============================ МОДЕЛЬ ============================ */

    static final class Vec {
        final double x;
        final double y;

        Vec(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    static final class Site {
        final Vec pos;
        final double reward;
        /** Типы задач (навыки), которые нужны на объекте. */
        final int[] tasks;
        final BitSet skillSet;

        Site(Vec pos, double reward, int[] tasks) {
            this.pos = pos;
            this.reward = reward;
            this.tasks = tasks;
            this.skillSet = new BitSet();
            for (int t : tasks) {
                this.skillSet.set(t);
            }
        }
    }

    static final class Office {
        final Vec pos;
        final double cost;
        final int maxSize;

        Office(Vec pos, double cost, int maxSize) {
            this.pos = pos;
            this.cost = cost;
            this.maxSize = maxSize;
        }
    }

    static final class Instance {
        final Site[] sites;
        final Office[] offices;
        final int[][] workers;
        final BitSet[] workerSkills;
        final int[] workerPrimarySkill;
        /** Сколько работников владеют навыком skill. */
        final int[] skillSupply;
        final double[][] distances;
        final int maxDifference;
        final double balanceCoefficient;
        final int skillBound;
        /** Навыки, которые вообще нельзя покрыть. */
        final BitSet coveredGlobally;

        Instance(Site[] sites, Office[] offices, int[][] workers, double[][] distances,
                 int maxDifference, double balanceCoefficient) {
            this.sites = sites;
            this.offices = offices;
            this.workers = workers;
            this.distances = distances;
            this.maxDifference = maxDifference;
            this.balanceCoefficient = balanceCoefficient;

            int maxSkill = 0;
            this.workerSkills = new BitSet[workers.length];
            this.workerPrimarySkill = new int[workers.length];
            for (int i = 0; i < workers.length; i++) {
                workerSkills[i] = new BitSet();
                workerPrimarySkill[i] = workers[i].length == 0 ? -1 : workers[i][0];
                for (int s : workers[i]) {
                    workerSkills[i].set(s);
                    maxSkill = Math.max(maxSkill, s);
                }
            }
            for (Site site : sites) {
                for (int t : site.tasks) {
                    maxSkill = Math.max(maxSkill, t);
                }
            }
            this.skillBound = maxSkill + 1;
            this.skillSupply = new int[skillBound];
            this.coveredGlobally = new BitSet();
            for (BitSet skills : workerSkills) {
                for (int s = skills.nextSetBit(0); s >= 0; s = skills.nextSetBit(s + 1)) {
                    if (s < skillBound) {
                        skillSupply[s]++;
                        coveredGlobally.set(s);
                    }
                }
            }
        }

        @SuppressWarnings("unchecked")
        static Instance fromJson(Map<String, Object> raw) {
            List<Object> sitesRaw = (List<Object>) raw.get("sites");
            List<Object> officesRaw = (List<Object>) raw.get("offices");
            List<Object> workersRaw = (List<Object>) raw.get("workers");
            List<Object> distRaw = (List<Object>) raw.get("distances");

            Site[] sites = new Site[sitesRaw.size()];
            for (int i = 0; i < sitesRaw.size(); i++) {
                Map<String, Object> s = (Map<String, Object>) sitesRaw.get(i);
                sites[i] = new Site(
                        new Vec(num(s.get("x")), num(s.get("y"))),
                        num(s.get("reward")),
                        intArray(s.get("tasks")));
            }
            Office[] offices = new Office[officesRaw.size()];
            for (int i = 0; i < officesRaw.size(); i++) {
                Map<String, Object> o = (Map<String, Object>) officesRaw.get(i);
                offices[i] = new Office(
                        new Vec(num(o.get("x")), num(o.get("y"))),
                        num(o.get("cost")),
                        (int) num(o.get("maxSize")));
            }
            int[][] workers = new int[workersRaw.size()][];
            for (int i = 0; i < workersRaw.size(); i++) {
                workers[i] = intArray(workersRaw.get(i));
            }
            double[][] distances = new double[distRaw.size()][];
            for (int i = 0; i < distRaw.size(); i++) {
                List<Object> row = (List<Object>) distRaw.get(i);
                distances[i] = new double[row.size()];
                for (int j = 0; j < row.size(); j++) {
                    distances[i][j] = num(row.get(j));
                }
            }
            return new Instance(sites, offices, workers, distances,
                    (int) num(raw.get("maxDifference")),
                    num(raw.get("balanceCoefficient")));
        }

        static double num(Object v) {
            return ((Number) v).doubleValue();
        }

        @SuppressWarnings("unchecked")
        static int[] intArray(Object value) {
            if (!(value instanceof List<?> list)) {
                return new int[0];
            }
            int[] out = new int[list.size()];
            for (int i = 0; i < list.size(); i++) {
                out[i] = ((Number) list.get(i)).intValue();
            }
            return out;
        }

        boolean siteFeasible(int siteId) {
            BitSet need = sites[siteId].skillSet;
            for (int s = need.nextSetBit(0); s >= 0; s = need.nextSetBit(s + 1)) {
                if (!coveredGlobally.get(s)) {
                    return false;
                }
            }
            return true;
        }
    }

    static final class OpenedOffice {
        final int officeId;
        final List<Integer> sites;
        final List<Integer> workers;

        OpenedOffice(int officeId, List<Integer> sites, List<Integer> workers) {
            this.officeId = officeId;
            this.sites = sites;
            this.workers = workers;
        }
    }

    static final class Solution {
        final List<OpenedOffice> offices;
        final double objective;

        Solution(List<OpenedOffice> offices, double objective) {
            this.offices = offices;
            this.objective = objective;
        }

        static Solution empty() {
            return new Solution(List.of(), 0.0);
        }

        String toJson() {
            StringBuilder sb = new StringBuilder();
            sb.append("[\n");
            for (int i = 0; i < offices.size(); i++) {
                OpenedOffice o = offices.get(i);
                sb.append("  {\n");
                sb.append("    \"officeId\": ").append(o.officeId).append(",\n");
                sb.append("    \"sites\": ").append(ints(o.sites)).append(",\n");
                sb.append("    \"workers\": ").append(ints(o.workers)).append("\n");
                sb.append("  }");
                if (i + 1 < offices.size()) {
                    sb.append(',');
                }
                sb.append('\n');
            }
            sb.append("]\n");
            return sb.toString();
        }

        private static String ints(List<Integer> values) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < values.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(values.get(i));
            }
            sb.append(']');
            return sb.toString();
        }
    }

    /* ============================ ГЕОМЕТРИЯ ============================ */

    /**
     * Выпуклая оболочка и проверка покрытия точки (включая границу),
     * по смыслу как shapely MultiPoint.convex_hull.covers.
     */
    static final class Geometry {
        private static final double EPS = 1e-12;

        static List<Vec> convexHull(List<Vec> points) {
            List<Vec> filtered = new ArrayList<>();
            for (Vec p : points) {
                boolean dup = false;
                for (Vec q : filtered) {
                    if (same(p, q)) {
                        dup = true;
                        break;
                    }
                }
                if (!dup) {
                    filtered.add(p);
                }
            }
            int n = filtered.size();
            if (n <= 2) {
                return filtered;
            }
            filtered.sort(Comparator
                    .comparingDouble((Vec p) -> p.x)
                    .thenComparingDouble(p -> p.y));
            List<Vec> lower = new ArrayList<>();
            for (Vec p : filtered) {
                while (lower.size() >= 2 && cross(lower.get(lower.size() - 2), lower.get(lower.size() - 1), p) <= EPS) {
                    lower.remove(lower.size() - 1);
                }
                lower.add(p);
            }
            List<Vec> upper = new ArrayList<>();
            for (int i = filtered.size() - 1; i >= 0; i--) {
                Vec p = filtered.get(i);
                while (upper.size() >= 2 && cross(upper.get(upper.size() - 2), upper.get(upper.size() - 1), p) <= EPS) {
                    upper.remove(upper.size() - 1);
                }
                upper.add(p);
            }
            lower.remove(lower.size() - 1);
            upper.remove(upper.size() - 1);
            lower.addAll(upper);
            return lower;
        }

        static boolean covers(List<Vec> hull, Vec p) {
            int n = hull.size();
            if (n == 0) {
                return false;
            }
            if (n == 1) {
                return same(hull.get(0), p);
            }
            if (n == 2) {
                return onSegment(hull.get(0), hull.get(1), p);
            }
            boolean hasPos = false;
            boolean hasNeg = false;
            for (int i = 0; i < n; i++) {
                double cr = cross(hull.get(i), hull.get((i + 1) % n), p);
                if (cr > EPS) {
                    hasPos = true;
                } else if (cr < -EPS) {
                    hasNeg = true;
                }
                if (hasPos && hasNeg) {
                    return false;
                }
            }
            return true;
        }

        static boolean same(Vec a, Vec b) {
            return Math.abs(a.x - b.x) <= EPS && Math.abs(a.y - b.y) <= EPS;
        }

        static double cross(Vec a, Vec b, Vec c) {
            return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
        }

        static boolean onSegment(Vec a, Vec b, Vec p) {
            if (Math.abs(cross(a, b, p)) > EPS) {
                return false;
            }
            return p.x + EPS >= Math.min(a.x, b.x) && p.x - EPS <= Math.max(a.x, b.x)
                    && p.y + EPS >= Math.min(a.y, b.y) && p.y - EPS <= Math.max(a.y, b.y);
        }

        static double euclid2(Vec a, Vec b) {
            double dx = a.x - b.x;
            double dy = a.y - b.y;
            return dx * dx + dy * dy;
        }
    }

    /** Проверки, эквивалентные validate_solution.py. */
    static final class Constraints {
        static boolean valid(Instance inst, List<OpenedOffice> opened) {
            int nO = inst.offices.length;
            int nS = inst.sites.length;
            int nW = inst.workers.length;
            boolean[] usedOffice = new boolean[nO];
            boolean[] usedSite = new boolean[nS];
            boolean[] usedWorker = new boolean[nW];
            for (OpenedOffice o : opened) {
                if (o.officeId < 0 || o.officeId >= nO || usedOffice[o.officeId]) {
                    return false;
                }
                usedOffice[o.officeId] = true;
                if (o.sites.size() > inst.offices[o.officeId].maxSize) {
                    return false;
                }
                for (int s : o.sites) {
                    if (s < 0 || s >= nS || usedSite[s]) {
                        return false;
                    }
                    usedSite[s] = true;
                }
                for (int w : o.workers) {
                    if (w < 0 || w >= nW || usedWorker[w]) {
                        return false;
                    }
                    usedWorker[w] = true;
                }
            }
            int minSites = Integer.MAX_VALUE;
            int maxSites = 0;
            for (OpenedOffice o : opened) {
                minSites = Math.min(minSites, o.sites.size());
                maxSites = Math.max(maxSites, o.sites.size());
                for (int s : o.sites) {
                    for (int task : inst.sites[s].tasks) {
                        boolean ok = false;
                        for (int w : o.workers) {
                            if (inst.workerSkills[w].get(task)) {
                                ok = true;
                                break;
                            }
                        }
                        if (!ok) {
                            return false;
                        }
                    }
                }
            }
            if (!opened.isEmpty() && maxSites - minSites > inst.maxDifference) {
                return false;
            }
            boolean[][] covered = new boolean[opened.size()][];
            for (int i = 0; i < opened.size(); i++) {
                OpenedOffice o = opened.get(i);
                List<Vec> raw = new ArrayList<>();
                raw.add(inst.offices[o.officeId].pos);
                for (int s : o.sites) {
                    raw.add(inst.sites[s].pos);
                }
                List<Vec> hull = Geometry.convexHull(raw);
                boolean[] mask = new boolean[nS + nO];
                for (int s = 0; s < nS; s++) {
                    mask[s] = Geometry.covers(hull, inst.sites[s].pos);
                }
                for (int j = 0; j < nO; j++) {
                    mask[nS + j] = Geometry.covers(hull, inst.offices[j].pos);
                }
                covered[i] = mask;
            }
            for (int i = 0; i < opened.size(); i++) {
                for (int j = i + 1; j < opened.size(); j++) {
                    for (int p = 0; p < nS + nO; p++) {
                        if (covered[i][p] && covered[j][p]) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }
    }

    /* ============================ СОЛВЕР ============================ */

    static final class Solver {
        private final Instance inst;
        private final long deadlineNanos;
        private Solution best = Solution.empty();

        Solver(Instance inst, long deadlineNanos) {
            this.inst = inst;
            this.deadlineNanos = deadlineNanos;
        }

        boolean timeLeft() {
            return System.nanoTime() < deadlineNanos;
        }

        Solution solve() {
            int nOff = inst.offices.length;
            if (nOff == 0 || inst.sites.length == 0) {
                return Solution.empty();
            }

            double[] singleScore = new double[nOff];
            Integer[] rank = new Integer[nOff];
            for (int o = 0; o < nOff; o++) {
                consider(buildSingleOffice(o));
                singleScore[o] = estimateSingle(o);
                rank[o] = o;
            }
            Arrays.sort(rank, Comparator.comparingDouble((Integer o) -> singleScore[o]).reversed());

            int maxK = Math.min(nOff, 8);
            for (int k = 2; k <= maxK && timeLeft(); k++) {
                long combos = binom(nOff, k);
                if (combos <= 2500) {
                    int[] cur = new int[k];
                    enumerate(0, 0, k, cur);
                } else {
                    sampleRanked(rank, k, 400);
                }
            }
            return best;
        }

        private void enumerate(int start, int filled, int k, int[] cur) {
            if (!timeLeft()) {
                return;
            }
            if (filled == k) {
                consider(buildMultiOffice(cur.clone()));
                return;
            }
            int nOff = inst.offices.length;
            for (int i = start; i <= nOff - (k - filled); i++) {
                cur[filled] = i;
                enumerate(i + 1, filled + 1, k, cur);
                if (!timeLeft()) {
                    return;
                }
            }
        }

        private void sampleRanked(Integer[] rank, int k, int limit) {
            int nOff = inst.offices.length;
            int[] cur = new int[k];
            int tried = 0;
            // Сначала комбинации из лучших офисов (префикс ранжирования).
            int pref = Math.min(nOff, Math.max(k + 2, 8));
            tried += enumerateLimited(rank, pref, k, 0, 0, cur, limit - tried);
            while (tried < limit && timeLeft()) {
                // Дополнительные случайные подмножества со смещением к лучшим.
                boolean[] used = new boolean[nOff];
                int filled = 0;
                int guard = 0;
                while (filled < k && guard < 10_000) {
                    guard++;
                    int idx = biasedIndex(nOff, tried + filled);
                    int o = rank[idx];
                    if (!used[o]) {
                        used[o] = true;
                        cur[filled++] = o;
                    }
                }
                if (filled == k) {
                    Arrays.sort(cur);
                    consider(buildMultiOffice(cur.clone()));
                    tried++;
                } else {
                    break;
                }
            }
        }

        private int enumerateLimited(Integer[] rank, int pref, int k, int start, int filled,
                                     int[] cur, int remaining) {
            if (remaining <= 0 || !timeLeft()) {
                return 0;
            }
            if (filled == k) {
                consider(buildMultiOffice(cur.clone()));
                return 1;
            }
            int used = 0;
            for (int i = start; i <= pref - (k - filled); i++) {
                cur[filled] = rank[i];
                used += enumerateLimited(rank, pref, k, i + 1, filled + 1, cur, remaining - used);
                if (remaining - used <= 0 || !timeLeft()) {
                    break;
                }
            }
            return used;
        }

        private int biasedIndex(int n, int salt) {
            // Чаще берём офисы из начала рейтинга.
            double u = Math.abs(Math.sin(salt * 12.9898 + n * 78.233)) % 1.0;
            return Math.min(n - 1, (int) (u * u * n));
        }

        private void consider(Solution sol) {
            if (sol == null || sol.offices.isEmpty()) {
                return;
            }
            if (!Constraints.valid(inst, sol.offices)) {
                return;
            }
            if (sol.objective > best.objective + 1e-9) {
                best = sol;
            }
        }

        /** Верхняя оценка одного офиса без геометрии (для ранжирования). */
        private double estimateSingle(int officeId) {
            Office office = inst.offices[officeId];
            List<double[]> vals = new ArrayList<>();
            for (int s = 0; s < inst.sites.length; s++) {
                if (!inst.siteFeasible(s)) {
                    continue;
                }
                double v = inst.sites[s].reward - inst.distances[officeId][s];
                if (v > 0) {
                    vals.add(new double[]{v});
                }
            }
            vals.sort((a, b) -> Double.compare(b[0], a[0]));
            double sum = 0;
            int take = Math.min(office.maxSize, vals.size());
            for (int i = 0; i < take; i++) {
                sum += vals.get(i)[0];
            }
            return sum - office.cost;
        }

        private Solution buildSingleOffice(int officeId) {
            Office office = inst.offices[officeId];
            if (office.maxSize < 1) {
                return Solution.empty();
            }
            List<int[]> cand = new ArrayList<>();
            for (int s = 0; s < inst.sites.length; s++) {
                if (!inst.siteFeasible(s)) {
                    continue;
                }
                double v = inst.sites[s].reward - inst.distances[officeId][s];
                if (v > 0) {
                    cand.add(new int[]{s, Double.hashCode(v)});
                }
            }
            cand.sort((a, b) -> {
                double va = inst.sites[a[0]].reward - inst.distances[officeId][a[0]];
                double vb = inst.sites[b[0]].reward - inst.distances[officeId][b[0]];
                return Double.compare(vb, va);
            });
            List<Integer> picked = new ArrayList<>();
            BitSet need = new BitSet();
            for (int[] c : cand) {
                if (picked.size() >= office.maxSize) {
                    break;
                }
                picked.add(c[0]);
                need.or(inst.sites[c[0]].skillSet);
            }
            List<Integer> workers = assignWorkers(new BitSet[]{need});
            if (workers == null) {
                // Снимаем объекты с дефицитными навыками.
                picked = dropUntilCoverable(picked);
                need = skillsOf(picked);
                workers = assignWorkers(new BitSet[]{need});
                if (workers == null) {
                    return Solution.empty();
                }
            }
            if (picked.isEmpty()) {
                return Solution.empty();
            }
            OpenedOffice opened = new OpenedOffice(officeId, picked, workers);
            return pack(List.of(opened));
        }

        private Solution buildMultiOffice(int[] officeIds) {
            int k = officeIds.length;
            int nSites = inst.sites.length;
            int[] assign = new int[nSites];
            Arrays.fill(assign, -1);
            int[] count = new int[k];
            int[] voronoi = new int[nSites];
            Arrays.fill(voronoi, -1);

            for (int s = 0; s < nSites; s++) {
                if (!inst.siteFeasible(s)) {
                    continue;
                }
                int bestO = -1;
                double bestD = Double.POSITIVE_INFINITY;
                for (int t = 0; t < k; t++) {
                    double d = Geometry.euclid2(inst.sites[s].pos, inst.offices[officeIds[t]].pos);
                    if (d < bestD) {
                        bestD = d;
                        bestO = t;
                    }
                }
                voronoi[s] = bestO;
            }

            List<int[]> order = new ArrayList<>();
            for (int s = 0; s < nSites; s++) {
                if (voronoi[s] < 0) {
                    continue;
                }
                int t = voronoi[s];
                double v = inst.sites[s].reward - inst.distances[officeIds[t]][s];
                if (v <= 0) {
                    continue;
                }
                double d = Geometry.euclid2(inst.sites[s].pos, inst.offices[officeIds[t]].pos);
                order.add(new int[]{s, t, Double.hashCode(d)});
            }
            order.sort(Comparator.comparingDouble(a ->
                    Geometry.euclid2(inst.sites[a[0]].pos, inst.offices[officeIds[a[1]]].pos)));

            List<Vec>[] hulls = newList(k);
            boolean[][] covered = new boolean[k][];

            for (int[] rec : order) {
                int s = rec[0];
                int t = rec[1];
                int officeId = officeIds[t];
                if (assign[s] >= 0) {
                    continue;
                }
                if (count[t] >= inst.offices[officeId].maxSize) {
                    continue;
                }
                if (!countOk(count, t, 1)) {
                    continue;
                }
                if (!geometryOk(officeIds, assign, t, s, hulls, covered)) {
                    continue;
                }
                assign[s] = t;
                count[t]++;
                refreshHull(officeIds, assign, t, hulls, covered);
            }

            // Добираем оставшиеся выгодные объекты в любой офис (не только Voronoi).
            List<int[]> leftover = new ArrayList<>();
            for (int s = 0; s < nSites; s++) {
                if (assign[s] >= 0 || !inst.siteFeasible(s)) {
                    continue;
                }
                for (int t = 0; t < k; t++) {
                    double v = inst.sites[s].reward - inst.distances[officeIds[t]][s];
                    if (v > 0) {
                        leftover.add(new int[]{s, t});
                    }
                }
            }
            leftover.sort((a, b) -> {
                double va = inst.sites[a[0]].reward - inst.distances[officeIds[a[1]]][a[0]];
                double vb = inst.sites[b[0]].reward - inst.distances[officeIds[b[1]]][b[0]];
                return Double.compare(vb, va);
            });
            for (int[] rec : leftover) {
                int s = rec[0];
                int t = rec[1];
                if (assign[s] >= 0) {
                    continue;
                }
                int officeId = officeIds[t];
                if (count[t] >= inst.offices[officeId].maxSize) {
                    continue;
                }
                if (!countOk(count, t, 1)) {
                    continue;
                }
                if (!geometryOk(officeIds, assign, t, s, hulls, covered)) {
                    continue;
                }
                assign[s] = t;
                count[t]++;
                refreshHull(officeIds, assign, t, hulls, covered);
            }

            trimTaskImbalance(officeIds, assign, count, hulls, covered);
            return toSolution(officeIds, assign);
        }

        @SuppressWarnings("unchecked")
        private List<Vec>[] newList(int k) {
            return new List[k];
        }

        private boolean countOk(int[] count, int t, int delta) {
            int[] tmp = count.clone();
            tmp[t] += delta;
            int min = Integer.MAX_VALUE;
            int max = 0;
            boolean any = false;
            for (int c : tmp) {
                if (c > 0) {
                    any = true;
                    min = Math.min(min, c);
                    max = Math.max(max, c);
                }
            }
            if (!any) {
                return true;
            }
            return max - min <= inst.maxDifference;
        }

        private boolean geometryOk(int[] officeIds, int[] assign, int t, int siteId,
                                   List<Vec>[] hulls, boolean[][] covered) {
            List<Integer> pts = new ArrayList<>();
            pts.add(siteId);
            for (int s = 0; s < assign.length; s++) {
                if (assign[s] == t) {
                    pts.add(s);
                }
            }
            List<Vec> raw = new ArrayList<>(pts.size() + 1);
            raw.add(inst.offices[officeIds[t]].pos);
            for (int s : pts) {
                raw.add(inst.sites[s].pos);
            }
            List<Vec> hull = Geometry.convexHull(raw);
            boolean[] cov = coverMask(hull);
            int nSites = inst.sites.length;
            for (int u = 0; u < officeIds.length; u++) {
                if (u == t || hulls[u] == null) {
                    continue;
                }
                if (intersects(cov, covered[u], nSites + inst.offices.length)) {
                    return false;
                }
            }
            return true;
        }

        private void refreshHull(int[] officeIds, int[] assign, int t,
                                 List<Vec>[] hulls, boolean[][] covered) {
            List<Vec> raw = new ArrayList<>();
            raw.add(inst.offices[officeIds[t]].pos);
            for (int s = 0; s < assign.length; s++) {
                if (assign[s] == t) {
                    raw.add(inst.sites[s].pos);
                }
            }
            hulls[t] = Geometry.convexHull(raw);
            covered[t] = coverMask(hulls[t]);
        }

        private boolean[] coverMask(List<Vec> hull) {
            int n = inst.sites.length + inst.offices.length;
            boolean[] cov = new boolean[n];
            for (int i = 0; i < inst.sites.length; i++) {
                cov[i] = Geometry.covers(hull, inst.sites[i].pos);
            }
            for (int j = 0; j < inst.offices.length; j++) {
                cov[inst.sites.length + j] = Geometry.covers(hull, inst.offices[j].pos);
            }
            return cov;
        }

        private boolean intersects(boolean[] a, boolean[] b, int n) {
            for (int i = 0; i < n; i++) {
                if (a[i] && b[i]) {
                    return true;
                }
            }
            return false;
        }

        /**
         * При большом alpha выгоднее выровнять число задач, даже потеряв объект.
         */
        private void trimTaskImbalance(int[] officeIds, int[] assign, int[] count,
                                       List<Vec>[] hulls, boolean[][] covered) {
            if (inst.balanceCoefficient <= 0 || officeIds.length < 2) {
                return;
            }
            int guard = 0;
            while (guard++ < inst.sites.length) {
                int[] tasks = taskLoads(officeIds, assign);
                int maxT = -1;
                int minT = -1;
                int maxV = Integer.MIN_VALUE;
                int minV = Integer.MAX_VALUE;
                for (int t = 0; t < officeIds.length; t++) {
                    if (count[t] == 0) {
                        continue;
                    }
                    if (tasks[t] > maxV) {
                        maxV = tasks[t];
                        maxT = t;
                    }
                    if (tasks[t] < minV) {
                        minV = tasks[t];
                        minT = t;
                    }
                }
                if (maxT < 0 || minT < 0 || maxT == minT) {
                    break;
                }
                int gap = maxV - minV;
                if (gap <= 0) {
                    break;
                }
                // Ищем самый дешёвый объект в перегруженном офисе.
                int drop = -1;
                double dropLoss = Double.POSITIVE_INFINITY;
                for (int s = 0; s < assign.length; s++) {
                    if (assign[s] != maxT) {
                        continue;
                    }
                    double loss = inst.sites[s].reward - inst.distances[officeIds[maxT]][s];
                    double gain = inst.balanceCoefficient * Math.min(gap, inst.sites[s].tasks.length);
                    if (gain > loss && loss < dropLoss) {
                        dropLoss = loss;
                        drop = s;
                    }
                }
                if (drop < 0) {
                    break;
                }
                assign[drop] = -1;
                count[maxT]--;
                if (count[maxT] == 0) {
                    hulls[maxT] = null;
                    covered[maxT] = null;
                } else {
                    refreshHull(officeIds, assign, maxT, hulls, covered);
                }
                if (!countOk(count, maxT, 0) && maxT >= 0) {
                    // После удаления мог нарушиться баланс числа объектов: вернём, если нельзя оставить.
                    // Здесь countOk с delta 0 проверяет текущее состояние.
                }
                int minC = Integer.MAX_VALUE;
                int maxC = 0;
                int opened = 0;
                for (int c : count) {
                    if (c > 0) {
                        opened++;
                        minC = Math.min(minC, c);
                        maxC = Math.max(maxC, c);
                    }
                }
                if (opened >= 2 && maxC - minC > inst.maxDifference) {
                    assign[drop] = maxT;
                    count[maxT]++;
                    refreshHull(officeIds, assign, maxT, hulls, covered);
                    break;
                }
            }
        }

        private int[] taskLoads(int[] officeIds, int[] assign) {
            int[] tasks = new int[officeIds.length];
            for (int s = 0; s < assign.length; s++) {
                if (assign[s] >= 0) {
                    tasks[assign[s]] += inst.sites[s].tasks.length;
                }
            }
            return tasks;
        }

        private Solution toSolution(int[] officeIds, int[] assign) {
            int k = officeIds.length;
            List<List<Integer>> groups = new ArrayList<>();
            for (int t = 0; t < k; t++) {
                groups.add(new ArrayList<>());
            }
            for (int s = 0; s < assign.length; s++) {
                if (assign[s] >= 0) {
                    groups.get(assign[s]).add(s);
                }
            }
            List<Integer> usedOffices = new ArrayList<>();
            List<List<Integer>> usedGroups = new ArrayList<>();
            List<BitSet> needs = new ArrayList<>();
            for (int t = 0; t < k; t++) {
                if (groups.get(t).isEmpty()) {
                    continue;
                }
                usedOffices.add(officeIds[t]);
                usedGroups.add(groups.get(t));
                needs.add(skillsOf(groups.get(t)));
            }
            if (usedOffices.isEmpty()) {
                return Solution.empty();
            }

            repairSiteCountBalance(usedOffices, usedGroups);
            if (usedOffices.isEmpty()) {
                return Solution.empty();
            }
            needs.clear();
            for (List<Integer> group : usedGroups) {
                needs.add(skillsOf(group));
            }

            List<Integer> allWorkers = assignWorkers(needs.toArray(BitSet[]::new));
            if (allWorkers == null) {
                // Жадно выкидываем объекты, требующие дефицитных навыков.
                for (int i = 0; i < usedGroups.size(); i++) {
                    usedGroups.set(i, dropUntilCoverablePerOffice(usedGroups, i));
                }
                List<Integer> nonemptyOff = new ArrayList<>();
                List<List<Integer>> nonemptyGr = new ArrayList<>();
                List<BitSet> nonemptyNeed = new ArrayList<>();
                for (int i = 0; i < usedGroups.size(); i++) {
                    if (usedGroups.get(i).isEmpty()) {
                        continue;
                    }
                    nonemptyOff.add(usedOffices.get(i));
                    nonemptyGr.add(usedGroups.get(i));
                    nonemptyNeed.add(skillsOf(usedGroups.get(i)));
                }
                usedOffices = nonemptyOff;
                usedGroups = nonemptyGr;
                needs = nonemptyNeed;
                if (usedOffices.isEmpty()) {
                    return Solution.empty();
                }
                allWorkers = assignWorkers(needs.toArray(BitSet[]::new));
                if (allWorkers == null) {
                    return Solution.empty();
                }
            }

            List<OpenedOffice> opened = new ArrayList<>();
            BitSet[] needArr = needs.toArray(BitSet[]::new);
            List<Integer>[] split = allocateWorkers(needArr);
            if (split == null) {
                return Solution.empty();
            }
            for (int i = 0; i < usedOffices.size(); i++) {
                opened.add(new OpenedOffice(usedOffices.get(i), usedGroups.get(i), split[i]));
            }
            return pack(opened);
        }

        /**
         * После удаления пустых офисов / объектов с дефицитными навыками
         * разница числа объектов может превысить maxDifference.
         * Снимаем наименее выгодные объекты с самых больших кластеров.
         */
        private void repairSiteCountBalance(List<Integer> officeIds, List<List<Integer>> groups) {
            int guard = 0;
            while (guard++ < inst.sites.length + 2) {
                if (groups.isEmpty()) {
                    return;
                }
                int minI = 0;
                int maxI = 0;
                int minV = groups.get(0).size();
                int maxV = minV;
                for (int i = 1; i < groups.size(); i++) {
                    int sz = groups.get(i).size();
                    if (sz < minV) {
                        minV = sz;
                        minI = i;
                    }
                    if (sz > maxV) {
                        maxV = sz;
                        maxI = i;
                    }
                }
                if (maxV - minV <= inst.maxDifference) {
                    return;
                }
                List<Integer> big = groups.get(maxI);
                if (big.isEmpty()) {
                    officeIds.remove(maxI);
                    groups.remove(maxI);
                    continue;
                }
                int dropAt = 0;
                double worst = Double.POSITIVE_INFINITY;
                int officeId = officeIds.get(maxI);
                for (int j = 0; j < big.size(); j++) {
                    int s = big.get(j);
                    double v = inst.sites[s].reward - inst.distances[officeId][s];
                    if (v < worst) {
                        worst = v;
                        dropAt = j;
                    }
                }
                big.remove(dropAt);
                if (big.isEmpty()) {
                    officeIds.remove(maxI);
                    groups.remove(maxI);
                }
            }
        }

        private List<Integer> dropUntilCoverable(List<Integer> sites) {
            List<Integer> rest = new ArrayList<>(sites);
            while (!rest.isEmpty()) {
                BitSet need = skillsOf(rest);
                if (assignWorkers(new BitSet[]{need}) != null) {
                    return rest;
                }
                int drop = scarcestSite(rest);
                if (drop < 0) {
                    rest.remove(rest.size() - 1);
                } else {
                    rest.remove(Integer.valueOf(drop));
                }
            }
            return rest;
        }

        private List<Integer> dropUntilCoverablePerOffice(List<List<Integer>> groups, int idx) {
            List<Integer> rest = new ArrayList<>(groups.get(idx));
            while (!rest.isEmpty()) {
                BitSet[] needs = new BitSet[groups.size()];
                for (int i = 0; i < groups.size(); i++) {
                    needs[i] = i == idx ? skillsOf(rest) : skillsOf(groups.get(i));
                }
                if (assignWorkers(needs) != null) {
                    return rest;
                }
                int drop = scarcestSite(rest);
                if (drop < 0) {
                    rest.remove(rest.size() - 1);
                } else {
                    rest.remove(Integer.valueOf(drop));
                }
            }
            return rest;
        }

        private int scarcestSite(List<Integer> sites) {
            int best = -1;
            int bestSupply = Integer.MAX_VALUE;
            for (int s : sites) {
                for (int skill = inst.sites[s].skillSet.nextSetBit(0);
                     skill >= 0;
                     skill = inst.sites[s].skillSet.nextSetBit(skill + 1)) {
                    int supply = skill < inst.skillSupply.length ? inst.skillSupply[skill] : 0;
                    if (supply < bestSupply) {
                        bestSupply = supply;
                        best = s;
                    }
                }
            }
            return best;
        }

        private BitSet skillsOf(List<Integer> sites) {
            BitSet need = new BitSet();
            for (int s : sites) {
                need.or(inst.sites[s].skillSet);
            }
            return need;
        }

        /**
         * Назначает работников офисам так, чтобы покрыть требуемые навыки.
         * Возвращает плоский список (для одного офиса) или null, если покрытия нет.
         */
        private List<Integer> assignWorkers(BitSet[] needs) {
            List<Integer>[] perOffice = allocateWorkers(needs);
            if (perOffice == null) {
                return null;
            }
            List<Integer> flat = new ArrayList<>();
            for (List<Integer> w : perOffice) {
                flat.addAll(w);
            }
            return flat;
        }

        @SuppressWarnings("unchecked")
        private List<Integer>[] allocateWorkers(BitSet[] needs) {
            boolean[] used = new boolean[inst.workers.length];
            return allocateWorkers(needs, used);
        }

        @SuppressWarnings("unchecked")
        private List<Integer>[] allocateWorkers(BitSet[] needs, boolean[] used) {
            List<Integer>[] perOffice = new List[needs.length];
            for (int i = 0; i < needs.length; i++) {
                perOffice[i] = new ArrayList<>();
                BitSet remain = (BitSet) needs[i].clone();
                // Жадное покрытие множества навыков.
                while (!remain.isEmpty()) {
                    int bestW = -1;
                    int bestCover = 0;
                    for (int w = 0; w < inst.workers.length; w++) {
                        if (used[w]) {
                            continue;
                        }
                        int cover = 0;
                        for (int s = remain.nextSetBit(0); s >= 0; s = remain.nextSetBit(s + 1)) {
                            if (inst.workerSkills[w].get(s)) {
                                cover++;
                            }
                        }
                        if (cover > bestCover) {
                            bestCover = cover;
                            bestW = w;
                        }
                    }
                    if (bestW < 0 || bestCover == 0) {
                        return null;
                    }
                    used[bestW] = true;
                    perOffice[i].add(bestW);
                    remain.andNot(inst.workerSkills[bestW]);
                }
            }
            return perOffice;
        }

        private Solution pack(List<OpenedOffice> opened) {
            if (opened.isEmpty() || !Constraints.valid(inst, opened)) {
                return Solution.empty();
            }
            double reward = 0;
            double distance = 0;
            double cost = 0;
            int minTasks = Integer.MAX_VALUE;
            int maxTasks = 0;
            for (OpenedOffice o : opened) {
                cost += inst.offices[o.officeId].cost;
                int tasks = 0;
                for (int s : o.sites) {
                    reward += inst.sites[s].reward;
                    distance += inst.distances[o.officeId][s];
                    tasks += inst.sites[s].tasks.length;
                }
                minTasks = Math.min(minTasks, tasks);
                maxTasks = Math.max(maxTasks, tasks);
            }
            double imbalance = opened.isEmpty() ? 0 : (maxTasks - minTasks);
            double obj = reward - distance - cost - inst.balanceCoefficient * imbalance;
            return new Solution(opened, obj);
        }

        private static long binom(int n, int k) {
            if (k < 0 || k > n) {
                return 0;
            }
            long r = 1;
            for (int i = 1; i <= k; i++) {
                r = r * (n - k + i) / i;
                if (r > 10_000_000L) {
                    return r;
                }
            }
            return r;
        }
    }
}
