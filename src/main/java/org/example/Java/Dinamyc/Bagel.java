package org.example.Java.Dinamyc;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Bagel {
    private int[][] matrix;
    private int N = 0;
    private int M = 0;
    private String way;

    public Bagel(int[][] matrix, int n, int m) {
        runByInlineData(matrix, n, m);
    }

    public Bagel(String dataPath, String outputPath) {
        runByFile(dataPath, outputPath);
    }

    private void readFieldData(String filePath) {
        try (BufferedReader file = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "UTF-8"))) {
            String line;
            List<Integer> data = new ArrayList<>();
            while ((line = file.readLine()) != null) {
                String[] parts = line.split(";");
                M = parts.length;
                N++;
                for (String p : parts) {
                    if (!p.isEmpty()) {
                        data.add(Integer.parseInt(p.trim()));
                    }
                }
            }
            writeMatrix(data);
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    private void writeMatrix(List<Integer> data) {
        matrix = new int[N][M];
        for (int j = 0; j < N; j++) {
            for (int i = 0; i < M; i++) {
                this.matrix[j][i] = data.getFirst();
                data.removeFirst();
            }
        }
    }

    private void writeToFile(String filePath, String way) {
        try (BufferedWriter file = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), "UTF-8"))) {
            file.write(way.length());
            file.newLine();
            file.write(way);
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    private static void writeToConsole(String way) {
        System.out.println(way);
    }

    private String recreatePath(int[] prev, int M, int N) {
        int size = N * M;
        int cur = size - 1;

        StringBuilder stringBuilder = new StringBuilder();
        while (cur != 0) {
            int p = prev[cur];
            if (p == cur - 1) {
                stringBuilder.append('D');
            } else if (p == cur - M) {
                stringBuilder.append('R');
            }
            cur = p;
        }
        return stringBuilder.toString();
    }

    public void runByFile(String dataPath, String outputPath) {
        readFieldData(dataPath);
        antiBellmanFord(matrix, N, M);
        writeToFile(outputPath, way);
    }

    public void runByInlineData(final int[][] matrix, int n, int m) {
        antiBellmanFord(matrix, n, m);
        writeToConsole(way);
    }

    private void antiBellmanFord(final int[][] matrix, int N, int M) {
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
        this.way = recreatePath(prev, M, N);
    }
}
