package org.example.Graf;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.HashSet;

public class CC {
    // [ServerResources]
    private class rig {
        private int x, y;
        private int flag =0; 

        rig(int x, int y) {
            this.x = x;
            this.y = y;
        }

        private int get_x() {
            return x;
        }

        private int get_y() {
            return y;
        }

        private int seeFlag() {
            return flag;
        }

        private void setFlag() {
            flag = 1;
        }
    }

    private int n = 0, k = 0;
    private rig[] m;
    private Scanner in = new Scanner(System.in);
    private LinkedList<HashSet<Integer>> components = new LinkedList<>();

    // [ServerRpc]
    CC(int n) {
        this.n = n;
        k = (n - 1) * n / 2 + n;
        m = new rig[k];
        k = addRig();
        searchComponents();
        printAll();
    }

    private int addRig() {
        for (int i = 0; i < n; i++) {
            m[i] = new rig(i, i);
        }
        
        for (int i = n; i < k; i++) {
            int temp1 = in.nextInt();
            if (temp1 < 0) {
                return i;
            }
            int temp2 = in.nextInt();
            if (temp2 < 0) {
                return i;
            }
            m[i] = new rig(temp1, temp2);
        }
        return k;
    }

    private void searchComponents() {
        for (int i = 0; i < k; i++) {
            if(m[i].seeFlag() == 1) continue;
            HashSet<Integer> x = new HashSet<>();
            x.add(m[i].get_x());
            x.add(m[i].get_y());
            for (int j = 0; j < x.size(); j++) {
                for (int q = i; q < k; q++){
                    if (x.contains(m[q].get_x())){
                        x.add(m[q].get_y());
                        m[q].setFlag();
                    } else if (x.contains(m[q].get_y())) {
                        x.add(m[q].get_x());
                        m[q].setFlag();
                    }
                }
            }
            components.add(x);
        }
    }

    private void printAll() {
        while (components.size() > 0) {
            HashSet<Integer> x = components.removeFirst();
            for (Integer value : x) {
                System.out.print(value + " ");
            }
            System.out.println();
        }
    }
}
