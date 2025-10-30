package org.example;

import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.ArrayDeque;
import java.util.Deque;

public class Lab11 extends Labs {
    @Override
    protected void handleInput(String text, long chatId, Update update) {

        String x = text;
        int temp = x.length();
        if (temp % 2 == 1) message(chatId,"False");
        else {
            Deque<Character> str = new ArrayDeque<>(temp / 2);
            boolean answer = true;
            for (int i = 0; i < temp; i++) {
                char y = x.charAt(i);
                if (y == '(' || y == '{' || y == '[') {
                    if (str.size() == temp / 2) break;
                    str.push(y);
                } else if (str.isEmpty()) {
                    answer = false;
                    break;
                } else {
                    char z = str.pop();
                    if (z != '(' && y == ')' ||
                            z != '{' && y == '}' ||
                            z != '[' && y == ']') {
                        break;
                    }
                }
            }
            if (str.isEmpty() && answer) message(chatId,"True");
            else {
                message(chatId,"False");
            }
        }
    }
}
