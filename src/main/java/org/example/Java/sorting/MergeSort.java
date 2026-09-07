package org.example.Java.sorting;

import java.util.ArrayList;
import java.util.List;

public class MergeSort<T extends Comparable<T>> implements Sorting<T> {
    private void slice(List<T> nums, int start, int end) {
        if (start >= end)
            return;
        int mid = start + (end - start) / 2;
        slice(nums, start, mid);
        slice(nums, mid + 1, end);
        merge(nums, start, mid, end);
    }

    private void merge(List<T> nums, int start, int middle, int end) {
        List<T> left = new ArrayList<>(nums.subList(start, middle + 1));
        List<T> right = new ArrayList<>(nums.subList(middle + 1, end + 1));
        int i = 0, j = 0, k = start;
        while (i < left.size() && j < right.size()) {
            if (left.get(i).compareTo(right.get(j)) <= 0) {
                nums.set(k++, left.get(i++));
            } else {
                nums.set(k++, right.get(j++));
            }
        }
        while (i < left.size()) {
            nums.set(k++, left.get(i++));
        }
        while (j < right.size()) {
            nums.set(k++, right.get(j++));
        }
    }

    @Override
    public void sort(List<T> nums) {
        if (nums == null) {
            throw new IllegalArgumentException("Received null as an array to sort");
        }
        if (nums.size() < 2) {
            return;
        }
        slice(nums, 0, nums.size() - 1);
    }
}
