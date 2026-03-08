//import org.example.Dinamyc.Bagel;
//import org.example.Dinamyc.NVP;
//import org.example.Dinamyc.NOP;

package org.example.Dinamyc;

public class DinamicMain {
    void main() {
        // int[][] x = { {0, 1, 1, 0}, {0, 0, 2, 3}, {4, -2, 1, -1}, {-3, 8, -6, 0} };
        // new Bagel(x, 4, 4);

        // int[] x = {7, 1, 4, 3, 3, 5, 4, 8, 6, 9};
        // NVP.nvp(10, x);

        // char[] x = {'A', 'B', 'C', 'D', 'E'};
        // char[] y = {'D', 'C', 'D', 'A'};
        // System.out.println(NOP.nop(x, y));

        // Bagel x = new Bagel("roguelike-input.csv", "roguelike-output.txt");
        // NVP x = new NVP("lis-input.txt", "lis-output.txt");
        new NOP("lcs-input.txt");
    }
}
