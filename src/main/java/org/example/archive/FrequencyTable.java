package org.example.archive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FrequencyTable {

    private final List<SymbolStat> stats;
    private final long totalLetters;
    private final double entropyBits;

    private FrequencyTable(List<SymbolStat> stats, long totalLetters, double entropyBits) {
        this.stats = stats;
        this.totalLetters = totalLetters;
        this.entropyBits = entropyBits;
    }

    public static FrequencyTable fromText(String text) {
        Map<Character, Long> counts = new LinkedHashMap<>();
        long total = 0;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            Character symbol = asCountedSymbol(cp);
            if (symbol == null) {
                continue;
            }
            counts.merge(symbol, 1L, Long::sum);
            total++;
        }
        if (total == 0) {
            throw new IllegalStateException("В тексте не найдено ни одной буквы или знака. Проверьте извлечение PDF.");
        }
        List<SymbolStat> rows = new ArrayList<>();
        double entropy = 0.0;
        for (Map.Entry<Character, Long> entry : counts.entrySet()) {
            double p = entry.getValue() / (double) total;
            rows.add(new SymbolStat(entry.getKey(), entry.getValue(), p));
            entropy += -p * log2(p);
        }
        rows.sort(Comparator
                .comparingLong(SymbolStat::count)
                .thenComparing(SymbolStat::letter));
        return new FrequencyTable(Collections.unmodifiableList(rows), total, entropy);
    }

    public List<SymbolStat> stats() {
        return stats;
    }

    public long totalLetters() {
        return totalLetters;
    }

    public double entropyBits() {
        return entropyBits;
    }

    /**
     * Средняя длина кода Хаффмана: сумма p_i * |code_i|.
     */
    public double averageHuffmanLength() {
        double sum = 0.0;
        for (SymbolStat stat : stats) {
            sum += stat.probability() * stat.huffmanLength();
        }
        return sum;
    }

    public double huffmanRedundancy() {
        return averageHuffmanLength() - entropyBits;
    }

    /**
     * Нижняя граница интервала символа (LS на доске) — сумма вероятностей всех, кто левее в алфавитном порядке.
     */
    public double lowBound(char letter) {
        double acc = 0.0;
        for (SymbolStat stat : alphabetOrder()) {
            if (stat.letter() == letter) {
                return acc;
            }
            acc += stat.probability();
        }
        throw new IllegalArgumentException("Буквы нет в таблице: " + letter);
    }

    /**
     * Верхняя граница интервала символа (HS на доске).
     */
    public double highBound(char letter) {
        return lowBound(letter) + probabilityOf(letter);
    }

    public double probabilityOf(char letter) {
        for (SymbolStat stat : stats) {
            if (stat.letter() == letter) {
                return stat.probability();
            }
        }
        throw new IllegalArgumentException("Буквы нет в таблице: " + letter);
    }

    public boolean contains(char letter) {
        for (SymbolStat stat : stats) {
            if (stat.letter() == letter) {
                return true;
            }
        }
        return false;
    }

    public List<SymbolStat> alphabetOrder() {
        List<SymbolStat> copy = new ArrayList<>(stats);
        copy.sort(Comparator.comparing(SymbolStat::letter));
        return copy;
    }

    /**
     * Текст, суженный до алфавита таблицы: те же буквы и знаки, что пошли в Хаффман.
     * Этот же поток кодируется арифметикой по LS/HS.
     */
    public String countedSymbols(String text) {
        StringBuilder symbols = new StringBuilder();
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            Character symbol = asCountedSymbol(cp);
            if (symbol != null) {
                symbols.append(symbol);
            }
        }
        return symbols.toString();
    }

    public String formatPercent(double percent) {
        return String.format(Locale.US, "%.4f", percent);
    }

    private static Character asCountedSymbol(int cp) {
        if (!Character.isValidCodePoint(cp) || Character.charCount(cp) != 1) {
            return null;
        }
        char symbol = (char) cp;
        if (symbol == '\u00A0') {
            return ' ';
        }
        if (symbol == ' ') {
            return ' ';
        }
        if (Character.isLetter(symbol)) {
            char lower = Character.toLowerCase(symbol);
            if (Character.UnicodeBlock.of(lower) == Character.UnicodeBlock.CYRILLIC) {
                return lower;
            }
            return null;
        }
        return isPunctuation(symbol) ? symbol : null;
    }

    private static boolean isPunctuation(char symbol) {
        int type = Character.getType(symbol);
        return type == Character.CONNECTOR_PUNCTUATION
                || type == Character.DASH_PUNCTUATION
                || type == Character.START_PUNCTUATION
                || type == Character.END_PUNCTUATION
                || type == Character.INITIAL_QUOTE_PUNCTUATION
                || type == Character.FINAL_QUOTE_PUNCTUATION
                || type == Character.OTHER_PUNCTUATION;
    }

    private static double log2(double value) {
        return Math.log(value) / Math.log(2.0);
    }
}
