package org.example;

import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.HashMap;
import java.util.Map;

public class Lab12 extends Labs {
    enum Stage {WAIT_SIZE, WAIT_ARRAY, WAIT_DELETE, COMPLETE}

    private Map<Long, Stage> userStage = new HashMap<>();
    private Map<Long, Integer> sizeMap = new HashMap<>();
    private Map<Long, int[]> arrays = new HashMap<>();
    private Map<Long, Integer> idx = new HashMap<>();
    private Map<Long, Integer> delValue = new HashMap<>();
    private Map<Long, Integer> sizeEnd = new HashMap<>();

    @Override
    protected void handleInput(String text, long chatId, Update update) {
        Stage stage = userStage.getOrDefault(chatId, Stage.WAIT_SIZE);

        switch (stage) {
            case WAIT_SIZE:
                try {
                    int n = Integer.parseInt(text);
                    if (n < 0 || n > 100) throw new Exception("Wrong number");
                    sizeMap.put(chatId, n);
                    arrays.put(chatId, new int[n]);
                    idx.put(chatId, 0);
                    userStage.put(chatId, Stage.WAIT_ARRAY);
                    message(chatId, "Enter number 1");
                } catch (Exception ex) {
                    message(chatId, "invalid size, try again");
                }
                break;

            case WAIT_ARRAY:
                int i = idx.get(chatId);
                int n = sizeMap.get(chatId);
                int arr[] = arrays.get(chatId);
                try {
                    arr[i] = Integer.parseInt(text);
                    if (arr[i] < 0 || arr[i] > 50) throw new Exception("Wrong number");
                    i++;
                    idx.put(chatId, i);
                    if (i < n) {
                        message(chatId, "Enter number " + (i + 1));
                    } else {
                        userStage.put(chatId, Stage.WAIT_DELETE);
                        message(chatId, "please, write delete value");
                    }
                } catch (Exception ex) {
                    message(chatId, "invalid value, try again");
                }
                break;

            case WAIT_DELETE:
                try {
                    int val = Integer.parseInt(text);
                    if (val < 0 || val > 100) throw new Exception("Wrong number");
                    delValue.put(chatId, val);
                    int k = removeInplace(arrays.get(chatId), sizeMap.get(chatId), val);
                    sizeEnd.put(chatId, k);
                    userStage.put(chatId, Stage.COMPLETE);
                    print(chatId, k);
                } catch (Exception ex) {
                    message(chatId, "invalid delete value, try again");
                }
                break;

            case COMPLETE:
                message(chatId, "Completed. Write /start to start again");
        }
    }

    private int removeInplace(int[] array, int n, int val) {
        int newSize = 0;
        for (int i = 0; i < n; i++) {
            if (array[i] != val) {
                array[newSize] = array[i];
                newSize++;
            }
        }
        return newSize;
    }

    private void print(long chatId, int k) {
        int[] arr = arrays.get(chatId);
        StringBuilder sb = new StringBuilder(k + " arr: ");
        for (int i = 0; i < k; i++) {
            sb.append(arr[i]);
            if (i < k - 1) sb.append(", ");
        }
        message(chatId, sb.toString());
    }
}
