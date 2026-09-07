package org.example.Java.minimums;

import java.util.HashMap;
import java.util.Map;

public class PriorityQueueImpl implements PriorityQueue<Integer> {
    private static class Element {
        int value;
        long operationIndex;

        Element(int value, long operationIndex) {
            this.value = value;
            this.operationIndex = operationIndex;
        }
    }

    private Element[] heap;
    private Map<Long, Integer> operationToHeapIndex;
    private int size;
    private long operationCounter;

    public PriorityQueueImpl(int capacity) {
        heap = new Element[capacity + 1];
        operationToHeapIndex = new HashMap<>();
        size = 0;
        operationCounter = 0;
    }

    private void swap(int i, int j) {
        Element temp = heap[i];
        heap[i] = heap[j];
        heap[j] = temp;
        operationToHeapIndex.put(heap[i].operationIndex, i);
        operationToHeapIndex.put(heap[j].operationIndex, j);
    }

    private void siftUp(int idx) {
        while (idx > 1) {
            int parent = idx / 2;
            if (heap[parent].value >= heap[idx].value) break;
            swap(parent, idx);
            idx = parent;
        }
    }

    private void siftDown(int idx) {
        while (true) {
            int left = idx * 2;
            int right = idx * 2 + 1;
            int largest = idx;
            if (left <= size && heap[left].value > heap[largest].value) largest = left;
            if (right <= size && heap[right].value > heap[largest].value) largest = right;
            if (largest == idx) break;
            swap(idx, largest);
            idx = largest;
        }
    }

    @Override
    public void enqueue(Integer value) {
        operationCounter++;
        size++;
        heap[size] = new Element(value, operationCounter);
        operationToHeapIndex.put(operationCounter, size);
        siftUp(size);
    }

    @Override
    public Integer dequeueMax() {
        if (size == 0) {
            return null;
        }
        Element max = heap[1];
        swap(1, size);
        operationToHeapIndex.remove(max.operationIndex);
        heap[size] = null;
        size--;
        siftDown(1);
        return max.value;
    }

    @Override
    public void increment(long operation, Integer addition) {
        Integer heapIndex = operationToHeapIndex.get(operation);
        if (heapIndex == null) return;
        heap[heapIndex].value += addition;
        siftUp(heapIndex);
    }
}