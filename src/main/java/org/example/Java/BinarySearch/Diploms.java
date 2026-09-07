package org.example.Java.BinarySearch;

import java.util.Scanner;

public class Diploms {
    private static Scanner in = new Scanner(System.in);
    private static int n;
    private static int w;
    private static int h;

    public Diploms() {
        n = in.nextInt();
        w = in.nextInt();
        h = in.nextInt();
        System.out.println(box());
    }

    private int box() {
        int sum = 2;
        int height = h;
        int width = w;
        while (sum <= n) {
            if (height < width) {
                height += h;
                sum += width / w;
                System.out.println("h" + height);
            } else {
                width += w;
                sum += height / h;
                System.out.println("w" + width);
            }
        }

        return Math.max(height, width);
    }
}
