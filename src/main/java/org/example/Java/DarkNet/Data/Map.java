package org.example.Java.DarkNet.Data;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.example.Java.DarkNet.DarkNet;

public class Map {
    public static Coordinates initData(List<Node> nodes, List<Edge> edges, HashMap<Long, Integer> nodeIdToPos) {
        try {
            nodes = ReadFiles.readNodes("nodes.csv");
            edges = ReadFiles.readEdges("edges.csv");

            for (int i = 0; i < nodes.size(); i++) {
                nodeIdToPos.put(nodes.get(i).getId(), i);
            }

            System.out.println(nodes.size());
            System.out.println(edges.size());

            double minLon = nodes.get(0).getLon();
            double maxLon = nodes.get(0).getLon();
            double minLat = nodes.get(0).getLat();
            double maxLat = nodes.get(0).getLat();

            for (Node node : nodes) {
                if (minLat > node.getLat())
                    minLat = node.getLat();
                if (minLon > node.getLon())
                    minLon = node.getLon();
                if (maxLat < node.getLat())
                    maxLat = node.getLat();
                if (maxLon < node.getLon())
                    maxLon = node.getLon();
            }

            double deltaLon = maxLon - minLon;
            double deltaLat = maxLat - minLat;
            double scale = (double) DarkNet.CANVAS_HEIGHT / Math.min(deltaLat, deltaLon);

            System.out.println(deltaLon + " " + deltaLat);
            System.out.println(minLon + " " + minLat + "; " + maxLon + " " + maxLat);

            for (Node node : nodes) {
                node.x = (node.getLon() - minLon) * scale;
                node.y = DarkNet.CANVAS_HEIGHT - (node.getLat() - minLat) * scale;
            }

            for (Edge edge : edges) {
                Node u = nodes.get(nodeIdToPos.get(edge.getU()));
                Node v = nodes.get(nodeIdToPos.get(edge.getV()));
                edge.ux = u.x;
                edge.uy = u.y;
                edge.vx = v.x;
                edge.vy = v.y;
                edge.dist = (long) Euclid.euclideanDist(edge.ux, edge.uy, edge.vx, edge.vy);
            }
            return new Coordinates(nodes, edges, nodeIdToPos);
        } catch (IOException e) {
            System.err.println("Ошибка чтения CSV: " + e.getMessage());
            e.printStackTrace();
            return new Coordinates();
        }
    }
}
