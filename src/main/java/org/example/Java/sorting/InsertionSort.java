package org.example.Java.sorting;

import java.util.List;

public class InsertionSort<T extends Comparable<T>> implements Sorting<T> {
    @Override
    public void sort(List<T> nums) {
        for (int i = 1; i < nums.size(); i++) {
            T k = nums.get(i);
            int j = i - 1;
            while ((j >= 0) && (k.compareTo(nums.get(j))) < 0) {
                nums.set(j + 1, nums.get(j));
                j--;
            }
            nums.set(j + 1, k);
        }
    }
}
