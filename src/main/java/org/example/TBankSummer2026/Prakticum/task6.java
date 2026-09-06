package org.example.TBankSummer2026.Prakticum;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public class task6 {
    static List<Integer>[] adj;
    static int[] disc, low, cycleBlocks;
    static int timer;
    static boolean found;
    static Deque<int[]> stack;

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        int t = in.nextInt();
        StringBuilder out = new StringBuilder();
        while (t-- > 0) {
            int n = in.nextInt();
            int m = in.nextInt();
            adj = new ArrayList[n + 1];
            for (int i = 1; i <= n; i++) {
                adj[i] = new ArrayList<>();
            }
            for (int i = 0; i < m; i++) {
                int a = in.nextInt();
                int b = in.nextInt();
                adj[a].add(b);
                adj[b].add(a);
            }
            out.append(hasVertexOnTwoCycles(n) ? "YES\n" : "NO\n");
        }
        System.out.print(out);
    }

    static boolean hasVertexOnTwoCycles(int n) {
        disc = new int[n + 1];
        low = new int[n + 1];
        cycleBlocks = new int[n + 1];
        timer = 0;
        found = false;
        stack = new ArrayDeque<>();
        for (int v = 1; v <= n && !found; v++) {
            if (disc[v] == 0) {
                dfs(v, 0);
                if (!stack.isEmpty()) {
                    processBlock();
                }
            }
        }
        if (found) {
            return true;
        }
        for (int v = 1; v <= n; v++) {
            if (cycleBlocks[v] >= 2) {
                return true;
            }
        }
        return false;
    }

    static void dfs(int v, int parent) {
        disc[v] = low[v] = ++timer;
        for (int to : adj[v]) {
            if (to == parent) {
                continue;
            }
            if (disc[to] == 0) {
                stack.push(new int[]{v, to});
                dfs(to, v);
                low[v] = Math.min(low[v], low[to]);
                if (low[to] >= disc[v]) {
                    processBlockUntil(v, to);
                }
            } else if (disc[to] < disc[v]) {
                stack.push(new int[]{v, to});
                low[v] = Math.min(low[v], disc[to]);
            }
        }
    }

    static void processBlockUntil(int u, int v) {
        List<int[]> edges = new ArrayList<>();
        while (!stack.isEmpty()) {
            int[] e = stack.pop();
            edges.add(e);
            if (e[0] == u && e[1] == v) {
                break;
            }
        }
        inspect(edges);
    }

    static void processBlock() {
        List<int[]> edges = new ArrayList<>();
        while (!stack.isEmpty()) {
            edges.add(stack.pop());
        }
        inspect(edges);
    }

    static void inspect(List<int[]> edges) {
        if (edges.isEmpty()) {
            return;
        }
        Set<Integer> verts = new HashSet<>();
        for (int[] e : edges) {
            verts.add(e[0]);
            verts.add(e[1]);
        }
        int nv = verts.size();
        int ne = edges.size();
        if (nv < 3) {
            return;
        }
        if (ne > nv) {
            found = true;
            return;
        }
        if (ne == nv) {
            for (int v : verts) {
                cycleBlocks[v]++;
            }
        }
    }
}
