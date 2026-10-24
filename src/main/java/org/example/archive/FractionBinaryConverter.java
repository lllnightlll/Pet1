package org.example.archive;

public class FractionBinaryConverter {

    /**
     * Возвращает строку вида 0.010011… заданной длины после точки.
     */
    public String toBinaryFraction(double value, int bits) {
        if (value < 0) {
            value = 0;
        }
        if (value >= 1) {
            value = value - Math.floor(value);
        }
        StringBuilder bitsBuilder = new StringBuilder("0.");
        double current = value;
        for (int i = 0; i < bits; i++) {
            current *= 2.0;
            if (current >= 1.0) {
                bitsBuilder.append('1');
                current -= 1.0;
            } else {
                bitsBuilder.append('0');
            }
        }
        return bitsBuilder.toString();
    }

    /**
     * Пошаговая трассировка, как столбик 0.073×2 на доске.
     */
    public String trace(double start, int steps) {
        StringBuilder trace = new StringBuilder();
        double current = start;
        for (int i = 0; i < steps; i++) {
            double doubled = current * 2.0;
            int bit = doubled >= 1.0 ? 1 : 0;
            trace.append(String.format(java.util.Locale.US,
                    "%.6f × 2 = %.6f  → бит %d%n", current, doubled, bit));
            current = doubled >= 1.0 ? doubled - 1.0 : doubled;
        }
        return trace.toString();
    }
}
