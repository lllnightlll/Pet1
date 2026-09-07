package org.example.Java.DarkNet;

import java.util.List;
import java.util.PriorityQueue;

import org.example.Java.DarkNet.Data.Adj;
import org.example.Java.DarkNet.Data.Node;
import org.example.Java.DarkNet.Data.State;

public class MEGA extends Kraken {
    private static double heuristic(Node a, Node b) {
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    public State addPq(Integer start, List<Node> nodes, Integer target) {
        return new State(start, 0.0, heuristic(nodes.get(start), nodes.get(target)));
    }

    @Override
    public PriorityQueue<State> searchPath(PriorityQueue<State> pq, List<List<Adj>> adj, List<Node> nodes,
            Integer target) {
        boolean[] closed = new boolean[nodes.size()];
        while (!pq.isEmpty()) {
            State temp = pq.poll();
            if (closed[temp.getV()]) {
                continue;
            }
            closed[temp.getV()] = true;

            if (temp.getV() == target) {
                found = true;
                break;
            }

            for (Adj a : adj.get(temp.getV())) {
                int v = a.getTo();
                if (closed[v])
                    continue;

                double tentativeG = dist[temp.getV()] + a.getW();
                if (tentativeG < dist[v]) {
                    dist[v] = tentativeG;
                    parent[v] = temp.getV();

                    double h = heuristic(nodes.get(v), nodes.get(target));
                    pq.add(new State(v, dist[v], h));
                }
            }
        }

        return pq;
    }
}
