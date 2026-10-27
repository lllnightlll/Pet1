package org.example.archive;

import java.util.Collections;
import java.util.Map;

/**
 * Кодировка одного шрифта PDF.
 * В этой книге pdfFactory кладёт кириллицу в subset-шрифты F1+1 / F2+1 / F3+1
 * с ToUnicode CMap, а обычные F1 / F2 / F3 остаются WinAnsi (байт = ASCII).
 * Без этого разделения байт 0x20 у F1 (пробел вёрстки) читается CMap-ом F1+1 как «и».
 */
public final class PdfFontEncoding {

    private final Map<Integer, Integer> toUnicode;
    private final boolean winAnsi;

    private PdfFontEncoding(Map<Integer, Integer> toUnicode, boolean winAnsi) {
        this.toUnicode = toUnicode;
        this.winAnsi = winAnsi;
    }

    /**
     * Стандартный Times New Roman без ToUnicode: символы 32..126 — как в ASCII.
     */
    public static PdfFontEncoding winAnsi() {
        return new PdfFontEncoding(Collections.emptyMap(), true);
    }

    /**
     * Subset-шрифт с таблицей «байт глифа → Unicode».
     */
    public static PdfFontEncoding fromToUnicode(Map<Integer, Integer> cmap) {
        return new PdfFontEncoding(Map.copyOf(cmap), false);
    }

    /**
     * Переводит один байт строки (...) Tj в символ книги.
     *
     * @return Unicode-символ или 0, если байт нужно пропустить
     */
    public char decode(int code) {
        if (winAnsi) {
            return (code >= 32 && code < 127) ? (char) code : 0;
        }
        Integer unicode = toUnicode.get(code);
        if (unicode == null) {
            return (code >= 32 && code < 127) ? (char) code : 0;
        }
        char mapped = (char) unicode.intValue();
        // В CMap этой книги точка иногда числится как греческая «Π».
        if (isGreek(mapped) && code < 127) {
            return (char) code;
        }
        // ASCII-пунктуация, которую CMap увёл в некириллицу, оставляем как знак.
        if (isAsciiPunctuation(code) && !isCyrillic(mapped)) {
            return (char) code;
        }
        return mapped;
    }

    private static boolean isGreek(char ch) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
        return block == Character.UnicodeBlock.GREEK
                || block == Character.UnicodeBlock.GREEK_EXTENDED;
    }

    private static boolean isCyrillic(char ch) {
        return Character.UnicodeBlock.of(ch) == Character.UnicodeBlock.CYRILLIC;
    }

    private static boolean isAsciiPunctuation(int code) {
        return code < 127 && !Character.isLetterOrDigit(code) && !Character.isWhitespace(code);
    }
}
