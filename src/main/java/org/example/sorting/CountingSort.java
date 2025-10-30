package org.example.sorting;

import java.util.List;

public class CountingSort implements Sorting<Integer> {

    @Override
    public void sort(List<Integer> nums) {
        int[] temp = new int[128];
        for (int id : nums) {
            temp[id]++;
        }

        int k = 0;
        for (int i = 0; i < temp.length; i++) {
            for (int j = 0; j < temp[i]; j++) {
                nums.set(k++, i);
            }
        }
    }
}
