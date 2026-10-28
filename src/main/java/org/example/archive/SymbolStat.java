package org.example.archive;

public final class SymbolStat {

    private final char letter;
    private final long count;
    private final double probability;
    private String huffmanCode;

    public SymbolStat(char letter, long count, double probability) {
        this.letter = letter;
        this.count = count;
        this.probability = probability;
        this.huffmanCode = "";
    }

    public char letter() {
        return letter;
    }

    public long count() {
        return count;
    }

    public double probability() {
        return probability;
    }

    public double percent() {
        return probability * 100.0;
    }

    public String huffmanCode() {
        return huffmanCode;
    }

    public void setHuffmanCode(String huffmanCode) {
        this.huffmanCode = huffmanCode;
    }

    public int huffmanLength() {
        return huffmanCode.length();
    }

    /**
     * Вклад символа в среднюю длину кода: (Процент / 100) * Длина = p_i * l_i.
     */
    public double lengthContribution() {
        return probability * huffmanLength();
    }

    /**
     * Знаки вроде пробела или кавычек в отчёте иначе сливаются с колонками.
     */
    public String letterDisplay() {
        return switch (letter) {
            case ' ' -> "(пробел)";
            case '\t' -> "(таб)";
            case '\n' -> "(перевод)";
            case '\r' -> "(CR)";
            default -> String.valueOf(letter);
        };
    }
}
