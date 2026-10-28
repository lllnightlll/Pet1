package org.example.archive;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;

/**
 * Пишет отчёты лабораторной: частоты+Хаффман и арифметическое кодирование.
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
            writer.write("Символы с числом вхождений меньше "
                    + ArchiveSettings.HUFFMAN_MIN_SYMBOL_COUNT
                    + " в таблицу не входят.");
            writer.newLine();
            writer.write("Столбец Средняя длина = (Процент/100)*Длина — вклад буквы в среднюю длину L.");
            writer.newLine();
            writer.newLine();
            writer.write(String.format("%-4s %-12s %-12s %-12s %-24s %-8s %-14s",
                    "№", "Символ", "Сколько", "Процент", "Код_Хаффмана", "Длина", "Средняя длина"));
            writer.newLine();
            int rank = 1;
            for (SymbolStat stat : table.stats()) {
                if (stat.count() < ArchiveSettings.HUFFMAN_MIN_SYMBOL_COUNT) {
                    continue;
                }
                writer.write(String.format(Locale.US, "%-4d %-12s %-12d %-12s %-24s %-8d %-14s",
                        rank,
                        stat.letterDisplay(),
                        stat.count(),
                        table.formatPercent(stat.percent()) + "%",
                        stat.huffmanCode(),
                        stat.huffmanLength(),
                        table.formatPercent(stat.lengthContribution())));
                writer.newLine();
                rank++;
            }
        }
    }

    public void writeArithmeticReport(
            FrequencyTable table,
            String sample,
            ArithmeticRangeCodec.EncodeResult encoded,
            String decoded
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
            writer.write("Код (все " + encoded.bits().length() + " бит):");
            writer.newLine();
            writeWrapped(writer, encoded.bits(), 80);
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

    /**
     * Пишет длинную битовую строку по строкам фиксированной ширины,
     * чтобы отчёт можно было открыть в редакторе.
     */
    private void writeWrapped(BufferedWriter writer, String bits, int width) throws IOException {
        int from = 0;
        while (from < bits.length()) {
            int to = Math.min(bits.length(), from + width);
            writer.write(bits.substring(from, to));
            writer.newLine();
            from = to;
        }
    }
}
