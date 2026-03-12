package org.example.DarkNet;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.*;
import static com.raylib.Colors.*;
import static com.raylib.Raylib.*;


public class DarkNet {
    static final int CANVAS_WIDTH = 1200;
    static final int CANVAS_HEIGHT = 800;

    private static class Node {
        long id;
        double lon;
        double lat;

        double x;
        double y;

        Node(long id, double lon, double lat) {
            this.id = id;
            this.lon = lon;
            this.lat = lat;
        }
    };

    private static class Edge {
        long u;
        long v;

        double ux;
        double uy;

        double vx;
        double vy;

        long dist; // расстояние между u-v

        Edge(long u, long v) {
            this.u = u;
            this.v = v;
        }
    };

    public static double euclideanDist(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y1 - y2, 2));
    }

    private static List<Node> readNodes(String path) throws IOException {
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

    private static List<Edge> readEdges(String path) throws IOException {
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

    public static void main(String[] args) {
        try {
            List<Node> nodes = readNodes("nodes.csv");
            List<Edge> edges = readEdges("edges.csv");

            HashMap<Long, Integer> nodeIdToPos = new HashMap<>();
            for (int i = 0; i < nodes.size(); i++) {
                nodeIdToPos.put(nodes.get(i).id, i);
            }

            System.out.println(nodes.size());
            System.out.println(edges.size());

            double minLon = nodes.get(0).lon;
            double maxLon = nodes.get(0).lon;
            double minLat = nodes.get(0).lat;
            double maxLat = nodes.get(0).lat;

            for (Node node : nodes) {
                if (minLat > node.lat)
                    minLat = node.lat;
                if (minLon > node.lon)
                    minLon = node.lon;
                if (maxLat < node.lat)
                    maxLat = node.lat;
                if (maxLon < node.lon)
                    maxLon = node.lon;
            }

            double deltaLon = maxLon - minLon;
            double deltaLat = maxLat - minLat;
            double scale = (double) CANVAS_HEIGHT / Math.min(deltaLat, deltaLon);

            System.out.println(deltaLon + " " + deltaLat);
            System.out.println(minLon + " " + minLat + "; " + maxLon + " " + maxLat);

            for (Node node : nodes) {
                node.x = (node.lon - minLon) * scale;
                node.y = CANVAS_HEIGHT - (node.lat - minLat) * scale; // тот же костыль
            }

            for (Edge edge : edges) {
                Node u = nodes.get(nodeIdToPos.get(edge.u));
                Node v = nodes.get(nodeIdToPos.get(edge.v));
                edge.ux = u.x;
                edge.uy = u.y;
                edge.vx = v.x;
                edge.vy = v.y;
                edge.dist = (long) euclideanDist(edge.ux, edge.uy, edge.vx, edge.vy);
            }

            InitWindow(CANVAS_WIDTH, CANVAS_HEIGHT, "OMSK");
            SetTargetFPS(60);

            while (!WindowShouldClose()) {
                BeginDrawing();
                ClearBackground(RAYWHITE);

                for (Node node : nodes) {
                    DrawCircle((int) node.x, (int) node.y, 2, RED);
                }

                for (Edge edge : edges) {
                    DrawLine(
                        (int) edge.ux, (int) edge.uy,
                        (int) edge.vx, (int) edge.vy,
                        BLACK
                    );
                }

                EndDrawing();
            }
            CloseWindow();
        } catch (IOException e) {
            System.err.println("Ошибка чтения CSV: " + e.getMessage());
            e.printStackTrace();
        }
    }
}