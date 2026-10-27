package org.example.archive;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class TextDocumentLoader {

    private final ArchiveSettings settings;
    private final PdfTextExtractor pdfTextExtractor;

    public TextDocumentLoader(ArchiveSettings settings, PdfTextExtractor pdfTextExtractor) {
        this.settings = settings;
        this.pdfTextExtractor = pdfTextExtractor;
    }

    public String loadLimitedText() throws IOException {
        if (!Files.exists(settings.sourcePdf())) {
            throw new IOException("Не найден PDF: " + settings.sourcePdf());
        }
        String raw = pdfTextExtractor.extract(settings.sourcePdf());
        List<String> lines = splitLines(raw);
        int limit = Math.min(ArchiveSettings.MAX_TEXT_LINES, lines.size());
        List<String> head = lines.subList(0, limit);
        String limited = String.join("\n", head);
        writeExtractedCopy(head);
        return limited;
    }

    private List<String> splitLines(String raw) {
        String[] parts = raw.split("\\R", -1);
        List<String> lines = new ArrayList<>();
        for (String part : parts) {
            // Пустые Td-сдвиги вёрстки дают пачки пробелов; для частоты и отчётов
            // оставляем один пробел между словами.
            String trimmed = part.strip().replaceAll("\\s+", " ");
            if (trimmed.isEmpty()) {
                continue;
            }
            wrapLine(trimmed, 80, lines);
        }
        return lines;
    }

    private void wrapLine(String paragraph, int width, List<String> lines) {
        int from = 0;
        while (from < paragraph.length()) {
            int to = Math.min(paragraph.length(), from + width);
            if (to < paragraph.length()) {
                int space = paragraph.lastIndexOf(' ', to);
                if (space > from + width / 2) {
                    to = space;
                }
            }
            String chunk = paragraph.substring(from, to).strip();
            if (!chunk.isEmpty()) {
                lines.add(chunk);
            }
            from = to;
            while (from < paragraph.length() && paragraph.charAt(from) == ' ') {
                from++;
            }
        }
    }

    private void writeExtractedCopy(List<String> lines) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(settings.extractedTextFile(), StandardCharsets.UTF_8)) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        }
    }
}
