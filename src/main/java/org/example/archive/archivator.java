package org.example.archive;

import java.io.IOException;

/**
 * Лабораторная по сжатию:
 * 1) частоты букв книги + код Хаффмана + энтропия;
 * 2) арифметическое кодирование (доска: LS/HS, LO/HI).
 *
 * Запуск: main этого класса. Отчёты появятся рядом с PDF в том же каталоге.
 */
public class archivator {

    public static void main(String[] args) {
        try {
            new archivator().run();
        } catch (Exception exception) {
            exception.printStackTrace();
            System.exit(1);
        }
    }

    public void run() throws IOException {
        ArchiveSettings settings = ArchiveSettings.fromCodeLocation();
        TextDocumentLoader loader = new TextDocumentLoader(settings, new PdfTextExtractor());
        String text = loader.loadLimitedText();
        int lineCount = text.isEmpty() ? 0 : text.split("\\R", -1).length;

        FrequencyTable table = FrequencyTable.fromText(text);
        HuffmanCodec huffmanCodec = new HuffmanCodec();
        huffmanCodec.assignCodes(table);

        String allSymbols = table.countedSymbols(text);
        String huffmanBits = huffmanCodec.encode(allSymbols, table);

        ArchiveReportWriter reports = new ArchiveReportWriter(settings);
        reports.writeFrequencyReport(table, lineCount, huffmanBits);

        String arithmeticSample = allSymbols.substring(
                0, Math.min(ArchiveSettings.ARITHMETIC_SAMPLE_LENGTH, allSymbols.length()));
        ArithmeticRangeCodec arithmetic = new ArithmeticRangeCodec(ArchiveSettings.ARITHMETIC_PRECISION_BITS);
        ArithmeticRangeCodec.EncodeResult arithmeticEncoded = arithmetic.encode(arithmeticSample, table);
        String arithmeticDecoded = arithmetic.decode(arithmeticEncoded.bits(), arithmeticSample.length(), table);
        reports.writeArithmeticReport(table, arithmeticSample, arithmeticEncoded, arithmeticDecoded);

        System.out.println("Готово.");
        System.out.println("Текст:     " + settings.extractedTextFile().toAbsolutePath());
        System.out.println("Хаффман:   " + settings.frequencyReportFile().toAbsolutePath());
        System.out.println("Арифметика:" + settings.arithmeticReportFile().toAbsolutePath());
        System.out.println("Букв: " + table.totalLetters()
                + ", энтропия: " + String.format(java.util.Locale.US, "%.4f", table.entropyBits())
                + ", L_хаффман: " + String.format(java.util.Locale.US, "%.4f", table.averageHuffmanLength()));
        System.out.println("Арифметика совпала: " + arithmeticSample.equals(arithmeticDecoded));
    }
}
