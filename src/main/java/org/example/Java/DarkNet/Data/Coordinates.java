package org.example.Java.DarkNet.Data;

import java.util.HashMap;
import java.util.List;

public class Coordinates {
    private List<Node> nodes;
    private List<Edge> edges;
    private HashMap<Long, Integer> nodeIdToPos;

    public Coordinates(List<Node> nodes, List<Edge> edges, HashMap<Long, Integer> nodeIdToPos) {
        this.nodes = nodes;
        this.edges = edges;
        this.nodeIdToPos = nodeIdToPos;
    }

    public Coordinates() {
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public List<Edge> getEdges() {
        return edges;
    }

    public HashMap<Long, Integer> getNodeIdToPos() {
        return nodeIdToPos;
    }
}