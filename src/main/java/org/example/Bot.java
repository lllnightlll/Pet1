package org.example;

//import org.example.sorting.Lab21;
//import org.example.sorting.Lab22;
//import org.example.sorting.Lab23;
import io.github.cdimascio.dotenv.Dotenv;
import org.example.sorting.MainSort;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Bot implements LongPollingSingleThreadUpdateConsumer {
    private TelegramClient telegramClient = new OkHttpTelegramClient(Dotenv.load().get("TOKEN"));
    private Map<Long, String> userStates = new HashMap<>();
    private Map<Long, Labs> chosenLabs = new HashMap<>();
    protected Long chatId;

    @Override
    public void consume(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            chatId = update.getMessage().getChatId();
            String state = userStates.get(chatId);
            Labs lab = chosenLabs.get(chatId);
            if (Objects.equals(getTypedText(update), "/start")) {
                message(chatId, "Choice lab");
                userStates.put(chatId, "waiting");
                chosenLabs.remove(chatId);
            }
            else if ("waiting".equals(state)) {
                switch (getTypedText(update)) {
                    case "end":
                        userStates.remove(chatId);
                        chosenLabs.remove(chatId);
                        message(chatId, "Send /start to start");
                        break;
                    case "lab11":
                        lab = new Lab11();
                        chosenLabs.put(chatId, lab);
                        message(chatId, "Введите входные данные: ");
                        userStates.put(chatId, "processing");
                        break;
                    case "lab12":
                        lab = new Lab12();
                        chosenLabs.put(chatId, lab);
                        message(chatId, "please, write size of array");
                        userStates.put(chatId, "processing");
                        break;
                    case "sort":
                        @SuppressWarnings("rawtypes")
                        Labs sort_lab = new MainSort();
                        lab = sort_lab;
                        chosenLabs.put(chatId, lab);
                        message(chatId,"please, write number");
                        userStates.put(chatId, "processing");
                        break;
                    default:
                        System.out.println("Wrong lab");
                        break;
                }
            }
            else if ("processing".equals(state) && lab != null) {
                lab.handleInput(getTypedText(update), chatId, update);
            }
        }
    }

    protected void message(Long chatId, String text) {
        SendMessage sendMessage = new SendMessage(chatId.toString(), text);
        try {
            telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    protected String getTypedText(Update update) {
        return update.getMessage().getText();
    }
}
