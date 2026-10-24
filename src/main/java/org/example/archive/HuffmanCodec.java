package org.example.archive;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

public class HuffmanCodec {

    /**
     * Строит дерево и записывает коды обратно в строки таблицы частот.
     */
    public void assignCodes(FrequencyTable table) {
        PriorityQueue<HuffmanNode> queue = new PriorityQueue<>();
        for (SymbolStat stat : table.stats()) {
            queue.add(HuffmanNode.leaf(stat.letter(), stat.probability()));
        }
        if (queue.size() == 1) {
            table.stats().get(0).setHuffmanCode("0");
            return;
        }
        while (queue.size() > 1) {
            HuffmanNode left = queue.poll();
            HuffmanNode right = queue.poll();
            queue.add(HuffmanNode.merge(left, right));
        }
        HuffmanNode root = queue.poll();
        Map<Character, String> codes = new HashMap<>();
        walk(root, "", codes);
        for (SymbolStat stat : table.stats()) {
            String code = codes.get(stat.letter());
            stat.setHuffmanCode(code == null ? "" : code);
        }
    }

    public String encode(String letters, FrequencyTable table) {
        Map<Character, String> codes = new HashMap<>();
        for (SymbolStat stat : table.stats()) {
            codes.put(stat.letter(), stat.huffmanCode());
        }
        StringBuilder bits = new StringBuilder();
        for (int i = 0; i < letters.length(); i++) {
            String code = codes.get(letters.charAt(i));
            if (code != null) {
                bits.append(code);
            }
        }
        return bits.toString();
    }

    private void walk(HuffmanNode node, String prefix, Map<Character, String> codes) {
        if (node == null) {
            return;
        }
        if (node.isLeaf()) {
            codes.put(node.letter(), prefix.isEmpty() ? "0" : prefix);
            return;
        }
        walk(node.left(), prefix + "0", codes);
        walk(node.right(), prefix + "1", codes);
    }
}
