//package org.example.hackatones.TechArena2026;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Эвристический солвер TechArena 2026: открытие сервисных офисов,
 * назначение объектов и специалистов.
 *
 * Целевая функция (максимизация):
 * reward − distance − cost − alpha * (maxTasks − minTasks)
 *
 * Ограничения повторяют validate_solution.py: индексы, ёмкость, баланс числа
 * объектов, покрытие навыков, непересекающиеся выпуклые оболочки (офис + объекты).
 *
 * Поиск как в версии с OPZ 7.5: один поток, дешёвые сборки, k вокруг predictK.
 * Thorough (дорога, навыки, leftover) — только на элитах, пока остаётся CPU:
 * дешёвый construct часто занижает хороший набор.
 */
public class Main {

    /**
     * Лимит NSUTS — процессорное время ~300 с. Многопоток его умножает и даёт TLE.
     * Держим один поток и оставляем запас.
     */
    private static final long DEFAULT_TIME_LIMIT_MS = 200_000L;

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
        Solution solution = Solution.empty();
        try {
            solution = solver.solve();
        } finally {
            Files.writeString(output, solution.toJson(), StandardCharsets.UTF_8);
        }
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

        static List<Integer> intList(Object value) {
            int[] a = intArray(value);
            List<Integer> out = new ArrayList<>(a.length);
            for (int v : a) {
                out.add(v);
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

        /** Целевая как в validate_solution.py. */
        static double objectiveOf(Instance inst, List<OpenedOffice> opened) {
            if (opened.isEmpty()) {
                return 0.0;
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
            return reward - distance - cost - inst.balanceCoefficient * (maxTasks - minTasks);
        }

        static Solution checked(Instance inst, List<OpenedOffice> opened) {
            if (opened.isEmpty() || !Constraints.valid(inst, opened)) {
                return empty();
            }
            return new Solution(opened, objectiveOf(inst, opened));
        }

        @SuppressWarnings("unchecked")
        static Solution fromJson(Instance inst, Object raw) {
            if (!(raw instanceof List<?> arr)) {
                return empty();
            }
            List<OpenedOffice> opened = new ArrayList<>();
            for (Object item : arr) {
                if (!(item instanceof Map<?, ?> map)) {
                    return empty();
                }
                Map<String, Object> m = (Map<String, Object>) map;
                opened.add(new OpenedOffice(
                        (int) Instance.num(m.get("officeId")),
                        Instance.intList(m.get("sites")),
                        Instance.intList(m.get("workers"))));
            }
            return checked(inst, opened);
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

    /** Кандидат объекта внутри кластера. */
    static final class Cand {
        final int site;
        final double value;
        final int tasks;

        Cand(int site, double value, int tasks) {
            this.site = site;
            this.value = value;
            this.tasks = tasks;
        }
    }

    /* ============================ СОЛВЕР ============================ */

    static final class Solver {
        /** Запас на финальную thorough-сборку и запись JSON. */
        private static final long THOROUGH_RESERVE_NS = 12_000_000_000L;
        /** Сколько лучших дешёвых наборов потом считаем thoroughly. */
        private static final int ELITE_CAP = 12;
        private static final int THOROUGH_NEIGHBOR_CAP = 8;
        private static final int RECOVER_GUARD = 40;
        private static final int LEFTOVER_RANK_CAP = 300;
        private static final int DIRECT_TRY_CAP = 64;
        private static final int PAIR_TRY_CAP = 24;

        private final Instance inst;
        private final long deadlineNanos;
        private final long reserveNs;
        private Solution best = Solution.empty();
        private long rng = 20261L;
        private final Set<String> searchedSets = new HashSet<>();
        private final List<Elite> elites = new ArrayList<>();
        private final double[] bestObjByK;
        private final int[][] bestIdsByK;

        static final class Elite {
            final int[] ids;
            double obj;

            Elite(int[] ids, double obj) {
                this.ids = ids;
                this.obj = obj;
            }
        }

        Solver(Instance inst, long deadlineNanos) {
            this.inst = inst;
            this.deadlineNanos = deadlineNanos;
            long budget = Math.max(0L, deadlineNanos - System.nanoTime());
            this.reserveNs = Math.min(THOROUGH_RESERVE_NS, Math.max(budget / 10, 2_000_000_000L));
            this.bestObjByK = new double[inst.offices.length + 1];
            Arrays.fill(this.bestObjByK, -1e100);
            this.bestIdsByK = new int[inst.offices.length + 1][];
        }

        boolean timeLeft() {
            return System.nanoTime() < deadlineNanos;
        }

        /** Поиск дешёвыми сборками останавливаем раньше, чтобы успеть thorough. */
        boolean searchTimeLeft() {
            return System.nanoTime() + reserveNs < deadlineNanos;
        }

        int randInt(int n) {
            if (n <= 1) {
                return 0;
            }
            rng ^= rng << 13;
            rng ^= rng >>> 7;
            rng ^= rng << 17;
            return (int) ((rng & Long.MAX_VALUE) % n);
        }

        Solution solve() {
            int nOff = inst.offices.length;
            if (nOff == 0 || inst.sites.length == 0) {
                return Solution.empty();
            }

            Integer[] rank = new Integer[nOff];
            double[] singleObj = new double[nOff];
            for (int o = 0; o < nOff; o++) {
                rank[o] = o;
                singleObj[o] = -1e100;
            }
            for (int o = 0; o < nOff && timeLeft(); o++) {
                Solution sol = construct(new int[]{o}, false);
                consider(sol);
                singleObj[o] = sol.objective;
            }
            Arrays.sort(rank, (a, b) -> Double.compare(singleObj[b], singleObj[a]));

            int maxK = Math.min(nOff, inst.workers.length);
            // Малый nOff: полный перебор наборов. Cheap недооценивает пары — для
            // nOff ≤ 8 сразу thorough, иначе дешёвый перебор и thorough элит.
            if (nOff <= 8) {
                searchAllMasks(maxK, true);
            } else if (nOff <= 12) {
                searchAllMasks(maxK, false);
            } else {
                int predicted = predictK(rank);
                Set<Integer> searchedK = new HashSet<>();
                for (int k : new int[]{2, 3}) {
                    if (k <= maxK && searchedK.add(k) && searchTimeLeft()) {
                        searchK(k, rank);
                    }
                }
                int lo = Math.max(2, predicted - 1);
                int hi = Math.min(maxK, predicted + 1);
                for (int k = lo; k <= hi && searchTimeLeft(); k++) {
                    if (searchedK.add(k)) {
                        searchK(k, rank);
                    }
                }
                searchSpread(rank, maxK);
                searchUntilTime(rank, predicted, maxK);
            }
            refineThorough(rank);
            return best;
        }

        /**
         * Все непустые подмножества офисов (2^nOff − 1). nOff ≤ 12 → ≤ 4095 наборов.
         */
        private void searchAllMasks(int maxK, boolean thorough) {
            int nOff = inst.offices.length;
            int limit = 1 << nOff;
            for (int mask = 1; mask < limit && timeLeft(); mask++) {
                if (!thorough && !searchTimeLeft()) {
                    break;
                }
                int k = Integer.bitCount(mask);
                if (k > maxK) {
                    continue;
                }
                int[] ids = new int[k];
                int p = 0;
                for (int o = 0; o < nOff; o++) {
                    if ((mask & (1 << o)) != 0) {
                        ids[p++] = o;
                    }
                }
                consider(construct(ids, thorough));
            }
        }

        /**
         * Дешёвая сборка занижает leftover/навыки/дорогу — пары вроде 5+11
         * на дешёвом construct выглядят слабыми. Thorough чинит назначение.
         * Делаем его не на всём поиске, а на элитах, пока есть CPU.
         */
        private void refineThorough(Integer[] rank) {
            List<int[]> queue = new ArrayList<>();
            addUniqueSet(queue, bestIds());
            for (int k = 1; k < bestIdsByK.length; k++) {
                addUniqueSet(queue, bestIdsByK[k]);
            }
            elites.sort((a, b) -> Double.compare(b.obj, a.obj));
            for (Elite e : elites) {
                addUniqueSet(queue, e.ids);
            }
            Set<String> done = new HashSet<>();
            for (int[] ids : queue) {
                if (!timeLeft() || ids == null || ids.length == 0) {
                    continue;
                }
                String key = Arrays.toString(ids);
                if (!done.add(key)) {
                    continue;
                }
                consider(construct(ids, true));
            }
            thoroughNeighbors(rank, done);
        }

        private void addUniqueSet(List<int[]> queue, int[] ids) {
            if (ids == null || ids.length == 0) {
                return;
            }
            int[] copy = ids.clone();
            Arrays.sort(copy);
            for (int[] e : queue) {
                if (Arrays.equals(e, copy)) {
                    return;
                }
            }
            queue.add(copy);
        }

        private void thoroughNeighbors(Integer[] rank, Set<String> done) {
            if (best.offices.isEmpty() || !timeLeft()) {
                return;
            }
            int[] cur = bestIds();
            int nOff = inst.offices.length;
            boolean[] in = new boolean[nOff];
            for (int id : cur) {
                in[id] = true;
            }
            int extra = 0;
            int bound = Math.min(nOff, Math.max(8, cur.length * 3));
            for (int r = 0; r < bound && extra < THOROUGH_NEIGHBOR_CAP && timeLeft(); r++) {
                int neu = rank[r];
                if (in[neu] || inst.offices[neu].maxSize < 1) {
                    continue;
                }
                for (int i = 0; i < cur.length && extra < THOROUGH_NEIGHBOR_CAP && timeLeft(); i++) {
                    int[] next = cur.clone();
                    next[i] = neu;
                    Arrays.sort(next);
                    String key = Arrays.toString(next);
                    if (!done.add(key)) {
                        continue;
                    }
                    consider(construct(next, true));
                    extra++;
                }
            }
        }

        /**
         * Оценка выгодного числа офисов: берём k лучших по одиночной оценке
         * и добавляем офис, пока probe-сборка растёт. Это не полный перебор k.
         */
        private int predictK(Integer[] rank) {
            int nOff = inst.offices.length;
            int cap = Math.min(nOff, Math.min(inst.workers.length, 6));
            int predicted = 1;
            double prev = best.objective;
            for (int k = 2; k <= cap && searchTimeLeft(); k++) {
                int[] probe = new int[k];
                for (int i = 0; i < k; i++) {
                    probe[i] = rank[i];
                }
                Solution sol = construct(probe, false);
                consider(sol);
                if (sol.objective > prev + 1e-6) {
                    predicted = k;
                    prev = sol.objective;
                } else {
                    break;
                }
            }
            return predicted;
        }

        private void searchK(int k, Integer[] rank) {
            int nOff = inst.offices.length;
            long combos = binom(nOff, k);
            if (combos <= 400) {
                int[] buf = new int[k];
                enumerate(0, 0, k, buf);
                return;
            }
            int limit = k <= 3 ? 80 : 40;
            Set<String> seen = new HashSet<>();
            int pref = Math.min(nOff, Math.max(k + 2, 8));
            int[] buf = new int[k];
            collectFromRank(rank, pref, 0, 0, k, buf, seen, limit);
            while (seen.size() < limit && searchTimeLeft()) {
                int[] pick = new int[k];
                boolean[] used = new boolean[nOff];
                int filled = 0;
                int guard = 0;
                while (filled < k && guard++ < nOff * 8) {
                    int o = rank[randInt(pref)];
                    if (randInt(4) == 0) {
                        o = randInt(nOff);
                    }
                    if (!used[o] && inst.offices[o].maxSize >= 1) {
                        used[o] = true;
                        pick[filled++] = o;
                    }
                }
                if (filled == k) {
                    Arrays.sort(pick);
                    if (seen.add(Arrays.toString(pick))) {
                        considerCheap(pick);
                    }
                }
            }
        }

        private void enumerate(int start, int filled, int k, int[] buf) {
            if (!searchTimeLeft()) {
                return;
            }
            if (filled == k) {
                considerCheap(buf);
                return;
            }
            int nOff = inst.offices.length;
            for (int i = start; i <= nOff - (k - filled); i++) {
                buf[filled] = i;
                enumerate(i + 1, filled + 1, k, buf);
                if (!searchTimeLeft()) {
                    return;
                }
            }
        }

        private void collectFromRank(Integer[] rank, int pref, int start, int filled, int k,
                                     int[] buf, Set<String> seen, int limit) {
            if (!searchTimeLeft() || seen.size() >= limit) {
                return;
            }
            if (filled == k) {
                int[] copy = buf.clone();
                Arrays.sort(copy);
                if (seen.add(Arrays.toString(copy))) {
                    considerCheap(copy);
                }
                return;
            }
            for (int i = start; i <= pref - (k - filled) && searchTimeLeft(); i++) {
                buf[filled] = rank[i];
                collectFromRank(rank, pref, i + 1, filled + 1, k, buf, seen, limit);
            }
        }

        /** Пространственно разнесённые офисы — дешёвая сборка, один набор на k. */
        private void searchSpread(Integer[] rank, int maxK) {
            int up = Math.min(maxK, 6);
            for (int k = 2; k <= up && searchTimeLeft(); k++) {
                int[] ids = pickSpread(k, rank);
                if (ids != null) {
                    considerCheap(ids);
                }
            }
        }

        private int[] pickSpread(int k, Integer[] rank) {
            int nOff = inst.offices.length;
            if (k > nOff) {
                return null;
            }
            boolean[] used = new boolean[nOff];
            int[] ids = new int[k];
            ids[0] = rank[0];
            used[rank[0]] = true;
            for (int filled = 1; filled < k; filled++) {
                int bestId = -1;
                double bestMin = -1;
                int bound = Math.min(nOff, Math.max(k * 4, 12));
                for (int r = 0; r < bound; r++) {
                    int o = rank[r];
                    if (used[o] || inst.offices[o].maxSize < 1) {
                        continue;
                    }
                    double minD = Double.POSITIVE_INFINITY;
                    for (int i = 0; i < filled; i++) {
                        minD = Math.min(minD, Geometry.euclid2(inst.offices[o].pos, inst.offices[ids[i]].pos));
                    }
                    if (minD > bestMin) {
                        bestMin = minD;
                        bestId = o;
                    }
                }
                if (bestId < 0) {
                    return null;
                }
                used[bestId] = true;
                ids[filled] = bestId;
            }
            Arrays.sort(ids);
            return ids;
        }

        /** Соседи лучшего набора: замена одного офиса. */
        private void searchNeighbors(Integer[] rank) {
            if (best.offices.isEmpty() || !searchTimeLeft()) {
                return;
            }
            int[] cur = bestIds();
            int nOff = inst.offices.length;
            boolean[] in = new boolean[nOff];
            for (int id : cur) {
                in[id] = true;
            }
            int bound = Math.min(nOff, Math.max(12, cur.length * 4));
            for (int r = 0; r < bound && searchTimeLeft(); r++) {
                int neu = rank[r];
                if (in[neu] || inst.offices[neu].maxSize < 1) {
                    continue;
                }
                for (int i = 0; i < cur.length && searchTimeLeft(); i++) {
                    int[] next = cur.clone();
                    next[i] = neu;
                    considerCheap(next);
                }
            }
        }

        /** Оставшееся CPU — дешёвые случайные наборы, не thorough. */
        private void searchUntilTime(Integer[] rank, int predicted, int maxK) {
            if (maxK < 2) {
                return;
            }
            int nOff = inst.offices.length;
            int miss = 0;
            String lastBest = Arrays.toString(bestIds());
            searchNeighbors(rank);
            while (searchTimeLeft()) {
                String nowBest = Arrays.toString(bestIds());
                if (!nowBest.equals(lastBest)) {
                    searchNeighbors(rank);
                    lastBest = nowBest;
                }
                int center = randInt(2) == 0 ? Math.max(2, predicted) : Math.max(2, best.offices.size());
                int k = Math.max(2, Math.min(maxK, center + randInt(3) - 1));
                int pref = Math.min(nOff, Math.max(k + 2, 8));
                int[] pick = randomSubset(rank, k, pref);
                if (pick == null) {
                    if (++miss > 64) {
                        return;
                    }
                    continue;
                }
                int before = searchedSets.size();
                considerCheap(pick);
                if (searchedSets.size() == before) {
                    if (++miss > 256) {
                        searchNeighbors(rank);
                        return;
                    }
                } else {
                    miss = 0;
                }
            }
        }

        private int[] randomSubset(Integer[] rank, int k, int pref) {
            int nOff = inst.offices.length;
            if (k <= 0 || k > nOff) {
                return null;
            }
            boolean[] used = new boolean[nOff];
            int[] pick = new int[k];
            int filled = 0;
            int guard = 0;
            while (filled < k && guard++ < nOff * 8) {
                int o = rank[randInt(Math.max(1, Math.min(pref, nOff)))];
                if (randInt(4) == 0) {
                    o = randInt(nOff);
                }
                if (!used[o] && inst.offices[o].maxSize >= 1) {
                    used[o] = true;
                    pick[filled++] = o;
                }
            }
            if (filled != k) {
                return null;
            }
            Arrays.sort(pick);
            return pick;
        }

        private int[] bestIds() {
            int[] ids = new int[best.offices.size()];
            for (int i = 0; i < ids.length; i++) {
                ids[i] = best.offices.get(i).officeId;
            }
            Arrays.sort(ids);
            return ids;
        }

        private void considerCheap(int[] ids) {
            if (!timeLeft()) {
                return;
            }
            int[] copy = ids.clone();
            Arrays.sort(copy);
            if (!searchedSets.add(Arrays.toString(copy))) {
                return;
            }
            consider(construct(copy, false));
        }

        private void consider(Solution sol) {
            if (sol == null || sol.offices.isEmpty()) {
                return;
            }
            int[] ids = idsOf(sol);
            rememberElite(ids, sol.objective);
            if (sol.objective <= best.objective + 1e-9) {
                return;
            }
            if (!Constraints.valid(inst, sol.offices)) {
                return;
            }
            best = sol;
        }

        private int[] idsOf(Solution sol) {
            int[] ids = new int[sol.offices.size()];
            for (int i = 0; i < ids.length; i++) {
                ids[i] = sol.offices.get(i).officeId;
            }
            Arrays.sort(ids);
            return ids;
        }

        private void rememberElite(int[] ids, double obj) {
            if (ids == null || ids.length == 0 || obj < -1e90) {
                return;
            }
            int[] copy = ids.clone();
            Arrays.sort(copy);
            int k = copy.length;
            if (k < bestObjByK.length && obj > bestObjByK[k] + 1e-9) {
                bestObjByK[k] = obj;
                bestIdsByK[k] = copy;
            }
            for (Elite e : elites) {
                if (Arrays.equals(e.ids, copy)) {
                    if (obj > e.obj) {
                        e.obj = obj;
                    }
                    return;
                }
            }
            elites.add(new Elite(copy, obj));
            elites.sort((a, b) -> Double.compare(b.obj, a.obj));
            while (elites.size() > ELITE_CAP) {
                elites.remove(elites.size() - 1);
            }
        }

        /**
         * Собирает решение для фиксированного набора офисов:
         * Вороной по евклидову расстоянию, несколько политик навыков,
         * добор чужих объектов с проверкой оболочек.
         */
        @SuppressWarnings("unchecked")
        Solution construct(int[] officeIds, boolean thorough) {
            if (!timeLeft()) {
                return Solution.empty();
            }
            List<Integer> unique = new ArrayList<>();
            boolean[] seen = new boolean[inst.offices.length];
            for (int o : officeIds) {
                if (o < 0 || o >= inst.offices.length || seen[o]) {
                    continue;
                }
                if (inst.offices[o].maxSize < 1) {
                    continue;
                }
                seen[o] = true;
                unique.add(o);
            }
            if (unique.isEmpty()) {
                return Solution.empty();
            }
            int k = unique.size();
            int[] oid = new int[k];
            for (int i = 0; i < k; i++) {
                oid[i] = unique.get(i);
            }

            Solution bestLocal = Solution.empty();
            List<List<Cand>[]> pools = new ArrayList<>();
            pools.add(clusterSites(oid, false));
            if (thorough) {
                pools.add(clusterSites(oid, true));
                pools.add(clusterCapped(oid, false));
                pools.add(clusterCapped(oid, true));
            }
            for (List<Cand>[] pool : pools) {
                if (!timeLeft()) {
                    break;
                }
                List<BitSet[]> policies = new ArrayList<>();
                policies.add(assignSkillsConcentrate(oid, pool));
                if (thorough) {
                    policies.add(assignSkillsByDemand(oid, pool));
                    policies.add(assignSkillsGreedy(oid, pool));
                    policies.add(assignSkillsRoundRobin(oid));
                }
                for (BitSet[] officeSkills : policies) {
                    if (!timeLeft()) {
                        break;
                    }
                    Solution sol = buildFromSkills(oid, pool, officeSkills, thorough);
                    if (sol.objective > bestLocal.objective) {
                        bestLocal = sol;
                    }
                    if (thorough) {
                        Solution alt = buildByValue(oid, officeSkills);
                        if (alt.objective > bestLocal.objective) {
                            bestLocal = alt;
                        }
                    }
                }
            }
            return bestLocal;
        }

        @SuppressWarnings("unchecked")
        private List<Cand>[] clusterSites(int[] oid, boolean road) {
            int k = oid.length;
            List<Cand>[] pool = new List[k];
            for (int t = 0; t < k; t++) {
                pool[t] = new ArrayList<>();
            }
            for (int s = 0; s < inst.sites.length; s++) {
                if (!inst.siteFeasible(s)) {
                    continue;
                }
                int bestT = 0;
                double bestKey = Double.POSITIVE_INFINITY;
                for (int t = 0; t < k; t++) {
                    double key = road
                            ? inst.distances[oid[t]][s]
                            : Geometry.euclid2(inst.sites[s].pos, inst.offices[oid[t]].pos);
                    if (key < bestKey - 1e-15
                            || (Math.abs(key - bestKey) <= 1e-15 && oid[t] < oid[bestT])) {
                        bestKey = key;
                        bestT = t;
                    }
                }
                double value = inst.sites[s].reward - inst.distances[oid[bestT]][s];
                pool[bestT].add(new Cand(s, value, inst.sites[s].tasks.length));
            }
            for (int t = 0; t < k; t++) {
                pool[t].sort((a, b) -> Double.compare(b.value, a.value));
            }
            return pool;
        }

        /**
         * Жадно кладём объект в офис с лучшим (reward−dist) среди тех,
         * у кого ещё есть ёмкость. Вороной без капа сваливает всё на один офис.
         */
        @SuppressWarnings("unchecked")
        private List<Cand>[] clusterCapped(int[] oid, boolean road) {
            int k = oid.length;
            List<Cand>[] pool = new List[k];
            int[] load = new int[k];
            for (int t = 0; t < k; t++) {
                pool[t] = new ArrayList<>();
            }
            Integer[] order = new Integer[inst.sites.length];
            int nFeas = 0;
            for (int s = 0; s < inst.sites.length; s++) {
                if (inst.siteFeasible(s)) {
                    order[nFeas++] = s;
                }
            }
            Arrays.sort(order, 0, nFeas, (a, b) -> {
                double va = bestRoadValue(oid, a);
                double vb = bestRoadValue(oid, b);
                return Double.compare(vb, va);
            });
            for (int i = 0; i < nFeas; i++) {
                int s = order[i];
                int bestT = -1;
                double bestV = -1e100;
                double bestKey = Double.POSITIVE_INFINITY;
                for (int t = 0; t < k; t++) {
                    if (load[t] >= inst.offices[oid[t]].maxSize) {
                        continue;
                    }
                    double v = inst.sites[s].reward - inst.distances[oid[t]][s];
                    double key = road
                            ? inst.distances[oid[t]][s]
                            : Geometry.euclid2(inst.sites[s].pos, inst.offices[oid[t]].pos);
                    if (v > bestV + 1e-15 || (Math.abs(v - bestV) <= 1e-15 && key < bestKey)) {
                        bestV = v;
                        bestKey = key;
                        bestT = t;
                    }
                }
                if (bestT >= 0) {
                    pool[bestT].add(new Cand(s, bestV, inst.sites[s].tasks.length));
                    load[bestT]++;
                }
            }
            for (int t = 0; t < k; t++) {
                pool[t].sort((a, b) -> Double.compare(b.value, a.value));
            }
            return pool;
        }

        private double bestRoadValue(int[] oid, int site) {
            double best = -1e100;
            for (int id : oid) {
                best = Math.max(best, inst.sites[site].reward - inst.distances[id][site]);
            }
            return best;
        }

        @SuppressWarnings("unchecked")
        private Solution buildFromSkills(int[] oid, List<Cand>[] pool, BitSet[] officeSkills,
                                         boolean thorough) {
            int k = oid.length;
            List<Cand>[] taken = new List[k];
            List<Cand>[] left = new List[k];
            for (int t = 0; t < k; t++) {
                taken[t] = new ArrayList<>();
                left[t] = new ArrayList<>();
                int cap = inst.offices[oid[t]].maxSize;
                for (Cand c : pool[t]) {
                    if (c.value > 0 && taken[t].size() < cap && coveredBy(c.site, officeSkills[t])) {
                        taken[t].add(c);
                    } else {
                        left[t].add(c);
                    }
                }
            }
            improve(oid, taken, left, officeSkills, thorough);
            if (thorough) {
                stealSites(oid, taken, officeSkills);
                recoverLeftovers(oid, taken, officeSkills);
                return polishSafe(oid, taken, officeSkills);
            }
            return finalize(oid, taken);
        }

        /**
         * Назначаем объект офису с максимальным (reward−dist) среди тех,
         * кто умеет его навыки. Геометрию чиним в finalize.
         */
        @SuppressWarnings("unchecked")
        private Solution buildByValue(int[] oid, BitSet[] skills) {
            int k = oid.length;
            List<Cand>[] taken = new List[k];
            List<Cand>[] left = new List[k];
            for (int t = 0; t < k; t++) {
                taken[t] = new ArrayList<>();
                left[t] = new ArrayList<>();
            }
            List<int[]> order = new ArrayList<>();
            for (int s = 0; s < inst.sites.length; s++) {
                if (!inst.siteFeasible(s)) {
                    continue;
                }
                for (int t = 0; t < k; t++) {
                    if (!coveredBy(s, skills[t])) {
                        continue;
                    }
                    double v = inst.sites[s].reward - inst.distances[oid[t]][s];
                    if (v > 0) {
                        order.add(new int[]{s, t});
                    }
                }
            }
            order.sort((a, b) -> {
                double va = inst.sites[a[0]].reward - inst.distances[oid[a[1]]][a[0]];
                double vb = inst.sites[b[0]].reward - inst.distances[oid[b[1]]][b[0]];
                return Double.compare(vb, va);
            });
            boolean[] used = new boolean[inst.sites.length];
            for (int[] rec : order) {
                int s = rec[0];
                int t = rec[1];
                if (used[s] || taken[t].size() >= inst.offices[oid[t]].maxSize) {
                    continue;
                }
                taken[t].add(new Cand(s, inst.sites[s].reward - inst.distances[oid[t]][s],
                        inst.sites[s].tasks.length));
                used[s] = true;
            }
            improve(oid, taken, left, skills, true);
            stealSites(oid, taken, skills);
            recoverLeftovers(oid, taken, skills);
            return polishSafe(oid, taken, skills);
        }

        /**
         * Polish может улучшить score(), но finalize потом чинит геометрию/баланс
         * и иногда теряет цель. Берём максимум до и после полировки.
         */
        @SuppressWarnings("unchecked")
        private Solution polishSafe(int[] oid, List<Cand>[] taken, BitSet[] skills) {
            List<Cand>[] before = new List[taken.length];
            for (int t = 0; t < taken.length; t++) {
                before[t] = new ArrayList<>(taken[t]);
            }
            polish(oid, taken, skills);
            recoverLeftovers(oid, taken, skills);
            Solution polished = finalize(oid, taken);
            Solution plain = finalize(oid, before);
            return polished.objective > plain.objective ? polished : plain;
        }

        /**
         * Навык отдаём офису, у которого больше непокрытых объектов пула, которым он нужен.
         */
        private BitSet[] assignSkillsByDemand(int[] oid, List<Cand>[] pool) {
            int k = oid.length;
            BitSet[] skills = new BitSet[k];
            for (int t = 0; t < k; t++) {
                skills[t] = new BitSet();
            }
            int[] used = new int[inst.skillBound];
            Integer[] skillsOrder = new Integer[inst.skillBound];
            int nSk = 0;
            for (int s = 0; s < inst.skillBound; s++) {
                if (inst.skillSupply[s] > 0) {
                    skillsOrder[nSk++] = s;
                }
            }
            Arrays.sort(skillsOrder, 0, nSk, Comparator.comparingInt(s -> inst.skillSupply[s]));
            for (int i = 0; i < nSk; i++) {
                int skill = skillsOrder[i];
                while (used[skill] < inst.skillSupply[skill]) {
                    int bestT = -1;
                    int bestNeed = 0;
                    for (int t = 0; t < k; t++) {
                        if (skills[t].get(skill)) {
                            continue;
                        }
                        int need = 0;
                        for (Cand c : pool[t]) {
                            if (c.value > 0 && inst.sites[c.site].skillSet.get(skill)
                                    && !coveredBy(c.site, skills[t])) {
                                need++;
                            }
                        }
                        if (need > bestNeed) {
                            bestNeed = need;
                            bestT = t;
                        }
                    }
                    if (bestT < 0) {
                        break;
                    }
                    skills[bestT].set(skill);
                    used[skill]++;
                }
            }
            return skills;
        }

        /**
         * Жадно раздаём навыки офисам: каждый следующий навык туда,
         * где он сильнее увеличивает сумму (reward−dist) покрываемых объектов ячейки.
         * Число офисов с навыком не превышает числа работников с этим навыком.
         */
        private BitSet[] assignSkillsGreedy(int[] oid, List<Cand>[] pool) {
            int k = oid.length;
            BitSet[] skills = new BitSet[k];
            for (int t = 0; t < k; t++) {
                skills[t] = new BitSet();
            }
            int[] used = new int[inst.skillBound];
            while (true) {
                double bestGain = 1e-9;
                int bestT = -1;
                int bestS = -1;
                for (int t = 0; t < k; t++) {
                    double cur = coverableValue(oid[t], pool[t], skills[t]);
                    for (int s = 0; s < inst.skillBound; s++) {
                        if (skills[t].get(s) || used[s] >= inst.skillSupply[s]) {
                            continue;
                        }
                        skills[t].set(s);
                        double gain = coverableValue(oid[t], pool[t], skills[t]) - cur;
                        skills[t].clear(s);
                        if (gain > bestGain) {
                            bestGain = gain;
                            bestT = t;
                            bestS = s;
                        }
                    }
                }
                if (bestT < 0) {
                    break;
                }
                skills[bestT].set(bestS);
                used[bestS]++;
            }
            return skills;
        }

        /**
         * Сначала лучшие офисы забирают по одному экземпляру каждого навыка,
         * остаток supply идёт следующим. Редкий навык остаётся у сильнейшего кластера.
         */
        private BitSet[] assignSkillsConcentrate(int[] oid, List<Cand>[] pool) {
            int k = oid.length;
            BitSet all = new BitSet();
            for (int s = 0; s < inst.skillBound; s++) {
                if (inst.skillSupply[s] > 0) {
                    all.set(s);
                }
            }
            Integer[] order = new Integer[k];
            double[] pot = new double[k];
            for (int t = 0; t < k; t++) {
                order[t] = t;
                pot[t] = coverableValue(oid[t], pool[t], all);
            }
            Arrays.sort(order, (a, b) -> Double.compare(pot[b], pot[a]));
            BitSet[] skills = new BitSet[k];
            for (int t = 0; t < k; t++) {
                skills[t] = new BitSet();
            }
            int[] used = new int[inst.skillBound];
            for (int t : order) {
                for (int s = 0; s < inst.skillBound; s++) {
                    if (used[s] < inst.skillSupply[s]) {
                        skills[t].set(s);
                        used[s]++;
                    }
                }
            }
            return skills;
        }

        /** Редкие навыки раздаём по кругу, чтобы ни один офис не остался пустым. */
        private BitSet[] assignSkillsRoundRobin(int[] oid) {
            int k = oid.length;
            BitSet[] skills = new BitSet[k];
            for (int t = 0; t < k; t++) {
                skills[t] = new BitSet();
            }
            Integer[] rare = new Integer[inst.skillBound];
            int nSk = 0;
            for (int s = 0; s < inst.skillBound; s++) {
                if (inst.skillSupply[s] > 0) {
                    rare[nSk++] = s;
                }
            }
            Arrays.sort(rare, 0, nSk, Comparator.comparingInt(s -> inst.skillSupply[s]));
            int cursor = 0;
            for (int i = 0; i < nSk; i++) {
                int skill = rare[i];
                for (int copy = 0; copy < inst.skillSupply[skill]; copy++) {
                    skills[cursor % k].set(skill);
                    cursor++;
                }
            }
            return skills;
        }

        /**
         * Объекты, которые ячейка Вороного не смогла взять из-за навыков,
         * пробуем отдать другому офису с подходящими навыками, если оболочка не конфликтует.
         */
        private void stealSites(int[] oid, List<Cand>[] taken, BitSet[] skills) {
            int nS = inst.sites.length;
            boolean[] used = new boolean[nS];
            for (List<Cand> group : taken) {
                for (Cand c : group) {
                    used[c.site] = true;
                }
            }
            List<int[]> cand = new ArrayList<>();
            for (int s = 0; s < nS; s++) {
                if (used[s] || !inst.siteFeasible(s)) {
                    continue;
                }
                for (int t = 0; t < oid.length; t++) {
                    if (taken[t].size() >= inst.offices[oid[t]].maxSize) {
                        continue;
                    }
                    if (!coveredBy(s, skills[t])) {
                        continue;
                    }
                    double value = inst.sites[s].reward - inst.distances[oid[t]][s];
                    cand.add(new int[]{s, t, Double.hashCode(value)});
                }
            }
            cand.sort((a, b) -> {
                double va = inst.sites[a[0]].reward - inst.distances[oid[a[1]]][a[0]];
                double vb = inst.sites[b[0]].reward - inst.distances[oid[b[1]]][b[0]];
                return Double.compare(vb, va);
            });
            List<Vec>[] hulls = currentHulls(oid, taken);
            boolean[][] covered = new boolean[oid.length][];
            for (int t = 0; t < oid.length; t++) {
                if (hulls[t] != null) {
                    covered[t] = coverMask(hulls[t]);
                }
            }
            int added = 0;
            int limit = Math.max(64, nS / 2);
            for (int[] rec : cand) {
                if (!timeLeft() || added >= limit) {
                    break;
                }
                int s = rec[0];
                int t = rec[1];
                if (used[s] || taken[t].size() >= inst.offices[oid[t]].maxSize) {
                    continue;
                }
                Cand c = new Cand(s, inst.sites[s].reward - inst.distances[oid[t]][s], inst.sites[s].tasks.length);
                double before = score(oid, taken);
                taken[t].add(c);
                if (!geometryAllows(oid, taken, t, hulls, covered)) {
                    taken[t].remove(taken[t].size() - 1);
                    continue;
                }
                double after = score(oid, taken);
                if (after <= before + 1e-9) {
                    taken[t].remove(taken[t].size() - 1);
                    continue;
                }
                used[s] = true;
                added++;
                hulls[t] = hullOf(oid[t], taken[t]);
                covered[t] = coverMask(hulls[t]);
            }
        }

        @SuppressWarnings("unchecked")
        private List<Vec>[] currentHulls(int[] oid, List<Cand>[] taken) {
            List<Vec>[] hulls = new List[oid.length];
            for (int t = 0; t < oid.length; t++) {
                if (!taken[t].isEmpty()) {
                    hulls[t] = hullOf(oid[t], taken[t]);
                }
            }
            return hulls;
        }

        private List<Vec> hullOf(int officeId, List<Cand> sites) {
            List<Vec> raw = new ArrayList<>(sites.size() + 1);
            raw.add(inst.offices[officeId].pos);
            for (Cand c : sites) {
                raw.add(inst.sites[c.site].pos);
            }
            return Geometry.convexHull(raw);
        }

        private boolean[] coverMask(List<Vec> hull) {
            int n = inst.sites.length + inst.offices.length;
            boolean[] cov = new boolean[n];
            if (hull == null || hull.isEmpty()) {
                return cov;
            }
            double minX = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY;
            double minY = Double.POSITIVE_INFINITY;
            double maxY = Double.NEGATIVE_INFINITY;
            for (Vec p : hull) {
                minX = Math.min(minX, p.x);
                maxX = Math.max(maxX, p.x);
                minY = Math.min(minY, p.y);
                maxY = Math.max(maxY, p.y);
            }
            double pad = 1e-9;
            for (int i = 0; i < inst.sites.length; i++) {
                Vec p = inst.sites[i].pos;
                if (p.x + pad < minX || p.x - pad > maxX || p.y + pad < minY || p.y - pad > maxY) {
                    continue;
                }
                cov[i] = Geometry.covers(hull, p);
            }
            for (int j = 0; j < inst.offices.length; j++) {
                Vec p = inst.offices[j].pos;
                if (p.x + pad < minX || p.x - pad > maxX || p.y + pad < minY || p.y - pad > maxY) {
                    continue;
                }
                cov[inst.sites.length + j] = Geometry.covers(hull, p);
            }
            return cov;
        }

        private Cand candOf(int officeId, int site) {
            return new Cand(site, inst.sites[site].reward - inst.distances[officeId][site],
                    inst.sites[site].tasks.length);
        }

        /**
         * Добор leftover только на thorough: прямой add и парный add против α.
         */
        private void recoverLeftovers(int[] oid, List<Cand>[] taken, BitSet[] skills) {
            int guard = 0;
            while (guard++ < RECOVER_GUARD && timeLeft()) {
                List<Integer> rest = leftoverSites(oid, taken);
                if (rest.isEmpty()) {
                    return;
                }
                if (admitDirect(oid, taken, skills, rest)
                        || admitPaired(oid, taken, skills, rest)) {
                    continue;
                }
                break;
            }
        }

        private List<Integer> leftoverSites(int[] oid, List<Cand>[] taken) {
            boolean[] used = new boolean[inst.sites.length];
            for (List<Cand> group : taken) {
                for (Cand c : group) {
                    used[c.site] = true;
                }
            }
            List<Integer> rest = new ArrayList<>();
            for (int s = 0; s < inst.sites.length; s++) {
                if (!used[s] && inst.siteFeasible(s)) {
                    rest.add(s);
                }
            }
            double[] val = new double[inst.sites.length];
            for (int s : rest) {
                double bestV = -1e100;
                for (int t = 0; t < oid.length; t++) {
                    if (taken[t].size() >= inst.offices[oid[t]].maxSize) {
                        continue;
                    }
                    bestV = Math.max(bestV, inst.sites[s].reward - inst.distances[oid[t]][s]);
                }
                val[s] = bestV;
            }
            rest.sort((a, b) -> Double.compare(val[b], val[a]));
            int cap = Math.min(rest.size(), LEFTOVER_RANK_CAP);
            return new ArrayList<>(rest.subList(0, cap));
        }

        private boolean admitDirect(int[] oid, List<Cand>[] taken, BitSet[] skills, List<Integer> rest) {
            List<Vec>[] hulls = currentHulls(oid, taken);
            boolean[][] covered = new boolean[oid.length][];
            for (int t = 0; t < oid.length; t++) {
                if (hulls[t] != null) {
                    covered[t] = coverMask(hulls[t]);
                }
            }
            double before = score(oid, taken);
            int bestT = -1;
            int bestS = -1;
            double bestSc = before;
            for (int t = 0; t < oid.length && timeLeft(); t++) {
                if (taken[t].size() >= inst.offices[oid[t]].maxSize) {
                    continue;
                }
                int tried = 0;
                for (int s : rest) {
                    if (!coveredBy(s, skills[t])) {
                        continue;
                    }
                    int directCap = inst.sites.length <= 80 ? rest.size() : DIRECT_TRY_CAP;
                    if (++tried > directCap) {
                        break;
                    }
                    taken[t].add(candOf(oid[t], s));
                    boolean geo = geometryAllows(oid, taken, t, hulls, covered);
                    double sc = geo ? score(oid, taken) : -1e100;
                    taken[t].remove(taken[t].size() - 1);
                    if (geo && sc > bestSc + 1e-9) {
                        bestSc = sc;
                        bestT = t;
                        bestS = s;
                    }
                }
            }
            if (bestT < 0) {
                return false;
            }
            taken[bestT].add(candOf(oid[bestT], bestS));
            return true;
        }

        private boolean admitPaired(int[] oid, List<Cand>[] taken, BitSet[] skills, List<Integer> rest) {
            if (oid.length < 2 || rest.size() < 2) {
                return false;
            }
            List<Vec>[] hulls = currentHulls(oid, taken);
            boolean[][] covered = new boolean[oid.length][];
            for (int t = 0; t < oid.length; t++) {
                if (hulls[t] != null) {
                    covered[t] = coverMask(hulls[t]);
                }
            }
            double before = score(oid, taken);
            int bestT1 = -1, bestT2 = -1, bestS1 = -1, bestS2 = -1;
            double bestSc = before;
            int nTry = Math.min(rest.size(), inst.sites.length <= 80 ? rest.size() : PAIR_TRY_CAP);
            for (int t1 = 0; t1 < oid.length && timeLeft(); t1++) {
                if (taken[t1].isEmpty() || taken[t1].size() >= inst.offices[oid[t1]].maxSize) {
                    continue;
                }
                for (int t2 = t1 + 1; t2 < oid.length; t2++) {
                    if (taken[t2].isEmpty() || taken[t2].size() >= inst.offices[oid[t2]].maxSize) {
                        continue;
                    }
                    for (int i = 0; i < nTry; i++) {
                        int s1 = rest.get(i);
                        if (!coveredBy(s1, skills[t1])) {
                            continue;
                        }
                        taken[t1].add(candOf(oid[t1], s1));
                        if (!geometryAllows(oid, taken, t1, hulls, covered)) {
                            taken[t1].remove(taken[t1].size() - 1);
                            continue;
                        }
                        List<Vec>[] hulls2 = currentHulls(oid, taken);
                        boolean[][] cov2 = new boolean[oid.length][];
                        for (int u = 0; u < oid.length; u++) {
                            if (hulls2[u] != null) {
                                cov2[u] = coverMask(hulls2[u]);
                            }
                        }
                        for (int j = 0; j < nTry; j++) {
                            int s2 = rest.get(j);
                            if (s2 == s1 || !coveredBy(s2, skills[t2])) {
                                continue;
                            }
                            taken[t2].add(candOf(oid[t2], s2));
                            boolean geo = geometryAllows(oid, taken, t2, hulls2, cov2);
                            double sc = geo ? score(oid, taken) : -1e100;
                            taken[t2].remove(taken[t2].size() - 1);
                            if (geo && sc > bestSc + 1e-9) {
                                bestSc = sc;
                                bestT1 = t1;
                                bestT2 = t2;
                                bestS1 = s1;
                                bestS2 = s2;
                            }
                        }
                        taken[t1].remove(taken[t1].size() - 1);
                    }
                }
            }
            if (bestT1 < 0) {
                return false;
            }
            taken[bestT1].add(candOf(oid[bestT1], bestS1));
            taken[bestT2].add(candOf(oid[bestT2], bestS2));
            return true;
        }

        /**
         * Локальный поиск назначения: закрыть слабый офис, выровнять |сайтов|,
         * перенести/обменять объекты. Геометрия и работники проверяются сразу.
         */
        private void polish(int[] oid, List<Cand>[] taken, BitSet[] skills) {
            int guard = 0;
            boolean changed = true;
            while (changed && guard++ < 80 && timeLeft()) {
                changed = tryCloseOffice(oid, taken)
                        || tryShiftToBalance(oid, taken, skills, true)
                        || relocateBest(oid, taken, skills)
                        || swapBest(oid, taken, skills);
            }
        }

        private boolean tryCloseOffice(int[] oid, List<Cand>[] taken) {
            if (liveCount(taken) < 2) {
                return false;
            }
            double cur = score(oid, taken);
            int bestT = -1;
            double bestSc = cur;
            for (int t = 0; t < oid.length && timeLeft(); t++) {
                if (taken[t].isEmpty()) {
                    continue;
                }
                List<Cand> saved = new ArrayList<>(taken[t]);
                taken[t].clear();
                boolean ok = assignmentGeometryOk(oid, taken) && workersFeasible(oid, taken);
                double sc = ok ? score(oid, taken) : -1e100;
                taken[t].addAll(saved);
                if (sc > bestSc + 1e-9) {
                    bestSc = sc;
                    bestT = t;
                }
            }
            if (bestT < 0) {
                return false;
            }
            taken[bestT].clear();
            return true;
        }

        private boolean tryShiftToBalance(int[] oid, List<Cand>[] taken, BitSet[] skills, boolean checkGeo) {
            int minI = -1;
            int maxI = -1;
            int minV = Integer.MAX_VALUE;
            int maxV = -1;
            for (int t = 0; t < taken.length; t++) {
                int sz = taken[t].size();
                if (sz == 0) {
                    continue;
                }
                if (sz < minV) {
                    minV = sz;
                    minI = t;
                }
                if (sz > maxV) {
                    maxV = sz;
                    maxI = t;
                }
            }
            if (minI < 0 || maxI < 0 || minI == maxI || maxV - minV <= inst.maxDifference) {
                return false;
            }
            if (taken[minI].size() >= inst.offices[oid[minI]].maxSize) {
                return false;
            }
            double cur = score(oid, taken);
            int bestJ = -1;
            double bestSc = cur;
            for (int j = 0; j < taken[maxI].size() && timeLeft(); j++) {
                Cand c = taken[maxI].get(j);
                if (skills != null && !coveredBy(c.site, skills[minI])) {
                    continue;
                }
                taken[maxI].remove(j);
                taken[minI].add(candOf(oid[minI], c.site));
                boolean ok = true;
                if (checkGeo) {
                    ok = assignmentGeometryOk(oid, taken) && workersFeasible(oid, taken);
                }
                double sc = ok ? score(oid, taken) : -1e100;
                taken[minI].remove(taken[minI].size() - 1);
                taken[maxI].add(j, c);
                if (sc > bestSc + 1e-9) {
                    bestSc = sc;
                    bestJ = j;
                }
            }
            if (bestJ < 0) {
                return false;
            }
            Cand c = taken[maxI].remove(bestJ);
            taken[minI].add(candOf(oid[minI], c.site));
            return true;
        }

        private boolean relocateBest(int[] oid, List<Cand>[] taken, BitSet[] skills) {
            double cur = score(oid, taken);
            int bestFrom = -1;
            int bestIdx = -1;
            int bestTo = -1;
            double bestSc = cur;
            int perOffice = inst.sites.length <= 80 ? Integer.MAX_VALUE : 10;
            for (int t = 0; t < oid.length && timeLeft(); t++) {
                int nTry = Math.min(taken[t].size(), perOffice);
                for (int i = 0; i < nTry; i++) {
                    Cand c = taken[t].get(i);
                    for (int u = 0; u < oid.length; u++) {
                        if (u == t || taken[u].size() >= inst.offices[oid[u]].maxSize) {
                            continue;
                        }
                        if (!coveredBy(c.site, skills[u])) {
                            continue;
                        }
                        taken[t].remove(i);
                        taken[u].add(candOf(oid[u], c.site));
                        boolean ok = assignmentGeometryOk(oid, taken) && workersFeasible(oid, taken);
                        double sc = ok ? score(oid, taken) : -1e100;
                        taken[u].remove(taken[u].size() - 1);
                        taken[t].add(i, c);
                        if (sc > bestSc + 1e-9) {
                            bestSc = sc;
                            bestFrom = t;
                            bestIdx = i;
                            bestTo = u;
                        }
                    }
                }
            }
            if (bestFrom < 0) {
                return false;
            }
            Cand c = taken[bestFrom].remove(bestIdx);
            taken[bestTo].add(candOf(oid[bestTo], c.site));
            return true;
        }

        private boolean swapBest(int[] oid, List<Cand>[] taken, BitSet[] skills) {
            if (oid.length < 2 || inst.sites.length > 90) {
                return false;
            }
            double cur = score(oid, taken);
            int bestT = -1, bestI = -1, bestU = -1, bestJ = -1;
            double bestSc = cur;
            for (int t = 0; t < oid.length && timeLeft(); t++) {
                for (int u = t + 1; u < oid.length; u++) {
                    if (taken[t].isEmpty() || taken[u].isEmpty()) {
                        continue;
                    }
                    for (int i = 0; i < taken[t].size(); i++) {
                        Cand a = taken[t].get(i);
                        if (!coveredBy(a.site, skills[u])) {
                            continue;
                        }
                        for (int j = 0; j < taken[u].size(); j++) {
                            Cand b = taken[u].get(j);
                            if (!coveredBy(b.site, skills[t])) {
                                continue;
                            }
                            taken[t].set(i, candOf(oid[t], b.site));
                            taken[u].set(j, candOf(oid[u], a.site));
                            boolean ok = assignmentGeometryOk(oid, taken) && workersFeasible(oid, taken);
                            double sc = ok ? score(oid, taken) : -1e100;
                            taken[t].set(i, a);
                            taken[u].set(j, b);
                            if (sc > bestSc + 1e-9) {
                                bestSc = sc;
                                bestT = t;
                                bestI = i;
                                bestU = u;
                                bestJ = j;
                            }
                        }
                    }
                }
            }
            if (bestT < 0) {
                return false;
            }
            Cand a = taken[bestT].get(bestI);
            Cand b = taken[bestU].get(bestJ);
            taken[bestT].set(bestI, candOf(oid[bestT], b.site));
            taken[bestU].set(bestJ, candOf(oid[bestU], a.site));
            return true;
        }

        private boolean assignmentGeometryOk(int[] oid, List<Cand>[] taken) {
            List<Vec>[] hulls = currentHulls(oid, taken);
            boolean[][] covered = new boolean[oid.length][];
            for (int t = 0; t < oid.length; t++) {
                if (hulls[t] != null) {
                    covered[t] = coverMask(hulls[t]);
                }
            }
            int n = inst.sites.length + inst.offices.length;
            for (int i = 0; i < oid.length; i++) {
                if (covered[i] == null) {
                    continue;
                }
                for (int j = i + 1; j < oid.length; j++) {
                    if (covered[j] == null) {
                        continue;
                    }
                    for (int p = 0; p < n; p++) {
                        if (covered[i][p] && covered[j][p]) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }

        private boolean workersFeasible(int[] oid, List<Cand>[] taken) {
            List<BitSet> needs = new ArrayList<>();
            for (List<Cand> group : taken) {
                if (group.isEmpty()) {
                    continue;
                }
                List<Integer> sites = new ArrayList<>(group.size());
                for (Cand c : group) {
                    sites.add(c.site);
                }
                needs.add(skillsOf(sites));
            }
            if (needs.isEmpty()) {
                return false;
            }
            return allocateWorkers(needs.toArray(BitSet[]::new)) != null;
        }

        private boolean geometryAllows(int[] oid, List<Cand>[] taken, int t,
                                       List<Vec>[] hulls, boolean[][] covered) {
            List<Vec> hull = hullOf(oid[t], taken[t]);
            boolean[] cov = coverMask(hull);
            int n = inst.sites.length + inst.offices.length;
            for (int u = 0; u < oid.length; u++) {
                if (u == t || covered[u] == null) {
                    continue;
                }
                for (int p = 0; p < n; p++) {
                    if (cov[p] && covered[u][p]) {
                        return false;
                    }
                }
            }
            return true;
        }

        private double coverableValue(int officeId, List<Cand> pool, BitSet skills) {
            double sum = 0;
            int cap = inst.offices[officeId].maxSize;
            int taken = 0;
            for (Cand c : pool) {
                if (c.value <= 0 || !coveredBy(c.site, skills)) {
                    continue;
                }
                sum += c.value;
                taken++;
                if (taken >= cap) {
                    break;
                }
            }
            return sum;
        }

        private boolean coveredBy(int siteId, BitSet skills) {
            BitSet need = inst.sites[siteId].skillSet;
            for (int s = need.nextSetBit(0); s >= 0; s = need.nextSetBit(s + 1)) {
                if (!skills.get(s)) {
                    return false;
                }
            }
            return true;
        }

        @SuppressWarnings("unchecked")
        private void improve(int[] oid, List<Cand>[] taken, List<Cand>[] left,
                             BitSet[] officeSkills, boolean thorough) {
            if (!thorough) {
                improveOnce(oid, taken, left, officeSkills, 0);
                return;
            }
            List<Cand>[] startTaken = copyGroups(taken);
            List<Cand>[] startLeft = copyGroups(left);
            double bestSc = -1e300;
            List<Cand>[] bestTaken = copyGroups(taken);
            List<Cand>[] bestLeft = copyGroups(left);
            // 0 — старый порядок (trim→add→drop), 1 — сдвиг до trim, 2 — сдвиг после trim.
            for (int mode = 0; mode <= 2; mode++) {
                restoreGroups(taken, startTaken);
                restoreGroups(left, startLeft);
                improveOnce(oid, taken, left, officeSkills, mode);
                double sc = score(oid, taken);
                if (sc > bestSc + 1e-9) {
                    bestSc = sc;
                    bestTaken = copyGroups(taken);
                    bestLeft = copyGroups(left);
                }
            }
            restoreGroups(taken, bestTaken);
            restoreGroups(left, bestLeft);
        }

        private void improveOnce(int[] oid, List<Cand>[] taken, List<Cand>[] left,
                                 BitSet[] officeSkills, int mode) {
            int guard = 0;
            boolean changed = true;
            while (changed && guard++ < 4000 && timeLeft()) {
                changed = false;
                if (mode == 0) {
                    if (trimSiteCounts(oid, taken, left)) {
                        changed = true;
                        continue;
                    }
                    if (tryBestAdd(oid, taken, left, officeSkills, 12)) {
                        changed = true;
                        continue;
                    }
                    if (tryBestDrop(oid, taken, left)) {
                        changed = true;
                    }
                    continue;
                }
                int addCap = inst.sites.length <= 80 ? Integer.MAX_VALUE : 12;
                if (tryBestAdd(oid, taken, left, officeSkills, addCap)) {
                    changed = true;
                    continue;
                }
                if (mode == 1 && tryShiftToBalance(oid, taken, officeSkills, false)) {
                    changed = true;
                    continue;
                }
                if (trimSiteCounts(oid, taken, left)) {
                    changed = true;
                    continue;
                }
                if (mode == 2 && tryShiftToBalance(oid, taken, officeSkills, false)) {
                    changed = true;
                    continue;
                }
                if (tryBestDrop(oid, taken, left)) {
                    changed = true;
                }
            }
            trimSiteCounts(oid, taken, left);
        }

        @SuppressWarnings("unchecked")
        private List<Cand>[] copyGroups(List<Cand>[] src) {
            List<Cand>[] copy = new List[src.length];
            for (int i = 0; i < src.length; i++) {
                copy[i] = new ArrayList<>(src[i]);
            }
            return copy;
        }

        private void restoreGroups(List<Cand>[] dst, List<Cand>[] src) {
            for (int i = 0; i < dst.length; i++) {
                dst[i].clear();
                dst[i].addAll(src[i]);
            }
        }

        private boolean trimSiteCounts(int[] oid, List<Cand>[] taken, List<Cand>[] left) {
            boolean any = false;
            int guard = 0;
            while (guard++ < inst.sites.length + 2) {
                int minI = -1;
                int maxI = -1;
                int minV = Integer.MAX_VALUE;
                int maxV = -1;
                int opened = 0;
                for (int t = 0; t < taken.length; t++) {
                    int sz = taken[t].size();
                    if (sz == 0) {
                        continue;
                    }
                    opened++;
                    if (sz < minV) {
                        minV = sz;
                        minI = t;
                    }
                    if (sz > maxV) {
                        maxV = sz;
                        maxI = t;
                    }
                }
                if (opened < 2 || maxV - minV <= inst.maxDifference) {
                    return any;
                }
                int dropAt = worstIndex(taken[maxI]);
                left[maxI].add(taken[maxI].remove(dropAt));
                any = true;
            }
            return any;
        }

        /** Добавляем объект, если прирост цели положительный (в т.ч. ради баланса задач). */
        private boolean tryBestAdd(int[] oid, List<Cand>[] taken, List<Cand>[] left,
                                   BitSet[] officeSkills, int addCap) {
            double cur = score(oid, taken);
            int bestT = -1;
            int bestJ = -1;
            double bestSc = cur;
            for (int t = 0; t < taken.length; t++) {
                int cap = inst.offices[oid[t]].maxSize;
                if (taken[t].size() >= cap || left[t].isEmpty()) {
                    continue;
                }
                indexByGain(left[t]);
                int tried = 0;
                for (int j = 0; j < left[t].size() && tried < addCap; j++) {
                    Cand c = left[t].get(j);
                    if (!coveredBy(c.site, officeSkills[t])) {
                        continue;
                    }
                    tried++;
                    taken[t].add(c);
                    double sc = score(oid, taken);
                    taken[t].remove(taken[t].size() - 1);
                    if (sc > bestSc + 1e-9) {
                        bestSc = sc;
                        bestT = t;
                        bestJ = j;
                    }
                }
            }
            if (bestT < 0) {
                return false;
            }
            taken[bestT].add(left[bestT].remove(bestJ));
            return true;
        }

        /** Снимаем объект с перегруженного по задачам офиса, если это увеличивает цель. */
        private boolean tryBestDrop(int[] oid, List<Cand>[] taken, List<Cand>[] left) {
            int maxT = maxTaskCluster(taken);
            if (maxT < 0 || (taken[maxT].size() <= 1 && liveCount(taken) == 1)) {
                return false;
            }
            double cur = score(oid, taken);
            int bestJ = -1;
            double bestSc = cur;
            Integer[] order = new Integer[taken[maxT].size()];
            for (int i = 0; i < order.length; i++) {
                order[i] = i;
            }
            Arrays.sort(order, Comparator.comparingDouble(j -> taken[maxT].get(j).value));
            int limit = Math.min(order.length, 16);
            for (int pos = 0; pos < limit; pos++) {
                int j = order[pos];
                Cand c = taken[maxT].remove(j);
                double sc = score(oid, taken);
                taken[maxT].add(j, c);
                if (sc > bestSc + 1e-9) {
                    bestSc = sc;
                    bestJ = j;
                }
            }
            if (bestJ < 0) {
                return false;
            }
            left[maxT].add(taken[maxT].remove(bestJ));
            return true;
        }

        private void indexByGain(List<Cand> list) {
            double alpha = inst.balanceCoefficient;
            list.sort((a, b) -> Double.compare(b.value + alpha * b.tasks, a.value + alpha * a.tasks));
        }

        private int worstIndex(List<Cand> list) {
            int w = 0;
            double worst = Double.POSITIVE_INFINITY;
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).value < worst) {
                    worst = list.get(i).value;
                    w = i;
                }
            }
            return w;
        }

        private int liveCount(List<Cand>[] taken) {
            int n = 0;
            for (List<Cand> t : taken) {
                if (!t.isEmpty()) {
                    n++;
                }
            }
            return n;
        }

        private int maxTaskCluster(List<Cand>[] taken) {
            int best = -1;
            int bestLoad = -1;
            for (int t = 0; t < taken.length; t++) {
                if (taken[t].isEmpty()) {
                    continue;
                }
                int load = 0;
                for (Cand c : taken[t]) {
                    load += c.tasks;
                }
                if (load > bestLoad) {
                    bestLoad = load;
                    best = t;
                }
            }
            return best;
        }

        private double score(int[] oid, List<Cand>[] taken) {
            int opened = 0;
            int minS = Integer.MAX_VALUE;
            int maxS = 0;
            int minT = Integer.MAX_VALUE;
            int maxT = 0;
            double reward = 0;
            double dist = 0;
            double cost = 0;
            for (int t = 0; t < taken.length; t++) {
                if (taken[t].isEmpty()) {
                    continue;
                }
                opened++;
                cost += inst.offices[oid[t]].cost;
                int load = 0;
                for (Cand c : taken[t]) {
                    reward += inst.sites[c.site].reward;
                    dist += inst.distances[oid[t]][c.site];
                    load += c.tasks;
                }
                int sz = taken[t].size();
                minS = Math.min(minS, sz);
                maxS = Math.max(maxS, sz);
                minT = Math.min(minT, load);
                maxT = Math.max(maxT, load);
            }
            if (opened == 0) {
                return -1e100;
            }
            if (maxS - minS > inst.maxDifference) {
                return -1e100;
            }
            return reward - dist - cost - inst.balanceCoefficient * (maxT - minT);
        }

        private Solution finalize(int[] oid, List<Cand>[] taken) {
            List<Integer> usedOff = new ArrayList<>();
            List<List<Integer>> groups = new ArrayList<>();
            List<BitSet> needs = new ArrayList<>();
            for (int t = 0; t < oid.length; t++) {
                if (taken[t].isEmpty()) {
                    continue;
                }
                List<Integer> sites = new ArrayList<>();
                for (Cand c : taken[t]) {
                    sites.add(c.site);
                }
                usedOff.add(oid[t]);
                groups.add(sites);
                needs.add(skillsOf(sites));
            }
            if (usedOff.isEmpty()) {
                return Solution.empty();
            }

            List<Integer>[] workers = allocateWorkers(needs.toArray(BitSet[]::new));
            if (workers == null) {
                for (int i = 0; i < groups.size(); i++) {
                    groups.set(i, dropUntilCoverable(groups, i));
                }
                List<Integer> off2 = new ArrayList<>();
                List<List<Integer>> gr2 = new ArrayList<>();
                List<BitSet> n2 = new ArrayList<>();
                for (int i = 0; i < groups.size(); i++) {
                    if (groups.get(i).isEmpty()) {
                        continue;
                    }
                    off2.add(usedOff.get(i));
                    gr2.add(groups.get(i));
                    n2.add(skillsOf(groups.get(i)));
                }
                usedOff = off2;
                groups = gr2;
                needs = n2;
                if (usedOff.isEmpty()) {
                    return Solution.empty();
                }
                workers = allocateWorkers(needs.toArray(BitSet[]::new));
                if (workers == null) {
                    return Solution.empty();
                }
            }

            enforceSiteBalance(usedOff, groups);
            if (usedOff.isEmpty()) {
                return Solution.empty();
            }
            needs.clear();
            for (List<Integer> g : groups) {
                needs.add(skillsOf(g));
            }
            workers = allocateWorkers(needs.toArray(BitSet[]::new));
            if (workers == null) {
                return Solution.empty();
            }

            List<OpenedOffice> opened = new ArrayList<>();
            for (int i = 0; i < usedOff.size(); i++) {
                opened.add(new OpenedOffice(usedOff.get(i), groups.get(i), workers[i]));
            }
            if (opened.size() > 1 && !Constraints.valid(inst, opened)) {
                opened = repairGeometry(opened);
                if (opened.isEmpty() || !Constraints.valid(inst, opened)) {
                    return Solution.empty();
                }
            }
            return pack(opened);
        }

        private void enforceSiteBalance(List<Integer> officeIds, List<List<Integer>> groups) {
            int guard = 0;
            while (guard++ < inst.sites.length + 2 && !groups.isEmpty()) {
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

        /**
         * Если оболочки всё же пересеклись (численность / ничья Вороного),
         * снимаем самый дальний объект у большего кластера.
         */
        private List<OpenedOffice> repairGeometry(List<OpenedOffice> opened) {
            int guard = 0;
            while (guard++ < inst.sites.length && !opened.isEmpty() && !Constraints.valid(inst, opened)) {
                int maxI = 0;
                for (int i = 1; i < opened.size(); i++) {
                    if (opened.get(i).sites.size() > opened.get(maxI).sites.size()) {
                        maxI = i;
                    }
                }
                OpenedOffice o = opened.get(maxI);
                if (o.sites.isEmpty()) {
                    opened.remove(maxI);
                    continue;
                }
                int far = 0;
                double farD = -1;
                for (int j = 0; j < o.sites.size(); j++) {
                    double d = Geometry.euclid2(inst.sites[o.sites.get(j)].pos, inst.offices[o.officeId].pos);
                    if (d > farD) {
                        farD = d;
                        far = j;
                    }
                }
                List<Integer> nextSites = new ArrayList<>(o.sites);
                nextSites.remove(far);
                if (nextSites.isEmpty()) {
                    opened.remove(maxI);
                    continue;
                }
                BitSet need = skillsOf(nextSites);
                List<Integer>[] w = allocateWorkers(new BitSet[]{need});
                if (w == null) {
                    opened.remove(maxI);
                    continue;
                }
                opened.set(maxI, new OpenedOffice(o.officeId, nextSites, w[0]));
                List<BitSet> needs = new ArrayList<>();
                for (OpenedOffice x : opened) {
                    needs.add(skillsOf(x.sites));
                }
                List<Integer>[] allW = allocateWorkers(needs.toArray(BitSet[]::new));
                if (allW == null) {
                    opened.remove(maxI);
                    continue;
                }
                List<OpenedOffice> rebuilt = new ArrayList<>();
                for (int i = 0; i < opened.size(); i++) {
                    rebuilt.add(new OpenedOffice(opened.get(i).officeId, opened.get(i).sites, allW[i]));
                }
                opened = rebuilt;
            }
            return opened;
        }

        private List<Integer> dropUntilCoverable(List<List<Integer>> groups, int idx) {
            List<Integer> rest = new ArrayList<>(groups.get(idx));
            while (!rest.isEmpty()) {
                BitSet[] needs = new BitSet[groups.size()];
                for (int i = 0; i < groups.size(); i++) {
                    needs[i] = i == idx ? skillsOf(rest) : skillsOf(groups.get(i));
                }
                if (allocateWorkers(needs) != null) {
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

        @SuppressWarnings("unchecked")
        private List<Integer>[] allocateWorkers(BitSet[] needs) {
            boolean[] used = new boolean[inst.workers.length];
            List<Integer>[] perOffice = new List[needs.length];
            for (int i = 0; i < needs.length; i++) {
                perOffice[i] = new ArrayList<>();
                BitSet remain = (BitSet) needs[i].clone();
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
            if (opened.isEmpty()) {
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
            double obj = reward - distance - cost - inst.balanceCoefficient * (maxTasks - minTasks);
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
