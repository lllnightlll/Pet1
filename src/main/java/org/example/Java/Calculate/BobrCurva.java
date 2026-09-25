package org.example.Java.Calculate;

import java.util.Scanner;

public class BobrCurva {
    @SuppressWarnings("resource")
    private Scanner in = new Scanner(System.in);
    @SuppressWarnings("unused")
    private String mathString;

    BobrCurva() {
        System.out.println("Halo, I'm Bobr Curva, ya perdole, what is your math question:");
        mathString = in.nextLine();
    }
}
