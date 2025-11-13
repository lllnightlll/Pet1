package org.example.BinarySearch;

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
        System.out.println(Box());
    }

    private int Box() {
        int sum = 2;
        int height = h;
        int weight = w;
        while (sum < n) {
            if (height < weight) {
                height += h;
                sum += weight / w;
                System.out.println("h" + height);
            } else {
                weight += w;
                sum += height / h;
                System.out.println("w" + weight);
            }
        }

        if (height < weight) return weight;
        return height;
    }
}
