package org.example.DarkNet.Data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ReadFiles {
    public static List<Node> readNodes(String path) throws IOException {
        List<Node> nodes = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line = br.readLine(); // header
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                long id = Long.parseLong(parts[0]);
                double lon = Double.parseDouble(parts[1]);
                double lat = Double.parseDouble(parts[2]);
                nodes.add(new Node(id, lon, lat));
            }
        }
        return nodes;
    }

    public static List<Edge> readEdges(String path) throws IOException {
        List<Edge> edges = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line = br.readLine(); // header
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                long u = Long.parseLong(parts[0]);
                long v = Long.parseLong(parts[1]);
                edges.add(new Edge(u, v));
            }
        }
        return edges;
    }
}
