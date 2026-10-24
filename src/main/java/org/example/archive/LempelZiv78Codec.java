package org.example.archive;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Словарный метод Лемпеля–Зива LZ78: растущий словарь фраз.
 * На каждом шаге берём самый длинный уже известный префикс w и следующий символ c,
 * выдаём пару (индекс(w), c) и заносим w+c в словарь.
 * Это не «классическое» ДП, но оптимальный жадный разбор по уже построенному словарю.
 */
public class LempelZiv78Codec {

    /**
     * Кодирует буквенную строку. Индекс 0 — пустая фраза.
     */
    public EncodeResult encode(String letters) {
        Map<String, Integer> dictionary = new LinkedHashMap<>();
        dictionary.put("", 0);
        List<Token> tokens = new ArrayList<>();
        String phrase = "";
        for (int i = 0; i < letters.length(); i++) {
            String next = phrase + letters.charAt(i);
            if (dictionary.containsKey(next)) {
                phrase = next;
                continue;
            }
            tokens.add(new Token(dictionary.get(phrase), letters.charAt(i)));
            dictionary.put(next, dictionary.size());
            phrase = "";
        }
        if (!phrase.isEmpty()) {
            char last = phrase.charAt(phrase.length() - 1);
            String prefix = phrase.substring(0, phrase.length() - 1);
            tokens.add(new Token(dictionary.get(prefix), last));
        }
        return new EncodeResult(tokens, dictionary.size());
    }

    /**
     * Восстановление текста по парам (индекс, символ).
     */
    public String decode(List<Token> tokens) {
        List<String> phrases = new ArrayList<>();
        phrases.add("");
        StringBuilder text = new StringBuilder();
        for (Token token : tokens) {
            String restored = phrases.get(token.dictionaryIndex()) + token.nextChar();
            phrases.add(restored);
            text.append(restored);
        }
        return text.toString();
    }

    /**
     * Грубая оценка длины битового кода: индекс словаря + 8 бит на букву
     * (для сравнения с Хаффманом и арифметикой, не канонический формат файла).
     */
    public int estimateBitLength(List<Token> tokens, int dictionarySize) {
        int indexBits = Math.max(1, 32 - Integer.numberOfLeadingZeros(Math.max(1, dictionarySize - 1)));
        return tokens.size() * (indexBits + 8);
    }

    public static final class Token {
        private final int dictionaryIndex;
        private final char nextChar;

        public Token(int dictionaryIndex, char nextChar) {
            this.dictionaryIndex = dictionaryIndex;
            this.nextChar = nextChar;
        }

        public int dictionaryIndex() {
            return dictionaryIndex;
        }

        public char nextChar() {
            return nextChar;
        }

        @Override
        public String toString() {
            return "(" + dictionaryIndex + ", '" + nextChar + "')";
        }
    }

    public static final class EncodeResult {
        private final List<Token> tokens;
        private final int dictionarySize;

        public EncodeResult(List<Token> tokens, int dictionarySize) {
            this.tokens = tokens;
            this.dictionarySize = dictionarySize;
        }

        public List<Token> tokens() {
            return tokens;
        }

        public int dictionarySize() {
            return dictionarySize;
        }
    }
}
