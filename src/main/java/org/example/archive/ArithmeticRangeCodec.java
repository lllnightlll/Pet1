package org.example.archive;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Арифметическое (range) кодирование по формулам с доски:
 * <pre>
 *   B  = HI - LO + 1
 *   LO = LO + B · LS
 *   HI = LO + B · HS - 1   (в обеих формулах берётся СТАРЫЙ LO)
 * </pre>
 * Регистры LO/HI — целые на 48 бит. Когда старшие биты LO и HI совпали,
 * этот бит выдаётся в код и оба регистра сдвигаются влево (как 10111/11100 → 01110/11001).
 */
public class ArithmeticRangeCodec {

    private final int precisionBits;
    private final BigInteger mask;
    private final BigInteger half;

    public ArithmeticRangeCodec(int precisionBits) {
        this.precisionBits = precisionBits;
        this.mask = BigInteger.ONE.shiftLeft(precisionBits).subtract(BigInteger.ONE);
        this.half = BigInteger.ONE.shiftLeft(precisionBits - 1);
    }

    /**
     * Кодирует буквенную строку. Модель — фиксированные частоты из таблицы (неадаптивная).
     */
    public EncodeResult encode(String letters, FrequencyTable table) {
        IntegerCumulative cumulative = IntegerCumulative.from(table);
        BigInteger lo = BigInteger.ZERO;
        BigInteger hi = mask;
        StringBuilder bits = new StringBuilder();
        int outstanding = 0;
        List<String> firstSteps = new ArrayList<>();

        for (int i = 0; i < letters.length(); i++) {
            char letter = letters.charAt(i);
            Bound bound = cumulative.boundOf(letter);
            BigInteger range = hi.subtract(lo).add(BigInteger.ONE);
            BigInteger newLo = lo.add(range.multiply(bound.lowInclusive).divide(bound.total));
            BigInteger newHi = lo.add(range.multiply(bound.highExclusive).divide(bound.total)).subtract(BigInteger.ONE);
            lo = newLo;
            hi = newHi;
            if (firstSteps.size() < 8) {
                firstSteps.add(String.format(Locale.US,
                        "символ '%s'  LS=%.6f  HS=%.6f  LO=%s  HI=%s",
                        letter,
                        bound.lowInclusive.doubleValue() / bound.total.doubleValue(),
                        bound.highExclusive.doubleValue() / bound.total.doubleValue(),
                        toBitString(lo),
                        toBitString(hi)));
            }
            Renorm renorm = renormalize(lo, hi, bits, outstanding);
            lo = renorm.lo;
            hi = renorm.hi;
            outstanding = renorm.outstanding;
        }
        finish(lo, bits, outstanding);
        return new EncodeResult(bits.toString(), firstSteps, letters.length());
    }

    /**
     * Обратный проход: по битам и той же модели восстанавливаем буквы.
     */
    public String decode(String bits, int letterCount, FrequencyTable table) {
        IntegerCumulative cumulative = IntegerCumulative.from(table);
        BitReader reader = new BitReader(bits);
        BigInteger lo = BigInteger.ZERO;
        BigInteger hi = mask;
        BigInteger value = BigInteger.ZERO;
        for (int i = 0; i < precisionBits; i++) {
            value = value.shiftLeft(1);
            if (reader.next() == 1) {
                value = value.add(BigInteger.ONE);
            }
        }
        StringBuilder decoded = new StringBuilder();
        for (int n = 0; n < letterCount; n++) {
            BigInteger range = hi.subtract(lo).add(BigInteger.ONE);
            BigInteger offset = value.subtract(lo).add(BigInteger.ONE).multiply(cumulative.total).subtract(BigInteger.ONE).divide(range);
            Bound bound = cumulative.boundByOffset(offset);
            decoded.append(bound.letter);
            BigInteger newLo = lo.add(range.multiply(bound.lowInclusive).divide(bound.total));
            BigInteger newHi = lo.add(range.multiply(bound.highExclusive).divide(bound.total)).subtract(BigInteger.ONE);
            lo = newLo;
            hi = newHi;
            while (true) {
                if (lo.compareTo(half) >= 0) {
                    lo = lo.subtract(half);
                    hi = hi.subtract(half);
                    value = value.subtract(half);
                } else if (hi.compareTo(half) < 0) {
                    // оба в нижней половине — ничего не вычитаем
                } else if (lo.compareTo(half.shiftRight(1)) >= 0 && hi.compareTo(half.add(half.shiftRight(1))) < 0) {
                    lo = lo.subtract(half.shiftRight(1));
                    hi = hi.subtract(half.shiftRight(1));
                    value = value.subtract(half.shiftRight(1));
                } else {
                    break;
                }
                lo = lo.shiftLeft(1).and(mask);
                hi = hi.shiftLeft(1).or(BigInteger.ONE).and(mask);
                value = value.shiftLeft(1).and(mask);
                if (reader.next() == 1) {
                    value = value.add(BigInteger.ONE);
                }
            }
        }
        return decoded.toString();
    }

    /**
     * Сдвиг, пока старший бит LO и HI одинаков (картинка справа на доске),
     * плюс обработка «висящих» битов, когда интервал зажал 0.5.
     */
    private Renorm renormalize(BigInteger lo, BigInteger hi, StringBuilder bits, int outstanding) {
        BigInteger quarter = half.shiftRight(1);
        BigInteger threeQuarter = half.add(quarter);
        while (true) {
            if (hi.compareTo(half) < 0) {
                emit(bits, 0, outstanding);
                outstanding = 0;
            } else if (lo.compareTo(half) >= 0) {
                emit(bits, 1, outstanding);
                outstanding = 0;
                lo = lo.subtract(half);
                hi = hi.subtract(half);
            } else if (lo.compareTo(quarter) >= 0 && hi.compareTo(threeQuarter) < 0) {
                outstanding++;
                lo = lo.subtract(quarter);
                hi = hi.subtract(quarter);
            } else {
                break;
            }
            lo = lo.shiftLeft(1).and(mask);
            hi = hi.shiftLeft(1).or(BigInteger.ONE).and(mask);
        }
        return new Renorm(lo, hi, outstanding);
    }

    private void emit(StringBuilder bits, int bit, int outstanding) {
        bits.append(bit == 1 ? '1' : '0');
        char opposite = bit == 1 ? '0' : '1';
        for (int i = 0; i < outstanding; i++) {
            bits.append(opposite);
        }
    }

    private void finish(BigInteger lo, StringBuilder bits, int outstanding) {
        outstanding++;
        if (lo.compareTo(half.shiftRight(1)) < 0) {
            emit(bits, 0, outstanding);
        } else {
            emit(bits, 1, outstanding);
        }
    }

    private String toBitString(BigInteger value) {
        String raw = value.and(mask).toString(2);
        StringBuilder padded = new StringBuilder();
        for (int i = raw.length(); i < precisionBits; i++) {
            padded.append('0');
        }
        padded.append(raw);
        return padded.toString();
    }

    public static final class EncodeResult {
        private final String bits;
        private final List<String> firstSteps;
        private final int letterCount;

        public EncodeResult(String bits, List<String> firstSteps, int letterCount) {
            this.bits = bits;
            this.firstSteps = firstSteps;
            this.letterCount = letterCount;
        }

        public String bits() {
            return bits;
        }

        public List<String> firstSteps() {
            return firstSteps;
        }

        public int letterCount() {
            return letterCount;
        }

        public double bitsPerLetter() {
            if (letterCount == 0) {
                return 0;
            }
            return bits.length() / (double) letterCount;
        }
    }

    private static final class Renorm {
        private final BigInteger lo;
        private final BigInteger hi;
        private final int outstanding;

        private Renorm(BigInteger lo, BigInteger hi, int outstanding) {
            this.lo = lo;
            this.hi = hi;
            this.outstanding = outstanding;
        }
    }

    /**
     * Целочисленные LS/HS: накопленные счётчики, чтобы не терять точность на double.
     */
    private static final class IntegerCumulative {
        private final List<Bound> bounds;
        private final BigInteger total;

        private IntegerCumulative(List<Bound> bounds, BigInteger total) {
            this.bounds = bounds;
            this.total = total;
        }

        static IntegerCumulative from(FrequencyTable table) {
            List<Bound> bounds = new ArrayList<>();
            long running = 0;
            for (SymbolStat stat : table.alphabetOrder()) {
                long next = running + stat.count();
                bounds.add(new Bound(
                        stat.letter(),
                        BigInteger.valueOf(running),
                        BigInteger.valueOf(next),
                        BigInteger.valueOf(table.totalLetters())));
                running = next;
            }
            return new IntegerCumulative(bounds, BigInteger.valueOf(table.totalLetters()));
        }

        Bound boundOf(char letter) {
            for (Bound bound : bounds) {
                if (bound.letter == letter) {
                    return bound;
                }
            }
            throw new IllegalArgumentException("Нет символа: " + letter);
        }

        Bound boundByOffset(BigInteger offset) {
            for (Bound bound : bounds) {
                if (offset.compareTo(bound.lowInclusive) >= 0 && offset.compareTo(bound.highExclusive) < 0) {
                    return bound;
                }
            }
            return bounds.get(bounds.size() - 1);
        }
    }

    private static final class Bound {
        private final char letter;
        private final BigInteger lowInclusive;
        private final BigInteger highExclusive;
        private final BigInteger total;

        private Bound(char letter, BigInteger lowInclusive, BigInteger highExclusive, BigInteger total) {
            this.letter = letter;
            this.lowInclusive = lowInclusive;
            this.highExclusive = highExclusive;
            this.total = total;
        }
    }

    private static final class BitReader {
        private final String bits;
        private int index;

        private BitReader(String bits) {
            this.bits = bits;
            this.index = 0;
        }

        private int next() {
            if (index >= bits.length()) {
                return 0;
            }
            return bits.charAt(index++) == '1' ? 1 : 0;
        }
    }
}
