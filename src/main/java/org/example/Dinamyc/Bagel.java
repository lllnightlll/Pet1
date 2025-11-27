package org.example.Dinamyc;

public class Bagel {
    private int[][] matrix;
    private int N;
    private int M;

    public Bagel() {

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
        int cur = size - 1;
        System.out.println(dist[size - 1]);
        while (cur != 0) {
            int p = prev[cur];
            if (p == cur - 1) System.out.print("D");
            else if (p == cur - M) System.out.print("R");
            cur = p;
        }
    }

}
