package org.example.Java.DarkNet.Data;

public class State implements Comparable<State> {
    private int v;
    private double dist;
    private double h;

    public State(int v, double dist) {
        this.v = v;
        this.dist = dist;
        this.h = 0.0;
    }

    public State(int v, double dist, double h) {
        this.v = v;
        this.dist = dist;
        this.h = h;
    }

    public double f() {
        return dist + h;
    }

    public int compareTo(State o) {
        return Double.compare(this.dist, o.dist);
    }

    public int getV() {
        return v;
    }

    public double getDist() {
        return dist;
    }

    public double getH() {
        return h;
    }
}
