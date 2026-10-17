//package org.example.hackatones.TechArena2026;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Локальный набор из 1000 синтетических тестов TechArena 2026.
 * Карты строятся здесь с нуля (не копируются из data/instance*).
 * Для каждого теста перебираются все подмножества офисов через
 * {@link Main.Solver#construct} (cheap и thorough) и записывается лучший ответ.
 */
public class LocalSuite {

    /** Сколько задач положить в data/local_tests. */
    private static final int TEST_COUNT = 1000;
    /** Соль ГПСЧ, чтобы набор воспроизводился. */
    private static final long FACTORY_SALT = 20261017L;

    public static void main(String[] args) throws Exception {
        int count = args.length > 0 ? Integer.parseInt(args[0]) : TEST_COUNT;
        Path root = Path.of(args.length > 1
                ? args[1]
                : "src/main/java/org/example/hackatones/TechArena2026/data/local_tests");
        Files.createDirectories(root);

        InstanceFactory factory = new InstanceFactory(FACTORY_SALT);
        StringBuilder scores = new StringBuilder();
        scores.append("index\tfamily\tnSites\tnOff\tnWorkers\tobjective\n");

        int nonempty = 0;
        double sumObj = 0.0;
        for (int i = 0; i < count; i++) {
            Path dir = root.resolve(String.format(Locale.ROOT, "t%04d", i));
            Files.createDirectories(dir);
            Path instancePath = dir.resolve("instance.json");
            Path solutionPath = dir.resolve("solution.json");

            DraftWorld world = factory.makeWorld(i);
            String json = factory.toJson(world);
            Files.writeString(instancePath, json, StandardCharsets.UTF_8);

            @SuppressWarnings("unchecked")
            Map<String, Object> raw = (Map<String, Object>) Main.Json.parse(json);
            Main.Instance instance = Main.Instance.fromJson(raw);
            Main.Solution solution = new LocalOracle(instance).best();
            Files.writeString(solutionPath, solution.toJson(), StandardCharsets.UTF_8);

            if (!solution.offices.isEmpty()) {
                nonempty++;
            }
            sumObj += solution.objective;
            scores.append(i).append('\t')
                    .append(world.family.name()).append('\t')
                    .append(world.sites.size()).append('\t')
                    .append(world.offices.size()).append('\t')
                    .append(world.workers.size()).append('\t')
                    .append(String.format(Locale.ROOT, "%.12f", solution.objective))
                    .append('\n');

            if ((i + 1) % 50 == 0 || i == 0) {
                System.out.printf(Locale.ROOT,
                        "local_tests %d/%d  obj=%.4f  nonempty=%d  %s%n",
                        i + 1, count, solution.objective, nonempty, dir.getFileName());
            }
        }
        Files.writeString(root.resolve("scores.tsv"), scores.toString(), StandardCharsets.UTF_8);
        System.out.printf(Locale.ROOT, "done: %d tests, nonempty=%d, meanObj=%.4f, root=%s%n",
                count, nonempty, sumObj / Math.max(1, count), root.toAbsolutePath());
    }

    /**
     * Перебор всех непустых наборов офисов (nOff ≤ 6 → до 63 наборов).
     * Для каждого — cheap и thorough construct, оставляем максимум целевой.
     */
    static final class LocalOracle {
        private final Main.Instance inst;
        private final Main.Solver solver;

        LocalOracle(Main.Instance inst) {
            this.inst = inst;
            this.solver = new Main.Solver(inst, System.nanoTime() + 180_000_000_000L);
        }

        Main.Solution best() {
            Main.Solution best = Main.Solution.empty();
            int nOff = inst.offices.length;
            if (nOff == 0 || inst.sites.length == 0) {
                return best;
            }
            int limit = 1 << nOff;
            for (int mask = 1; mask < limit; mask++) {
                int k = Integer.bitCount(mask);
                if (k > inst.workers.length) {
                    continue;
                }
                int[] ids = new int[k];
                int p = 0;
                for (int o = 0; o < nOff; o++) {
                    if ((mask & (1 << o)) != 0) {
                        ids[p++] = o;
                    }
                }
                best = keep(best, solver.construct(ids, false));
                best = keep(best, solver.construct(ids, true));
            }
            return best;
        }

        private Main.Solution keep(Main.Solution cur, Main.Solution cand) {
            if (cand == null || cand.offices.isEmpty()) {
                return cur;
            }
            if (cand.objective <= cur.objective + 1e-12) {
                return cur;
            }
            if (!Main.Constraints.valid(inst, cand.offices)) {
                return cur;
            }
            return cand;
        }
    }

    /** Семейства карт: α, навыки, геометрия, ёмкости. */
    enum Family {
        BALANCED,
        HIGH_ALPHA,
        ZERO_ALPHA,
        SCARCE_SKILL,
        MISSING_SKILL,
        TIGHT_CAP,
        WIDE_BALANCE,
        FAR_OFFICES,
        SITE_CLUSTERS,
        COSTLY_OPEN
    }

    static final class Vec2 {
        final double x;
        final double y;

        Vec2(double x, double y) {
            this.x = x;
            this.y = y;
        }

        double dist(Vec2 o) {
            return Math.hypot(x - o.x, y - o.y);
        }
    }

    static final class DraftSite {
        final Vec2 pos;
        final double reward;
        final int[] tasks;

        DraftSite(Vec2 pos, double reward, int[] tasks) {
            this.pos = pos;
            this.reward = reward;
            this.tasks = tasks;
        }
    }

    static final class DraftOffice {
        final Vec2 pos;
        final double cost;
        final int maxSize;

        DraftOffice(Vec2 pos, double cost, int maxSize) {
            this.pos = pos;
            this.cost = cost;
            this.maxSize = maxSize;
        }
    }

    static final class DraftWorld {
        final Family family;
        final List<DraftSite> sites = new ArrayList<>();
        final List<DraftOffice> offices = new ArrayList<>();
        final List<int[]> workers = new ArrayList<>();
        final double[][] distances;
        final int maxDifference;
        final double alpha;

        DraftWorld(Family family, int nOff, int nSites, int maxDifference, double alpha) {
            this.family = family;
            this.distances = new double[nOff][nSites];
            this.maxDifference = maxDifference;
            this.alpha = alpha;
        }
    }

    /** Генератор инстансов: детерминированный seed от номера теста. */
    static final class InstanceFactory {
        private final long salt;

        InstanceFactory(long salt) {
            this.salt = salt;
        }

        DraftWorld makeWorld(int index) {
            Rng rng = new Rng(salt ^ (index * 0x9E3779B97F4A7C15L) ^ (index + 1L) * 0xD1B54A32D192ED03L);
            Family family = Family.values()[index % Family.values().length];
            return build(rng, family, index);
        }

        String toJson(DraftWorld w) {
            StringBuilder sb = new StringBuilder();
            sb.append("{\n  \"sites\": [\n");
            for (int i = 0; i < w.sites.size(); i++) {
                DraftSite s = w.sites.get(i);
                sb.append("    {\"x\": ").append(num(s.pos.x))
                        .append(", \"y\": ").append(num(s.pos.y))
                        .append(", \"reward\": ").append(num(s.reward))
                        .append(", \"tasks\": ").append(ints(s.tasks)).append("}");
                sb.append(i + 1 < w.sites.size() ? ",\n" : "\n");
            }
            sb.append("  ],\n  \"offices\": [\n");
            for (int i = 0; i < w.offices.size(); i++) {
                DraftOffice o = w.offices.get(i);
                sb.append("    {\"x\": ").append(num(o.pos.x))
                        .append(", \"y\": ").append(num(o.pos.y))
                        .append(", \"cost\": ").append(num(o.cost))
                        .append(", \"maxSize\": ").append(o.maxSize).append("}");
                sb.append(i + 1 < w.offices.size() ? ",\n" : "\n");
            }
            sb.append("  ],\n  \"workers\": [\n");
            for (int i = 0; i < w.workers.size(); i++) {
                sb.append("    ").append(ints(w.workers.get(i)));
                sb.append(i + 1 < w.workers.size() ? ",\n" : "\n");
            }
            sb.append("  ],\n  \"distances\": [\n");
            for (int o = 0; o < w.offices.size(); o++) {
                sb.append("    [");
                for (int s = 0; s < w.sites.size(); s++) {
                    if (s > 0) {
                        sb.append(", ");
                    }
                    sb.append(num(w.distances[o][s]));
                }
                sb.append(o + 1 < w.offices.size() ? "],\n" : "]\n");
            }
            sb.append("  ],\n  \"maxDifference\": ").append(w.maxDifference)
                    .append(",\n  \"balanceCoefficient\": ").append(num(w.alpha))
                    .append("\n}\n");
            return sb.toString();
        }

        private DraftWorld build(Rng rng, Family family, int index) {
            SizeTier tier = SizeTier.of(index);
            int nSites = tier.minSites + rng.nextInt(tier.spanSites);
            int nOff = tier.minOff + rng.nextInt(tier.spanOff);
            int nSkills = 2 + rng.nextInt(3);
            if (family == Family.MISSING_SKILL) {
                nSkills = Math.max(3, nSkills);
            }

            double box = 8.0 + rng.nextDouble() * 12.0;
            double meanReward = 12.0 + rng.nextDouble() * 18.0;
            double alpha = switch (family) {
                case HIGH_ALPHA -> meanReward * (4.0 + rng.nextDouble() * 10.0);
                case ZERO_ALPHA -> 0.0;
                default -> rng.nextDouble() * meanReward * 0.25;
            };
            int maxDifference = switch (family) {
                case TIGHT_CAP -> 1 + rng.nextInt(2);
                case WIDE_BALANCE -> nSites;
                default -> Math.max(2, nSites / 4 + rng.nextInt(Math.max(2, nSites / 5)));
            };

            DraftWorld world = new DraftWorld(family, nOff, nSites, maxDifference, alpha);

            List<Vec2> hubs = new ArrayList<>();
            int hubCount = family == Family.SITE_CLUSTERS
                    ? Math.min(nOff, 2 + rng.nextInt(2))
                    : 1 + (rng.nextInt(3) == 0 ? 1 : 0);
            placeHubs(hubs, hubCount, box, rng);

            BitSet usedSkills = new BitSet();
            for (int s = 0; s < nSites; s++) {
                Vec2 hub = hubs.get(rng.nextInt(hubs.size()));
                double spread = family == Family.SITE_CLUSTERS ? box * 0.12 : box * 0.35;
                Vec2 pos = new Vec2(
                        clamp(hub.x + (rng.nextDouble() - 0.5) * spread, 0, box),
                        clamp(hub.y + (rng.nextDouble() - 0.5) * spread, 0, box));
                int taskCount = 1 + rng.nextInt(2);
                int[] tasks = new int[taskCount];
                for (int t = 0; t < taskCount; t++) {
                    int skill = rng.nextInt(nSkills);
                    if (family == Family.MISSING_SKILL && skill == 0) {
                        skill = 1 + rng.nextInt(nSkills - 1);
                    }
                    tasks[t] = skill;
                    usedSkills.set(skill);
                }
                double reward = meanReward * (0.75 + rng.nextDouble() * 0.7);
                if (rng.nextInt(11) == 0) {
                    reward *= 1.6;
                }
                world.sites.add(new DraftSite(pos, reward, tasks));
            }
            // Каждый реально встретившийся навык должен быть у какого-то объекта.
            if (usedSkills.cardinality() == 0) {
                usedSkills.set(family == Family.MISSING_SKILL ? 1 : 0);
            }

            int[] caps = new int[nOff];
            int capSum = 0;
            for (int o = 0; o < nOff; o++) {
                int cap = switch (family) {
                    case TIGHT_CAP -> 3 + rng.nextInt(3);
                    default -> Math.max(4, (nSites + nOff - 1) / nOff + 2 + rng.nextInt(4));
                };
                caps[o] = cap;
                capSum += cap;
            }
            int minCap = Math.max(nSites / 2, nOff);
            int extra = 0;
            while (capSum < minCap) {
                caps[extra % nOff]++;
                capSum++;
                extra++;
            }

            for (int o = 0; o < nOff; o++) {
                Vec2 pos = officePosition(family, world.sites, hubs, box, o, rng);
                double expectedCatch = meanReward * Math.min(caps[o], Math.max(3, nSites / Math.max(1, nOff)));
                double costShare = switch (family) {
                    case COSTLY_OPEN -> 0.28 + rng.nextDouble() * 0.18;
                    case HIGH_ALPHA -> 0.06 + rng.nextDouble() * 0.08;
                    default -> 0.08 + rng.nextDouble() * 0.12;
                };
                double cost = Math.max(0.5, expectedCatch * costShare);
                world.offices.add(new DraftOffice(pos, cost, caps[o]));
            }

            int nWorkers = Math.max(nOff, nSkills + 1 + rng.nextInt(nOff + 2));
            nWorkers = Math.min(nWorkers, nOff + nSkills + 6);
            fillWorkers(world, rng, family, nSkills, nWorkers, usedSkills);

            for (int o = 0; o < nOff; o++) {
                Vec2 op = world.offices.get(o).pos;
                for (int s = 0; s < nSites; s++) {
                    double euclid = op.dist(world.sites.get(s).pos);
                    double detour = 1.05 + rng.nextDouble() * 0.28;
                    world.distances[o][s] = Math.max(1e-6, euclid * detour);
                }
            }
            return world;
        }

        private static void placeHubs(List<Vec2> hubs, int hubCount, double box, Rng rng) {
            for (int guard = 0; guard < 40 && hubs.size() < hubCount; guard++) {
                Vec2 cand = new Vec2(1.0 + rng.nextDouble() * (box - 2.0),
                        1.0 + rng.nextDouble() * (box - 2.0));
                boolean far = true;
                for (Vec2 h : hubs) {
                    if (h.dist(cand) < box * 0.35) {
                        far = false;
                        break;
                    }
                }
                if (far || hubs.isEmpty()) {
                    hubs.add(cand);
                }
            }
            while (hubs.size() < hubCount) {
                hubs.add(new Vec2(rng.nextDouble() * box, rng.nextDouble() * box));
            }
        }

        private static Vec2 officePosition(Family family, List<DraftSite> sites,
                                           List<Vec2> hubs, double box, int officeIndex, Rng rng) {
            if (family == Family.FAR_OFFICES && officeIndex > 0 && rng.nextInt(3) != 0) {
                return new Vec2(-0.4 * box - rng.nextDouble() * 0.6 * box,
                        -0.4 * box - rng.nextDouble() * 0.6 * box);
            }
            if (family == Family.SITE_CLUSTERS) {
                Vec2 hub = hubs.get(officeIndex % hubs.size());
                return new Vec2(
                        clamp(hub.x + (rng.nextDouble() - 0.5) * box * 0.08, -1, box + 1),
                        clamp(hub.y + (rng.nextDouble() - 0.5) * box * 0.08, -1, box + 1));
            }
            DraftSite near = sites.get(rng.nextInt(sites.size()));
            return new Vec2(
                    clamp(near.pos.x + (rng.nextDouble() - 0.5) * 1.8, -0.5, box + 0.5),
                    clamp(near.pos.y + (rng.nextDouble() - 0.5) * 1.8, -0.5, box + 0.5));
        }

        /**
         * Сначала закрываем все навыки объектов, затем добиваем пул работников.
         */
        private static void fillWorkers(DraftWorld world, Rng rng, Family family,
                                        int nSkills, int nWorkers, BitSet usedSkills) {
            List<Integer> must = new ArrayList<>();
            for (int s = usedSkills.nextSetBit(0); s >= 0; s = usedSkills.nextSetBit(s + 1)) {
                must.add(s);
            }
            int rare = nSkills - 1;
            boolean scarce = family == Family.SCARCE_SKILL && usedSkills.get(rare);

            for (int w = 0; w < nWorkers; w++) {
                int primary;
                if (w < must.size()) {
                    primary = must.get(w);
                } else if (scarce && w > 0) {
                    primary = rng.nextInt(Math.max(1, nSkills - 1));
                    if (family == Family.MISSING_SKILL && primary == 0) {
                        primary = 1;
                    }
                } else {
                    primary = must.get(w % must.size());
                }
                if (family == Family.MISSING_SKILL && primary == 0) {
                    primary = must.get(0);
                }
                boolean extra = rng.nextInt(4) == 0 && nSkills > 1 && !(scarce && primary == rare);
                int second = rng.nextInt(nSkills);
                if (family == Family.MISSING_SKILL && second == 0) {
                    second = primary;
                }
                if (scarce && w > 0 && second == rare) {
                    second = primary;
                }
                int[] skills = extra && second != primary
                        ? new int[]{primary, second}
                        : new int[]{primary};
                world.workers.add(skills);
            }
            if (scarce) {
                boolean hasRare = false;
                for (int[] sk : world.workers) {
                    for (int v : sk) {
                        if (v == rare) {
                            hasRare = true;
                            break;
                        }
                    }
                }
                if (!hasRare) {
                    world.workers.set(0, new int[]{rare});
                }
            }
        }

        private static double clamp(double v, double lo, double hi) {
            return Math.max(lo, Math.min(hi, v));
        }

        private static String num(double v) {
            return String.format(Locale.ROOT, "%.12f", v);
        }

        private static String ints(int[] a) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < a.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(a[i]);
            }
            return sb.append(']').toString();
        }
    }

    /** Масштаб карты: мелкие / средние / чуть крупнее, всегда nOff ≤ 6. */
    enum SizeTier {
        SMALL(8, 8, 2, 2),
        MEDIUM(14, 12, 3, 3),
        LARGE(22, 14, 4, 3);

        final int minSites;
        final int spanSites;
        final int minOff;
        final int spanOff;

        SizeTier(int minSites, int spanSites, int minOff, int spanOff) {
            this.minSites = minSites;
            this.spanSites = spanSites;
            this.minOff = minOff;
            this.spanOff = spanOff;
        }

        static SizeTier of(int index) {
            int m = index % 7;
            if (m == 0) {
                return SMALL;
            }
            if (m == 1 || m == 2) {
                return LARGE;
            }
            return MEDIUM;
        }
    }

    /** SplitMix64, свой ГПСЧ, не из Main. */
    static final class Rng {
        private long state;

        Rng(long seed) {
            this.state = seed == 0 ? 0x9E3779B97F4A7C15L : seed;
        }

        long nextLong() {
            state += 0x9E3779B97F4A7C15L;
            long z = state;
            z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
            z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
            return z ^ (z >>> 31);
        }

        int nextInt(int n) {
            if (n <= 1) {
                return 0;
            }
            return (int) ((nextLong() & Long.MAX_VALUE) % n);
        }

        double nextDouble() {
            return (nextLong() >>> 11) * 0x1.0p-53;
        }
    }
}
