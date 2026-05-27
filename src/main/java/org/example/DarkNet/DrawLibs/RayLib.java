package org.example.DarkNet.DrawLibs;

import static com.raylib.Raylib.*;
import static com.raylib.Helpers.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.example.DarkNet.DarkNet;
import org.example.DarkNet.Kraken;
import org.example.DarkNet.MEGA;
import org.example.DarkNet.Data.Node;
import org.example.DarkNet.Data.Edge;

public class RayLib {
    static final private Scanner in = new Scanner(System.in);

    public static void draw(List<Node> nodes, List<Edge> edges, HashMap<Long, Integer> nodeIdToPos) {
        try {
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
            InitWindow(DarkNet.CANVAS_WIDTH, DarkNet.CANVAS_HEIGHT, "OMSK");
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
                List<List<Node>> zakladka_map = new ArrayList<>();
                if ((x.toUpperCase()).equals("KRAKEN")) {
                    zakladka_map = (new Kraken()).dijkstraPath(nodes, edges, nodeIdToPos, start, end);
                } else if ((x.toUpperCase()).equals("MEGA")) {
                    zakladka_map = (new MEGA()).dijkstraPath(nodes, edges, nodeIdToPos, start, end);
                }

                if (!zakladka_map.isEmpty()) {
                    System.out.println("We are search the nearest way...");
                    List<Node> zakladka = zakladka_map.get(0);
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
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
