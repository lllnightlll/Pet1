package org.example;

import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Labs extends Bot{
    private Map<Long, State> userStates = new HashMap<>();

    protected void sort(List<Integer> arrToSort) {
    }

    protected enum State {
        WAIT_INPUT, READY
    }
    protected String text;

    public void onUpdateReceived(Update update) {
        text = getTypedText(update);
        State currentState = userStates.getOrDefault(chatId, State.WAIT_INPUT);
        if (currentState == State.WAIT_INPUT) {
            message(chatId, "Введите строку:");
            userStates.put(chatId, State.READY);
        } else if (currentState == State.READY) {
            if (text != null && !text.isEmpty()) {
                handleInput(text, chatId, update);
            } else {
                message(chatId, "Введите строку ещё раз:");
            }
            userStates.put(chatId, State.WAIT_INPUT);
        }
    }

    protected abstract void handleInput(String text, long userId, Update update);
}
