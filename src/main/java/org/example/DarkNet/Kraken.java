package org.example.DarkNet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;

public class Kraken {

    double[] dist;
    int[] parent;
    List<DarkNet.Node> search = new ArrayList<>();
    boolean found = false;

    protected static class Adj {
        int to;
        double w;

        Adj(int to, double w) {
            this.to = to;
            this.w = w;
        }
    }

    protected static class State implements Comparable<State> {
        int v;
        double dist;
        double h;

        State(int v, double dist) {
            this.v = v;
            this.dist = dist;
            this.h = 0.0;
        }

        State(int v, double dist, double h) {
            this.v = v;
            this.dist = dist;
            this.h = h;
        }

        double f() {
            return dist + h;
        }

        public int compareTo(State o) {
            return Double.compare(this.dist, o.dist);
        }
    }

    protected static List<List<Adj>> setAdj(List<List<Adj>> adj, int n, List<DarkNet.Edge> edges,
            HashMap<Long, Integer> nodeIdToPos) {
        for (DarkNet.Edge e : edges) {
            Integer iu = nodeIdToPos.get(e.u);
            Integer iv = nodeIdToPos.get(e.v);

            if (iu == null || iv == null)
                continue;
            double w = e.dist;
            adj.get(iu).add(new Adj(iv, w));
            adj.get(iv).add(new Adj(iu, w));
        }

        return adj;
    }

    protected State addPq(Integer start, List<DarkNet.Node> nodes, Integer target) {
        return new State(start, 0.0);
    }

    protected PriorityQueue<State> searchPath(PriorityQueue<State> pq, List<List<Adj>> adj, List<DarkNet.Node> nodes,
            Integer target) {
        while (!pq.isEmpty()) {
            State temp = pq.poll();
            if (temp.dist > dist[temp.v]) {
                continue;
            }

            if (temp.v == target) {
                found = true;
                break;
            }

            for (Adj a : adj.get(temp.v)) {
                if ((dist[temp.v] + a.w) < dist[a.to]) {
                    dist[a.to] = dist[temp.v] + a.w;
                    parent[a.to] = temp.v;
                    pq.add(new State(a.to, dist[temp.v] + a.w));
                    search.add(nodes.get(a.to));
                }
            }
        }

        return pq;
    }

    protected List<List<DarkNet.Node>> dijkstraPath(
            List<DarkNet.Node> nodes,
            List<DarkNet.Edge> edges,
            HashMap<Long, Integer> nodeIdToPos,
            long startId,
            long targetId) {
        int n = nodes.size();
        List<List<Adj>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            adj.add(new ArrayList<>());
        }
        adj = setAdj(adj, n, edges, nodeIdToPos);

        Integer start = nodeIdToPos.get(startId);
        Integer target = nodeIdToPos.get(targetId);
        if (start == null || target == null) {
            return Collections.emptyList();
        }

        dist = new double[n];
        parent = new int[n];
        for (int i = 0; i < n; i++) {
            dist[i] = Double.POSITIVE_INFINITY;
            parent[i] = -1;
        }
        dist[start] = 0.0;

        PriorityQueue<State> pq = new PriorityQueue<>();
        pq.add(addPq(start, nodes, target));

        pq = searchPath(pq, adj, nodes, target);

        if (!found) {
            return Collections.emptyList();
        }

        List<DarkNet.Node> path = new ArrayList<>();
        int temp = target;
        while (temp != -1) {
            path.add(nodes.get(temp));
            temp = parent[temp];
        }
        Collections.reverse(path);
        List<List<DarkNet.Node>> returned = new ArrayList<>();
        returned.add(search);
        returned.add(path);
        return returned;
    }
}
