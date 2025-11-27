package org.example.Dinamyc;

import java.io.*;

public class NVP {
    private int n;
    private int[] x;

    public NVP() {
        try (BufferedReader file = new BufferedReader(new InputStreamReader(new FileInputStream("lis-input.txt"), "UTF-8"))) {
            String line;
            int i = 0;
            while ((line = file.readLine()) != null) {
                if (i == 0) {
                    n = Integer.parseInt(line);
                    x = new int[n];
                    i++;
                } else {
                    String[] parts = line.split(" ");
                    int j = 0;
                    for (String p : parts) {
                        if (!p.isEmpty()) {
                            x[j++] = Integer.parseInt(p.trim());
                        }
                    }
                }
            }
            nvp(n, x);
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public static void nvp(int n, int[] x) {
        int pos = 0;
        int[] data = new int[n];
        for (int i = 0; i < n; i++) {
            int sum = 1;
            int k = i;
            for (int j = i; j < n; j++) {
                if (x[j] > x[k]) {
                    sum++;
                    k = j;
                    data[i] = sum;
                }
            }
        }
        int k = n - 1;
        int j = 1;
        data[0] = x[k];
        for (int i = n - 1; i >= pos; i--) {
            if (x[i] < x[k]) {
                k = i;
                data[j++] = x[i];
            }
        }

        try (BufferedWriter file = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("lis-output.txt"), "UTF-8"))) {
            System.out.println(j);
            file.write(j+"");
            file.newLine();
            for (int i = j - 1; i > -1; i--) {
                System.out.printf(data[i] + " ");
                file.write(data[i] + " ");
            }
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }
}
