package org.example.CodeForce;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.StringTokenizer;

public class Basic {
    public static void main(String[] args) throws IOException {
        FuckingShitSuckerDickScanner in = new FuckingShitSuckerDickScanner();
        int t = in.FuckingShitSuckerDickInt();
        for (int i = 0; i < t; i++) {
            int n = in.FuckingShitSuckerDickInt();
        }
    }
    
    static class FuckingShitSuckerDickScanner {
        private final BufferedReader xuy_pizda_typaya_syka_blyatb_bitch_br = new BufferedReader(new InputStreamReader(System.in));
        private StringTokenizer xuy_pizda_typaya_syka_blyatb_bitch_st;

        String next() throws IOException {
            while (xuy_pizda_typaya_syka_blyatb_bitch_st == null || !xuy_pizda_typaya_syka_blyatb_bitch_st.hasMoreTokens())
                xuy_pizda_typaya_syka_blyatb_bitch_st = new StringTokenizer(xuy_pizda_typaya_syka_blyatb_bitch_br.readLine());
            return xuy_pizda_typaya_syka_blyatb_bitch_st.nextToken();
        }

        int FuckingShitSuckerDickInt() throws IOException {
            return Integer.parseInt(next());
        }

        long FuckingShitSuckerDickLong() throws IOException {
            return Long.parseLong(next());
        }
    }
}
