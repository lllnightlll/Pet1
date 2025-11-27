package org.example.Dinamyc;

import java.io.*;

public class Bagel {
    private int[][] matrix;
    private int N = 0;
    private int M = 0;

    public Bagel() {
        try (BufferedReader file = new BufferedReader(new InputStreamReader(new FileInputStream("roguelike-input.csv"), "UTF-8"))) {
            String line;
            while ((line = file.readLine()) != null) {
                M = line.split(";").length;
                N++;
            }
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }

        try (BufferedReader file = new BufferedReader(new InputStreamReader(new FileInputStream("roguelike-input.csv"), "UTF-8"))) {
            String line;
            matrix = new int[N][M];
            int i = 0;
            while ((line = file.readLine()) != null) {
                int j = 0;
                String[] parts = line.split(";");
                for (String p : parts) {
                    if (!p.isEmpty()) {
                        int value = Integer.parseInt(p.trim());
                        matrix[i][j++] = value;
                    }
                }
                i++;
            }
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }

        AntiBellmanFord(matrix, N, M);
    }

    public static void AntiBellmanFord(final int[][] matrix, int N, int M) {
        int size = N * M;
        int[] dist = new int[size];
        int[] prev = new int[size];
        for (int i = 0; i < size; i++) dist[i] = Integer.MIN_VALUE;
        dist[0] = matrix[0][0];
        for (int i = 0; i < size; i++) {
            dist[i] = Integer.MIN_VALUE;
            prev[i] = -1;
        }
        dist[0] = matrix[0][0];

        for (int iter = 0; iter < N + M - 2; iter++) {
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < M; j++) {
                    int v = i * M + j;
                    if (i + 1 < N) {
                        int to = (i + 1) * M + j;
                        if (dist[to] < dist[v] + matrix[i + 1][j]) {
                            dist[to] = dist[v] + matrix[i + 1][j];
                            prev[to] = v;
                        }
                    }
                    if (j + 1 < M) {
                        int to = i * M + (j + 1);
                        if (dist[to] < dist[v] + matrix[i][j + 1]) {
                            dist[to] = dist[v] + matrix[i][j + 1];
                            prev[to] = v;
                        }
                    }
                }
            }
        }
        try (BufferedWriter file = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("roguelike-output.txt"), "UTF-8"))) {
            int cur = size - 1;
            System.out.println(dist[size - 1]);
            file.write(String.valueOf(dist[size - 1]));
            file.newLine();
            while (cur != 0) {
                int p = prev[cur];
                if (p == cur - 1) {
                    System.out.print("D");
                    file.write("D");
                } else if (p == cur - M) {
                    System.out.print("R");
                    file.write("R");
                }
                cur = p;
            }
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }
}
