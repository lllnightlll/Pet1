package org.example.archive;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class ArchiveSettings {
    public static final int MAX_TEXT_LINES = 8_000;
    public static final int PROBABILITY_BINARY_BITS = 24;
    public static final int ARITHMETIC_PRECISION_BITS = 48;
    public static final int ARITHMETIC_SAMPLE_LENGTH = 400;
    public static final int LZ_SAMPLE_LENGTH = 2_000;

    private final Path workDirectory;
    private final Path sourcePdf;
    private final Path extractedTextFile;
    private final Path frequencyReportFile;
    private final Path arithmeticReportFile;
    private final Path lempelZivReportFile;

    public ArchiveSettings(Path workDirectory) {
        this.workDirectory = workDirectory;
        this.sourcePdf = workDirectory.resolve("gustav-lebon-psihologiya-narodov-i-mass-1995.pdf");
        this.extractedTextFile = workDirectory.resolve("extracted-text.txt");
        this.frequencyReportFile = workDirectory.resolve("frequency-huffman.txt");
        this.arithmeticReportFile = workDirectory.resolve("arithmetic-coding.txt");
        this.lempelZivReportFile = workDirectory.resolve("lempel-ziv.txt");
    }

    public static ArchiveSettings fromCodeLocation() {
        Path here = Paths.get("src", "main", "java", "org", "example", "archive");
        return new ArchiveSettings(here.toAbsolutePath().normalize());
    }

    public Path workDirectory() {
        return workDirectory;
    }

    public Path sourcePdf() {
        return sourcePdf;
    }

    public Path extractedTextFile() {
        return extractedTextFile;
    }

    public Path frequencyReportFile() {
        return frequencyReportFile;
    }

    public Path arithmeticReportFile() {
        return arithmeticReportFile;
    }

    public Path lempelZivReportFile() {
        return lempelZivReportFile;
    }
}
