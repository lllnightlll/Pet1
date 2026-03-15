package org.example.DarkNet;

import java.util.List;
import java.util.PriorityQueue;

public class MEGA extends Kraken {
    private static double heuristic(DarkNet.Node a, DarkNet.Node b) {
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    protected State addPq(Integer start, List<DarkNet.Node> nodes, Integer target) {
        return new State(start, 0.0, heuristic(nodes.get(start), nodes.get(target)));
    }

    @Override
    protected PriorityQueue<State> searchPath(PriorityQueue<State> pq, List<List<Adj>> adj, List<DarkNet.Node> nodes,
            Integer target) {
        boolean[] closed = new boolean[nodes.size()];
        while (!pq.isEmpty()) {
            State temp = pq.poll();
            if (closed[temp.v]) {
                continue;
            }
            closed[temp.v] = true;

            if (temp.v == target) {
                found = true;
                break;
            }

            for (Adj a : adj.get(temp.v)) {
                int v = a.to;
                if (closed[v])
                    continue;

                double tentativeG = dist[temp.v] + a.w;
                if (tentativeG < dist[v]) {
                    dist[v] = tentativeG;
                    parent[v] = temp.v;

                    double h = heuristic(nodes.get(v), nodes.get(target));
                    pq.add(new State(v, dist[v], h));
                }
            }
        }

        return pq;
    }
}
