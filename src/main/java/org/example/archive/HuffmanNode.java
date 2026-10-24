package org.example.archive;

/**
 * Узел дерева Хаффмана: лист хранит букву, внутренний узел — только сумму частот.
 */
public class HuffmanNode implements Comparable<HuffmanNode> {

    private final Character letter;
    private final double frequency;
    private final HuffmanNode left;
    private final HuffmanNode right;

    public HuffmanNode(Character letter, double frequency, HuffmanNode left, HuffmanNode right) {
        this.letter = letter;
        this.frequency = frequency;
        this.left = left;
        this.right = right;
    }

    public static HuffmanNode leaf(char letter, double frequency) {
        return new HuffmanNode(letter, frequency, null, null);
    }

    public static HuffmanNode merge(HuffmanNode left, HuffmanNode right) {
        return new HuffmanNode(null, left.frequency + right.frequency, left, right);
    }

    public boolean isLeaf() {
        return letter != null;
    }

    public char letter() {
        return letter;
    }

    public HuffmanNode left() {
        return left;
    }

    public HuffmanNode right() {
        return right;
    }

    @Override
    public int compareTo(HuffmanNode other) {
        int byFreq = Double.compare(this.frequency, other.frequency);
        if (byFreq != 0) {
            return byFreq;
        }
        char a = this.letter == null ? 0 : this.letter;
        char b = other.letter == null ? 0 : other.letter;
        return Character.compare(a, b);
    }
}
