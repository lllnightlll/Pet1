package org.example.DarkNet;

import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import org.example.DarkNet.Data.Coordinates;
import org.example.DarkNet.Data.Edge;
import org.example.DarkNet.Data.Node;
import org.example.DarkNet.Data.Map;
import org.example.DarkNet.DrawLibs.RayLib;
import org.example.DarkNet.DrawLibs.LibGDX.LibGDX;

public class DarkNet {
    public static final int CANVAS_WIDTH = 1200;
    public static final int CANVAS_HEIGHT = 800;

    private static final Scanner in = new Scanner(System.in);
    private static List<Node> nodes;
    private static List<Edge> edges;
    private static HashMap<Long, Integer> nodeIdToPos = new HashMap<>();

    private static void initData() {
        Coordinates x = Map.initData(nodes, edges, nodeIdToPos);
        nodes = x.getNodes();
        edges = x.getEdges();
        nodeIdToPos = x.getNodeIdToPos();
    }

    public static void main(String[] args) {
        initData();
        /*System.out.println("Do you like java maboy?");
        String x = (in.nextLine()).toUpperCase();
        while (!(x.equals("YES") || x.equals("NO"))) {
            System.out.println("You wrote a wrong ANSWER...\nWrite again:");
            x = (in.nextLine()).toUpperCase();
        }
        if (x.equals("YES")) {
            
        } else {
            
        }*/LibGDX.initLibGDX(nodes, edges, nodeIdToPos);
    RayLib.draw(nodes, edges, nodeIdToPos);
    }
}