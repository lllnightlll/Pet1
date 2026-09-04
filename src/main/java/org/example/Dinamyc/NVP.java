package org.example.Dinamyc;

import java.io.*;

public class NVP {
    private int n;
    private int[] x;
    private static int[] data;
    private static int j;

    public NVP (int[] x) {
        nvp(x.length, x);
    }

    public NVP(String inputFile, String outputFile) {
        writeByData(inputFile, outputFile);
    }

    private void writeByData(String inputFile, String outputFile) {
        readFile(inputFile);
        nvp(n, x, true);
        writeFile(outputFile);
    }

    private void readFile(String inputFile) {
        try (BufferedReader file = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), "UTF-8"))) {
            this.n = Integer.parseInt(file.readLine());
            this.x = new int[n];

            String[] parts = file.readLine().split(" ");
            int j = 0;
            for (String p : parts) {
                if (!p.isEmpty()) {
                    this.x[j++] = Integer.parseInt(p.trim());
                }
            }
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    private void writeFile(String outputFile) {
        try (BufferedWriter file = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8"))) {
            file.write(j+"");
            file.newLine();
            for (int i = j - 1; i > -1; i--) {
                file.write(data[i] + " ");
            }
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    private static void writeByConsole(int j, int[] data) {
        System.out.println(j);
        for (int i = j - 1; i > -1; i--) {
            System.out.printf(data[i] + " ");
        }
    }

    public static void nvp(int n, int[] x) {
        nvp(n, x, false);
    }
    public static void nvp(int n, int[] x, boolean f) {
        int pos = 0;
        int[] data = new int[n];
        /*for (int i = 0; i < n; i++) {
            int sum = 1;
            int k = i;
            for (int j = i; j < n; j++) {
                if (x[j] > x[k]) {
                    sum++;
                    k = j;
                    data[i] = sum;
                }
            }
        }*/
        int k = n - 1;
        int j = 1;
        data[0] = x[k];
        for (int i = n - 1; i >= pos; i--) {
            if (x[i] < x[k]) {
                k = i;
                data[j++] = x[i];
            }
        }

        if (f) {
            NVP.data = data;
            NVP.j = j;
        } else {
            writeByConsole(j, data);
        }
    }
}
