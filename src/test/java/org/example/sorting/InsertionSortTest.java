package org.example.sorting;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InsertionSortTest {
    private final Sorting<Integer> sorting = new InsertionSort<>();

    @Test
    void test() {
        List<Integer> nums = new ArrayList<>(Arrays.asList(1, 3, 2));
        sorting.sort(nums);

        assertArrayEquals(Arrays.asList(1, 2, 3).toArray(), nums.toArray());
    }
}