package org.example.Java.Graf;

import java.util.Scanner;
import java.util.ArrayList;
import java.util.Collections;

@SuppressWarnings("unchecked")
public class TS {
    // [ServerResources]
    private class Element {
        int x;
        boolean y = false;

        Element(int x) {
            this.x = x;
        }

        int get_x() {
            return x;
        }

        @SuppressWarnings("unused")
        boolean get_y() {
            return y;
        }

        @SuppressWarnings("unused")
        void setFlag() {
            y = true;
        }
    }

    private Scanner in = new Scanner(System.in);
    private ArrayList<Element>[] elements;
    private ArrayList<ArrayList<Integer>> printable;
    private boolean visited[];

    // [ServerRpc]
    TS(int n) {
        elements = new ArrayList[n];
        for (int i = 0; i < n; i++) {
            elements[i] = new ArrayList<>();
        }
        if (registerElements()) {
            DFS();
            printAll();
        } else {
            System.out.println("While(true)");
        }
    }

    private boolean registerElements() {
        for (int i = 0; i < elements.length; i++) {
            int temp1 = in.nextInt();
            if (temp1 < 0) {
                return true;
            }
            int temp2 = in.nextInt();
            if (temp2 < 0) {
                return true;
            }

            if (temp1 != temp2) {
                elements[temp1 - 1].add(new Element(temp2));
            } else if (elements[temp1 - 1].size() == 0) {
                elements[temp1 - 1].add(new Element(temp2));
            } else {
                return false;
            }
        }
        return true;
    }

    private void DFS() {
        printable = new ArrayList<>();
        visited = new boolean[elements.length];
        for (int i = 0; i < elements.length; i++) {
            if (!visited[i]) {
                ArrayList<Integer> component = new ArrayList<>();
                dfs(i, component);
                Collections.reverse(component);
                printable.add(component);
            }
        }
    }

    private void dfs(int i, ArrayList<Integer> component) {
        /*
         * boolean visited = false;
         * for (int j = 0; j < elements.length; j++) {
         * for (Element e : elements[i]) {
         * if (e.get_x() == i + 1 && !e.get_y()) {
         * visited = true;
         * break;
         * }
         * }
         * if (visited) {
         * break;
         * }
         * }
         * 
         * if (visited) {
         * return;
         * }
         */

        visited[i] = true;

        for (Element neighbor : elements[i]) {
            int next = neighbor.get_x() - 1;
            if (!visited[next]) {
                dfs(next, component);
            }
        }
        component.add(i + 1);
    }

    private void printAll() {
        for (ArrayList<Integer> level : printable) {
            for (int v : level) {
                System.out.print(v + " ");
            }
            System.out.println();
        }
    }
}
