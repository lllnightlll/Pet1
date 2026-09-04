package org.example.TBankSummer2026;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class task2 {
    public static void main(String[] args) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        int n = Integer.parseInt(in.readLine().trim());

        Map<String, Integer> count = new HashMap<>();
        int max = 0;
        for (int i = 0; i < n; i++) {
            String s = in.readLine();
            int c = count.merge(s, 1, Integer::sum);
            if (c > max) {
                max = c;
            }
        }

        List<String> answer = new ArrayList<>();
        for (Map.Entry<String, Integer> e : count.entrySet()) {
            if (e.getValue() == max) {
                answer.add(e.getKey());
            }
        }
        Collections.sort(answer);

        StringBuilder out = new StringBuilder();
        for (String s : answer) {
            out.append(s).append('\n');
        }
        System.out.print(out);
    }
}
