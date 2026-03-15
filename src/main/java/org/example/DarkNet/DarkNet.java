package org.example.DarkNet;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.*;

//import com.raylib.Colors;
//import com.raylib.jextract.raylib_h;

//import static com.raylib.Colors.*;
import static com.raylib.Raylib.*;
import static com.raylib.Helpers.*;

public class DarkNet {
    static final int CANVAS_WIDTH = 1200;
    static final int CANVAS_HEIGHT = 800;
    static final private Scanner in = new Scanner(System.in);

    public static class Node {
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

    public static class Edge {
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

            System.out.println("What is your favourite NarkoShop? Kraken or Mega?");
            String x = (in.nextLine()).toUpperCase();
            for (int i = 0; i < 10; i++) {
                System.out.print(".");
                TimeUnit.MILLISECONDS.sleep(100);
            }
            System.out.println();
            while (!(x.equals("KRAKEN") || x.equals("MEGA"))) {
                System.out.println("You wrote a wrong NarkoShop...\nWrite again:");
                x = (in.nextLine()).toUpperCase();
                for (int i = 0; i < 10; i++) {
                    System.out.print(".");
                    TimeUnit.MILLISECONDS.sleep(100);
                }
                System.out.println();
            }
            if (x.equals("KRAKEN") || x.equals("MEGA")) {
                System.out.println("CORRECT!");
            }
            InitWindow(CANVAS_WIDTH, CANVAS_HEIGHT, "OMSK");
            SetTargetFPS(144);
            System.out.println("Write cords: ");

            while (!WindowShouldClose()) {
                BeginDrawing();
                ClearBackground(newColor(30, 30, 30, 255));

                for (Node node : nodes) {
                    DrawCircle((int) node.x, (int) node.y, 1, newColor(104, 124, 124, 255));
                }

                for (Edge edge : edges) {
                    DrawLine(
                            (int) edge.ux, (int) edge.uy,
                            (int) edge.vx, (int) edge.vy,
                            newColor(91, 126, 119, 32));
                }
                EndDrawing();

                long start = in.nextLong();
                long end = in.nextLong();
                List<List<DarkNet.Node>> zakladka_map = new ArrayList<>();
                if ((x.toUpperCase()).equals("KRAKEN")) {
                    zakladka_map = (new Kraken()).dijkstraPath(nodes, edges, nodeIdToPos, start, end);
                } else if ((x.toUpperCase()).equals("MEGA")) {
                    zakladka_map = (new MEGA()).dijkstraPath(nodes, edges, nodeIdToPos, start, end);
                }

                if (!zakladka_map.isEmpty()) {
                    System.out.println("We are search the nearest way...");
                    List<DarkNet.Node> zakladka = zakladka_map.get(0);
                    if (zakladka != null && zakladka.size() > 1) {
                        int z = 0;
                        for (int i = 0; i < zakladka.size() - 1; i++) {
                            Node a = zakladka.get(i);
                            Node b = zakladka.get(i + 1);
                            if (z == 0) {
                                BeginDrawing();
                            } else if (z == 100 || i == zakladka.size() - 2) {
                                z = -1;
                                TimeUnit.MILLISECONDS.sleep(100);
                                EndDrawing();
                                System.out.println("PLease wait few second...");
                            }
                            DrawLine((int) a.x, (int) a.y, (int) b.x, (int) b.y, newColor(139, 232, 208, 64));
                            z++;
                        }
                    }
                    System.out.println("The shortest path was found...");
                    TimeUnit.SECONDS.sleep(1);
                    zakladka = zakladka_map.get(1);
                    if (zakladka != null && zakladka.size() > 1) {
                        for (int i = 0; i < zakladka.size() - 1; i++) {
                            Node a = zakladka.get(i);
                            Node b = zakladka.get(i + 1);
                            BeginDrawing();
                            DrawLine((int) a.x, (int) a.y, (int) b.x, (int) b.y, newColor(121, 250, 242, 255));
                            EndDrawing();
                            if (i == zakladka.size() - 2) {
                                if ((x.toUpperCase()).equals("MEGA")) {
                                    i = 0;
                                } else {
                                    while (true) {
                                        TimeUnit.MILLISECONDS.sleep(100);
                                    }
                                }
                            }
                        }
                    }
                } else {
                    System.out.println("We have error...\nTry again:");
                }
            }
            CloseWindow();
        } catch (IOException e) {
            System.err.println("Ошибка чтения CSV: " + e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}