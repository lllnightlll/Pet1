package org.example.DarkNet.Data;

public class Adj {
    private int to;
    private double w;

    public Adj(int to, double w) {
        this.to = to;
        this.w = w;
    }

    public int getTo() {
        return to;
    }

    public double getW() {
        return w;
    }
}
