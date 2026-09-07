package org.example.Java.DarkNet.Data;

import java.util.List;
import java.util.PriorityQueue;

public interface Logic {
    State addPq(Integer start, List<Node> nodes, Integer target);

    PriorityQueue<State> searchPath(PriorityQueue<State> pq, List<List<Adj>> adj, List<Node> nodes,
            Integer target);
}
