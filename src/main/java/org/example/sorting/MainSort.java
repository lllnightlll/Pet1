package org.example.sorting;

import org.example.Labs;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.*;

public class MainSort<T extends  Comparable<T>> extends Labs {
    enum Stage {WAIT_LAB, WAIT_SIZE, WAIT_ARRAY, SORT}

    Map<Long, MainSort.Stage> userStage = new HashMap<>();
    Map<Long, Session<?>> sessions = new HashMap<>();

    private Object parseAuto(String text) {
        try {
            return Integer.parseInt(text);
        } catch (Exception ignored) {}

        try {
            return UUID.fromString(text);
        } catch (Exception ignored) {}

        try {
            return LocalDateTime.parse(text);
        } catch (Exception ignored) {}

        String[] parts = text.trim().split("\\s+");
        if (parts.length == 2) {
            try {
                String name = parts[0];
                int age = Integer.parseInt(parts[1]);
                return new Student(name, age);
            } catch (Exception ignored) {}
        }

        return text;
    }

    @Override
    protected void handleInput(String text, long chatId, Update update) {
        MainSort.Stage stage = userStage.getOrDefault(chatId, MainSort.Stage.WAIT_LAB);

        switch (stage) {
            case WAIT_LAB:
                try {
                    switch (text) {
                        case "1" -> sessions.put(chatId, new Session<>(new InsertionSort<Integer>()));
                        case "2" -> sessions.put(chatId, new Session<>(new MergeSort<Integer>()));
                        case "3" -> sessions.put(chatId, new Session<>(new StudentSort()));
                        case "4" -> sessions.put(chatId, new Session<>(new CountingSort()));
                        case "5" -> sessions.put(chatId, new Session<>(new RadixSort()));
                        default -> throw new Exception("Wrong lab");
                    }
                    userStage.put(chatId, MainSort.Stage.WAIT_SIZE);
                    message(chatId, "write size");
                    break;
                } catch (Exception ex) {
                    message(chatId, "invalid lab, try again");
                }

            case WAIT_SIZE:
                try {
                    int N = Integer.parseInt(text);
                    if (N < 0 || N >= 100000) throw new Exception("Wrong size");
                    sessions.get(chatId).size = N;
                    userStage.put(chatId, MainSort.Stage.WAIT_ARRAY);
                    message(chatId, "Enter data 1");
                } catch (Exception ex) {
                    message(chatId, "invalid size, try again");
                }
                break;

            case WAIT_ARRAY:
                Session<?> session = sessions.get(chatId);
                if (session == null) return;
                int i = session.idx;
                int N = session.size;
                try {
                    if (session.sorter instanceof StudentSort) {
                        String[] parts = text.trim().split("\\s+");
                        if (parts.length == 2) {
                            String name = parts[0];
                            int age = Integer.parseInt(parts[1]);
                            ((Session<Student>) session).values.add(new Student(name, age));
                        }
                    } else if (session.sorter instanceof InsertionSort) {
                        Object parsed = parseAuto(text);
                        if (parsed instanceof Integer && session != null && session.values instanceof List<?>) {
                            ((Session<Integer>) session).values.add((Integer) parsed);
                        } else if (parsed instanceof UUID) {
                            ((Session<UUID>) session).values.add((UUID) parsed);
                        } /*else if (parsed instanceof LocalDateTime) {
                            ((Session<LocalDateTime>) session).values.add((LocalDateTime) parsed);
                        }*/ else if (parsed instanceof Student) {
                            ((Session<Student>) session).values.add((Student) parsed);
                        } else if (parsed instanceof String) {
                            ((Session<String>) session).values.add((String) parsed);
                        }
                    } else if (session.sorter instanceof MergeSort) {
                        ((Session<Integer>) session).values.add(Integer.parseInt(text));
                    } else if (session.sorter instanceof CountingSort) {
                        int parse= Integer.parseInt(text);
                        if(parse < 0 || parse > 127) throw new Exception("Wrong number");
                        ((Session<Integer>) session).values.add(parse);
                    } else if (session.sorter instanceof RadixSort) {
                        ((Session<BigInteger>) session).values.add(new BigInteger(text));
                    }

                    i++;
                    sessions.get(chatId).idx = i;
                    if (i < N) message(chatId, "Enter data " + (i + 1));
                    else {
                        userStage.put(chatId, MainSort.Stage.SORT);
                        message(chatId, "Send any msg to sort");
                    }
                } catch (Exception ex) {
                    message(chatId, "invalid value, try again");
                }
                break;

            case SORT:
                Session<?> sessionSort = sessions.get(chatId);
                if (sessionSort == null) return;
                message(chatId, "Input: " + print(sessionSort.values));
                Object first = sessionSort.values.get(0);
                if (first instanceof Integer) {
                    if (sessionSort.sorter instanceof MergeSort) {
                        ((Sorting<Integer>) sessionSort.sorter).sort((List<Integer>) sessionSort.values);
                    } else if (sessionSort.sorter instanceof InsertionSort) {
                        ((Sorting<Integer>) sessionSort.sorter).sort((List<Integer>) sessionSort.values);
                    } else if (sessionSort.sorter instanceof CountingSort) {
                        ((Sorting<Integer>) sessionSort.sorter).sort((List<Integer>) sessionSort.values);
                    }
                } else if (first instanceof BigInteger){
                    ((Sorting<BigInteger>) sessionSort.sorter).sort((List<BigInteger>) sessionSort.values);
                } else if (first instanceof String && sessionSort.sorter instanceof InsertionSort) {
                    ((Sorting<String>) sessionSort.sorter).sort((List<String>) sessionSort.values);
                } else if (first instanceof Student && sessionSort.sorter instanceof StudentSort) {
                    ((Sorting<Student>) sessionSort.sorter).sort((List<Student>) sessionSort.values);
                } else if (first instanceof UUID && sessionSort.sorter instanceof InsertionSort) {
                    ((Sorting<UUID>) sessionSort.sorter).sort((List<UUID>) sessionSort.values);
                } /*else if (first instanceof LocalDateTime && sessionSort.sorter instanceof InsertionSort) {
                    ((Sorting<LocalDateTime>) sessionSort.sorter).sort((List<LocalDateTime>) sessionSort.values);
                }*/
                message(chatId, "Sorted: " + print(sessionSort.values));
                break;
        }
    }

    private <T> String print(List<T> arr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.size(); i++) {
            sb.append(arr.get(i).toString());
            if (i < arr.size() - 1) sb.append(", ");
        }
        return sb.toString();
    }
}