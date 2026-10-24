package org.example.archive;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;

/**
 * Пишет три текстовых отчёта лабораторной: частоты+Хаффман, арифметика, LZ78.
 */
public class ArchiveReportWriter {

    private final ArchiveSettings settings;

    public ArchiveReportWriter(ArchiveSettings settings) {
        this.settings = settings;
    }

    public void writeFrequencyReport(FrequencyTable table, int sourceLines, String huffmanBits) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(settings.frequencyReportFile(), StandardCharsets.UTF_8)) {
            writer.write("Анализ частот букв и код Хаффмана");
            writer.newLine();
            writer.write("Строк текста: " + sourceLines);
            writer.newLine();
            writer.write("Всего символов (буквы + пробел + знаки): " + table.totalLetters());
            writer.newLine();
            writer.write(String.format(Locale.US, "Энтропия H = %.6f бит/букву", table.entropyBits()));
            writer.newLine();
            writer.write(String.format(Locale.US, "Средняя длина кода Хаффмана L = %.6f бит/букву", table.averageHuffmanLength()));
            writer.newLine();
            writer.write(String.format(Locale.US, "Избыточность L - H = %.6f", table.huffmanRedundancy()));
            writer.newLine();
            writer.write("Длина закодированного текста: " + huffmanBits.length() + " бит");
            writer.newLine();
            writer.write("Сортировка: от более редких букв к более частым");
            writer.newLine();
            writer.newLine();
            writer.write(String.format("%-4s %-12s %-12s %-12s %-24s %-8s %-30s",
                    "№", "Символ", "Сколько", "Процент", "Код_Хаффмана", "Длина", "P_в_двоичном(×2)"));
            writer.newLine();
            int rank = 1;
            for (SymbolStat stat : table.stats()) {
                writer.write(String.format(Locale.US, "%-4d %-12s %-12d %-12s %-24s %-8d %-30s",
                        rank,
                        stat.letterDisplay(),
                        stat.count(),
                        table.formatPercent(stat.percent()) + "%",
                        stat.huffmanCode(),
                        stat.huffmanLength(),
                        stat.probabilityBinary()));
                writer.newLine();
                rank++;
            }
        }
    }

    public void writeArithmeticReport(
            FrequencyTable table,
            String sample,
            ArithmeticRangeCodec.EncodeResult encoded,
            String decoded,
            String fractionTrace
    ) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(settings.arithmeticReportFile(), StandardCharsets.UTF_8)) {
            writer.write("Арифметическое кодирование (формулы с доски, 48-бит LO/HI)");
            writer.newLine();
            writer.write("B = HI - LO + 1");
            writer.newLine();
            writer.write("LO = LO + B * LS");
            writer.newLine();
            writer.write("HI = LO + B * HS - 1   (старый LO)");
            writer.newLine();
            writer.write("Совпавший старший бит LO и HI сдвигается в выходной код.");
            writer.newLine();
            writer.newLine();
            writer.write("Интервалы LS/HS по алфавиту:");
            writer.newLine();
            for (SymbolStat stat : table.alphabetOrder()) {
                writer.write(String.format(Locale.US, "  '%s'  LS=%.8f  HS=%.8f  P=%.8f",
                        stat.letterDisplay(),
                        table.lowBound(stat.letter()),
                        table.highBound(stat.letter()),
                        stat.probability()));
                writer.newLine();
            }
            writer.newLine();
            writer.write("Пример перевода вероятности в двоичную дробь (метод ×2), как 0.073 на доске:");
            writer.newLine();
            writer.write(fractionTrace);
            writer.newLine();
            writer.write("Фрагмент текста (" + sample.length() + " букв):");
            writer.newLine();
            writer.write(sample);
            writer.newLine();
            writer.newLine();
            writer.write("Первые шаги кодирования:");
            writer.newLine();
            for (String step : encoded.firstSteps()) {
                writer.write("  " + step);
                writer.newLine();
            }
            writer.newLine();
            writer.write("Длина кода: " + encoded.bits().length() + " бит");
            writer.newLine();
            writer.write(String.format(Locale.US, "Бит на букву: %.6f", encoded.bitsPerLetter()));
            writer.newLine();
            writer.write("Код (префикс): " + prefix(encoded.bits(), 160));
            writer.newLine();
            writer.write("Декодирование совпало: " + sample.equals(decoded));
            writer.newLine();
            if (!sample.equals(decoded)) {
                writer.write("Ожидали: " + sample);
                writer.newLine();
                writer.write("Получили: " + decoded);
                writer.newLine();
            }
        }
    }

    public void writeLempelZivReport(
            String sample,
            LempelZiv78Codec.EncodeResult encoded,
            String decoded,
            int estimatedBits
    ) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(settings.lempelZivReportFile(), StandardCharsets.UTF_8)) {
            writer.write("Словарный алгоритм Лемпеля–Зива LZ78");
            writer.newLine();
            writer.write("Словарь растёт: новая фраза = уже известный префикс + следующий символ.");
            writer.newLine();
            writer.write("Выход — пары (индекс_в_словаре, новый_символ).");
            writer.newLine();
            writer.newLine();
            writer.write("Фрагмент (" + sample.length() + " букв):");
            writer.newLine();
            writer.write(sample);
            writer.newLine();
            writer.newLine();
            writer.write("Размер словаря: " + encoded.dictionarySize());
            writer.newLine();
            writer.write("Число токенов: " + encoded.tokens().size());
            writer.newLine();
            writer.write("Оценка длины кода: " + estimatedBits + " бит");
            writer.newLine();
            writer.write("Декодирование совпало: " + sample.equals(decoded));
            writer.newLine();
            writer.newLine();
            writer.write("Первые 40 токенов:");
            writer.newLine();
            List<LempelZiv78Codec.Token> tokens = encoded.tokens();
            int limit = Math.min(40, tokens.size());
            for (int i = 0; i < limit; i++) {
                writer.write("  " + tokens.get(i).toString());
                writer.newLine();
            }
        }
    }

    private String prefix(String bits, int max) {
        if (bits.length() <= max) {
            return bits;
        }
        return bits.substring(0, max) + "...";
    }
}
