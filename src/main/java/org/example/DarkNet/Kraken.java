package org.example.DarkNet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;

import org.example.DarkNet.Data.Edge;
import org.example.DarkNet.Data.Node;
import org.example.DarkNet.Data.Adj;
import org.example.DarkNet.Data.State;
import org.example.DarkNet.Data.Logic;

public class Kraken implements Logic {
    protected double[] dist;
    protected int[] parent;
    protected List<Node> search = new ArrayList<>();
    protected boolean found = false;

    protected static List<List<Adj>> setAdj(List<List<Adj>> adj, int n, List<Edge> edges,
            HashMap<Long, Integer> nodeIdToPos) {
        for (Edge e : edges) {
            Integer iu = nodeIdToPos.get(e.getU());
            Integer iv = nodeIdToPos.get(e.getV());

            if (iu == null || iv == null)
                continue;
            double w = e.dist;
            adj.get(iu).add(new Adj(iv, w));
            adj.get(iv).add(new Adj(iu, w));
        }

        return adj;
    }

    @Override
    public State addPq(Integer start, List<Node> nodes, Integer target) {
        return new State(start, 0.0);
    }

    @Override
    public PriorityQueue<State> searchPath(PriorityQueue<State> pq, List<List<Adj>> adj, List<Node> nodes,
            Integer target) {
        while (!pq.isEmpty()) {
            State temp = pq.poll();
            if (temp.getDist() > dist[temp.getV()]) {
                continue;
            }

            if (temp.getV() == target) {
                found = true;
                break;
            }

            for (Adj a : adj.get(temp.getV())) {
                if ((dist[temp.getV()] + a.getW()) < dist[a.getTo()]) {
                    dist[a.getTo()] = dist[temp.getV()] + a.getW();
                    parent[a.getTo()] = temp.getV();
                    pq.add(new State(a.getTo(), dist[temp.getV()] + a.getW()));
                    search.add(nodes.get(a.getTo()));
                }
            }
        }

        return pq;
    }

    public List<List<Node>> dijkstraPath(
            List<Node> nodes,
            List<Edge> edges,
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

        List<Node> path = new ArrayList<>();
        int temp = target;
        while (temp != -1) {
            path.add(nodes.get(temp));
            temp = parent[temp];
        }
        Collections.reverse(path);
        List<List<Node>> returned = new ArrayList<>();
        returned.add(search);
        returned.add(path);
        return returned;
    }
}
