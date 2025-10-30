package org.example.sorting;

import java.math.BigInteger;
import java.util.List;

public class RadixSort implements Sorting<BigInteger> {
    private static final int NUMBER_OF_DIGITS = 10;

    @Override
    public void sort(List<BigInteger> nums) {
        BigInteger max = nums.get(0);
        for (int q = 1; q < nums.size(); q++) {
            if (max.compareTo(nums.get(q)) < 0) {
                max = nums.get(q);
            }
        }
        for (BigInteger pow = BigInteger.valueOf(1);
             max.divide(pow).compareTo(BigInteger.ZERO) > 0;
             pow = pow.multiply(BigInteger.TEN)) {

            function(nums, pow);
        }
    }

    private int getDigitAt(BigInteger num, BigInteger pow) {
        return num.divide(pow).mod(BigInteger.TEN).intValue();
    }

    private void function(List<BigInteger> nums, BigInteger pow) {
        int[] temp = new int[NUMBER_OF_DIGITS];
        BigInteger[] copy = new BigInteger[nums.size()];

        for (int w = 0; w < nums.size(); w++) {
            temp[getDigitAt(nums.get(w), pow)]++;
        }

        for (int q = 1; q < NUMBER_OF_DIGITS; q++) {
            temp[q] += temp[q - 1];
        }

        for (int q = nums.size() - 1; q >= 0; q--) {
            BigInteger num = nums.get(q);
            copy[temp[getDigitAt(num, pow) % NUMBER_OF_DIGITS] - 1] = num;
            temp[getDigitAt(num, pow) % NUMBER_OF_DIGITS]--;
        }

        for (int q = 0; q < nums.size(); q++) {
            nums.set(q, copy[q]);
        }
    }
}
