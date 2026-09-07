package org.example.Java.DarkNet.Data;

public class Edge {
    long u;
    long v;

    public double ux;
    public double uy;

    public double vx;
    public double vy;

    public long dist; // расстояние между u-v

    Edge(long u, long v) {
        this.u = u;
        this.v = v;
    }

    public long getU() {
        return u;
    }

    public long getV() {
        return v;
    }
};
