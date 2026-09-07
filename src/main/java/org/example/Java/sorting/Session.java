package org.example.Java.sorting;

import java.util.ArrayList;
import java.util.List;

public class Session <T extends  Comparable<T>>{
    public final Sorting<T> sorter;
    public List<T> values = new ArrayList<T>();
    public int idx = 0;
    public int size = 0;
    public Session(Sorting<T> sorter) {this.sorter = sorter;}
}
