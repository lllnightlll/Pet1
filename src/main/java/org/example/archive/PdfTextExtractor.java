package org.example.archive;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class PdfTextExtractor {

    private static final byte[] STREAM_MARK = "stream".getBytes(StandardCharsets.ISO_8859_1);
    private static final byte[] END_STREAM_MARK = "endstream".getBytes(StandardCharsets.ISO_8859_1);

    private static final Pattern FONT_NAME_AND_BASE = Pattern.compile(
            "/Name /(F\\d+\\+\\d)\\s*/BaseFont /([^/\\s\\r\\n]+)");
    private static final Pattern CMAP_NAME = Pattern.compile("/CMapName //([^\\s]+)\\s+def");
    private static final Pattern BF_CHAR = Pattern.compile("<([0-9A-Fa-f]+)>\\s*<([0-9A-Fa-f]+)>");
    private static final Pattern FONT_SELECT = Pattern.compile("/(F\\d+\\+\\d)\\s+[0-9.]+\\s+Tf");

    public String extract(Path pdfPath) throws IOException {
        byte[] pdf = Files.readAllBytes(pdfPath);
        Map<String, Map<Integer, Integer>> cmapsByBaseFont = new HashMap<>();
        StringBuilder pages = new StringBuilder();

        int from = 0;
        while (true) {
            int streamAt = indexOf(pdf, STREAM_MARK, from);
            if (streamAt < 0) {
                break;
            }
            int dataStart = skipEol(pdf, streamAt + STREAM_MARK.length);
            int endAt = indexOf(pdf, END_STREAM_MARK, dataStart);
            if (endAt < 0) {
                break;
            }
            String content = inflateOrRaw(slice(pdf, dataStart, endAt));
            if (content.contains("begincmap")) {
                parseCmap(content, cmapsByBaseFont);
            } else if (looksLikePageContent(content)) {
                pages.append(content).append('\n');
            }
            from = endAt + END_STREAM_MARK.length;
        }

        Map<String, Map<Integer, Integer>> cmapsByFontName = bindFontsToCmaps(pdf, cmapsByBaseFont);
        return collectPageText(pages.toString(), cmapsByFontName);
    }

    private Map<String, Map<Integer, Integer>> bindFontsToCmaps(
            byte[] pdf,
            Map<String, Map<Integer, Integer>> cmapsByBaseFont
    ) {
        String ascii = new String(pdf, StandardCharsets.ISO_8859_1);
        Map<String, Map<Integer, Integer>> byFont = new HashMap<>();
        Matcher matcher = FONT_NAME_AND_BASE.matcher(ascii);
        while (matcher.find()) {
            String fontName = matcher.group(1);
            String baseFont = matcher.group(2);
            Map<Integer, Integer> cmap = cmapsByBaseFont.get(baseFont);
            if (cmap != null) {
                byFont.put(fontName, cmap);
            }
        }
        if (byFont.isEmpty() && !cmapsByBaseFont.isEmpty()) {
            byFont.put("F1+1", cmapsByBaseFont.values().iterator().next());
        }
        return byFont;
    }

    private void parseCmap(String cmapText, Map<String, Map<Integer, Integer>> cmapsByBaseFont) {
        Matcher nameMatcher = CMAP_NAME.matcher(cmapText);
        String baseFont = nameMatcher.find() ? nameMatcher.group(1) : "unknown";
        Map<Integer, Integer> map = new HashMap<>();
        Matcher bf = BF_CHAR.matcher(cmapText);
        while (bf.find()) {
            int src = Integer.parseInt(bf.group(1), 16);
            int dst = Integer.parseInt(bf.group(2), 16);
            map.put(src, dst);
        }
        cmapsByBaseFont.put(baseFont, map);
    }

    private String collectPageText(String allContent, Map<String, Map<Integer, Integer>> cmapsByFontName) {
        StringBuilder text = new StringBuilder();
        Map<Integer, Integer> currentCmap = defaultCmap(cmapsByFontName);
        Matcher fontSelector = FONT_SELECT.matcher(allContent);
        int i = 0;
        while (i < allContent.length()) {
            if (allContent.charAt(i) == '/') {
                fontSelector.region(i, allContent.length());
                if (fontSelector.lookingAt()) {
                    Map<Integer, Integer> next = cmapsByFontName.get(fontSelector.group(1));
                    if (next != null) {
                        currentCmap = next;
                    }
                    i = fontSelector.end();
                    continue;
                }
            }
            if (allContent.charAt(i) == '(') {
                PdfString parsed = readPdfString(allContent, i);
                int after = skipSpacesIndex(allContent, parsed.endIndex);
                if (startsWith(allContent, after, "Tj")
                        || startsWith(allContent, after, "'")
                        || startsWith(allContent, after, "\"")) {
                    text.append(mapBytes(parsed.rawBytes, currentCmap));
                    if (startsWith(allContent, after, "'") || startsWith(allContent, after, "\"")) {
                        text.append('\n');
                    }
                }
                i = parsed.endIndex;
                continue;
            }
            if (allContent.charAt(i) == '[') {
                int close = findMatchingBracket(allContent, i);
                if (close > i) {
                    int after = skipSpacesIndex(allContent, close + 1);
                    if (startsWith(allContent, after, "TJ")) {
                        text.append(collectFromArray(allContent.substring(i + 1, close), currentCmap));
                    }
                    i = close;
                    continue;
                }
            }
            if (startsWith(allContent, i, "T*") || startsWith(allContent, i, "Td")) {
                text.append('\n');
            }
            i++;
        }
        return text.toString();
    }

    private Map<Integer, Integer> defaultCmap(Map<String, Map<Integer, Integer>> cmapsByFontName) {
        Map<Integer, Integer> regular = cmapsByFontName.get("F1+1");
        if (regular != null) {
            return regular;
        }
        if (!cmapsByFontName.isEmpty()) {
            return cmapsByFontName.values().iterator().next();
        }
        return Map.of();
    }

    private String collectFromArray(String arrayBody, Map<Integer, Integer> cmap) {
        StringBuilder text = new StringBuilder();
        int i = 0;
        while (i < arrayBody.length()) {
            if (arrayBody.charAt(i) == '(') {
                PdfString parsed = readPdfString(arrayBody, i);
                text.append(mapBytes(parsed.rawBytes, cmap));
                i = parsed.endIndex;
                continue;
            }
            if (arrayBody.charAt(i) == '-' || Character.isDigit(arrayBody.charAt(i))) {
                int start = i;
                if (arrayBody.charAt(i) == '-') {
                    i++;
                }
                while (i < arrayBody.length() && (Character.isDigit(arrayBody.charAt(i)) || arrayBody.charAt(i) == '.')) {
                    i++;
                }
                try {
                    double shift = Double.parseDouble(arrayBody.substring(start, i));
                    if (Math.abs(shift) >= 80.0) {
                        text.append(' ');
                    }
                } catch (NumberFormatException ignored) {
                }
                continue;
            }
            i++;
        }
        return text.toString();
    }

    private int findMatchingBracket(String source, int open) {
        int depth = 0;
        for (int i = open; i < source.length(); i++) {
            char ch = source.charAt(i);
            if (ch == '(') {
                PdfString parsed = readPdfString(source, i);
                i = parsed.endIndex - 1;
                continue;
            }
            if (ch == '[') {
                depth++;
            } else if (ch == ']') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private String mapBytes(byte[] rawBytes, Map<Integer, Integer> cmap) {
        StringBuilder mapped = new StringBuilder();
        for (int index = 0; index < rawBytes.length; index++) {
            int code = rawBytes[index] & 0xFF;
            if (code == 0x20) {
                int run = 1;
                while (index + run < rawBytes.length && (rawBytes[index + run] & 0xFF) == 0x20) {
                    run++;
                }
                if (run >= 2) {
                    mapped.append(' ');
                } else {
                    mapped.append('и');
                }
                index += run - 1;
                continue;
            }
            Integer unicode = cmap.get(code);
            if (unicode != null) {
                char mappedChar = (char) unicode.intValue();
                // Греческие «Π» на месте точки — это битый CMap, оставляем ASCII.
                if (isGreek(mappedChar) && code < 127) {
                    mapped.append((char) code);
                } else if (isAsciiPunctuation(code) && !isCyrillic(mappedChar)) {
                    mapped.append((char) code);
                } else {
                    mapped.append(mappedChar);
                }
            } else if (code >= 32 && code < 127) {
                mapped.append((char) code);
            }
        }
        return mapped.toString();
    }

    private boolean isGreek(char ch) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
        return block == Character.UnicodeBlock.GREEK || block == Character.UnicodeBlock.GREEK_EXTENDED;
    }

    private boolean isCyrillic(char ch) {
        return Character.UnicodeBlock.of(ch) == Character.UnicodeBlock.CYRILLIC;
    }

    private boolean isAsciiPunctuation(int code) {
        return code < 127 && !Character.isLetterOrDigit(code) && !Character.isWhitespace(code);
    }

    @SuppressWarnings("resource")
    private String inflateOrRaw(byte[] payload) {
        byte[] trimmed = trimEol(payload);
        try {
            Inflater inflater = new Inflater();
            inflater.setInput(trimmed);
            byte[] buffer = new byte[Math.max(4096, trimmed.length * 8)];
            StringBuilder decoded = new StringBuilder();
            while (!inflater.finished()) {
                int n = inflater.inflate(buffer);
                if (n <= 0) {
                    break;
                }
                decoded.append(new String(buffer, 0, n, StandardCharsets.ISO_8859_1));
            }
            inflater.end();
            if (decoded.length() > 0) {
                return decoded.toString();
            }
        } catch (DataFormatException ignored) {
            // поток без сжатия
        }
        return new String(trimmed, StandardCharsets.ISO_8859_1);
    }

    private boolean looksLikePageContent(String content) {
        return content.contains("Tj") || content.contains("TJ") || content.contains(" BT");
    }

    private PdfString readPdfString(String source, int openParen) {
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        int i = openParen + 1;
        int depth = 1;
        while (i < source.length() && depth > 0) {
            char ch = source.charAt(i);
            if (ch == '\\' && i + 1 < source.length()) {
                char next = source.charAt(i + 1);
                if (next >= '0' && next <= '7') {
                    int octLen = 1;
                    while (octLen < 3 && i + 1 + octLen < source.length()) {
                        char d = source.charAt(i + 1 + octLen);
                        if (d < '0' || d > '7') {
                            break;
                        }
                        octLen++;
                    }
                    int code = Integer.parseInt(source.substring(i + 1, i + 1 + octLen), 8);
                    bytes.write(code);
                    i += 1 + octLen;
                    continue;
                }
                bytes.write(unescape(next));
                i += 2;
                continue;
            }
            if (ch == '(') {
                depth++;
                bytes.write(ch);
            } else if (ch == ')') {
                depth--;
                if (depth > 0) {
                    bytes.write(ch);
                }
            } else {
                bytes.write(ch & 0xFF);
            }
            i++;
        }
        return new PdfString(bytes.toByteArray(), i);
    }

    private int unescape(char next) {
        return switch (next) {
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 't' -> '\t';
            case 'b' -> '\b';
            case 'f' -> '\f';
            default -> next & 0xFF;
        };
    }

    private int skipSpacesIndex(String s, int i) {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
            i++;
        }
        return i;
    }

    private boolean startsWith(String s, int i, String token) {
        return i + token.length() <= s.length() && s.startsWith(token, i);
    }

    private int skipEol(byte[] data, int i) {
        if (i < data.length && data[i] == '\r') {
            i++;
        }
        if (i < data.length && data[i] == '\n') {
            i++;
        }
        return i;
    }

    private byte[] trimEol(byte[] data) {
        int end = data.length;
        while (end > 0 && (data[end - 1] == '\n' || data[end - 1] == '\r')) {
            end--;
        }
        if (end == data.length) {
            return data;
        }
        byte[] cut = new byte[end];
        System.arraycopy(data, 0, cut, 0, end);
        return cut;
    }

    private int indexOf(byte[] haystack, byte[] needle, int from) {
        outer:
        for (int i = from; i + needle.length <= haystack.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private byte[] slice(byte[] data, int from, int to) {
        int len = Math.max(0, to - from);
        byte[] out = new byte[len];
        System.arraycopy(data, from, out, 0, len);
        return out;
    }

    private static final class PdfString {
        private final byte[] rawBytes;
        private final int endIndex;

        private PdfString(byte[] rawBytes, int endIndex) {
            this.rawBytes = rawBytes;
            this.endIndex = endIndex;
        }
    }
}
