package org.example.DarkNet.Data;

public class Euclid {
    public static double euclideanDist(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y1 - y2, 2));
    }
}
