package org.example.BinarySearch;

import java.util.Scanner;

public class Equation {
    private static Scanner in = new Scanner(System.in);
    private double x;
    private double a;

    public Equation() {
        a = in.nextInt();
        math();
    }

    private void math() {
        double n = 0;
        for (int i = 0; i < 10; i++) {
            x += ((double)i) / Math.pow(10, n);
            if (Math.pow(x, 2.0) - x + Math.sqrt(x) == a) break;
            else if (Math.pow(x, 2.0) - x + Math.sqrt(x) > a) {
                x -= (double)i / Math.pow(10.0, n);
                System.out.println(x);
                n+=1.0;
                if(n>=7.0) break;
                else i = 0;
            }
        }
        System.out.println("Result: " + x);
    }
}
