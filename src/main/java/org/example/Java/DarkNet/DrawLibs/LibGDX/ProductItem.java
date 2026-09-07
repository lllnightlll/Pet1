package org.example.Java.DarkNet.DrawLibs.LibGDX;

public class ProductItem {
    public final String name;
    public final float x;
    public final float y;
    public StoreMode mode = StoreMode.KRAKEN;

    public ProductItem(String name, float x, float y) {
        this.name = name;
        this.x = x;
        this.y = y;
    }
}

enum StoreMode {
    KRAKEN,
    MEGA
}