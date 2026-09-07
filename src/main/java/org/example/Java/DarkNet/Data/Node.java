package org.example.Java.DarkNet.Data;

public class Node {
    long id;
    double lon;
    double lat;

    public double x;
    public double y;

    Node(long id, double lon, double lat) {
        this.id = id;
        this.lon = lon;
        this.lat = lat;
    }

    public long getId() {
        return id;
    }

    public double getLon() {
        return lon;
    }

    public double getLat() {
        return lat;
    }
};
