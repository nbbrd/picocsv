package _benchmark.de.siegmar.csvbenchmark;

import de.siegmar.fastcsv.writer.CsvWriter;
import de.siegmar.fastcsv.writer.LineDelimiter;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

public final class Constant {

    public static final char SEPARATOR = ',';
    public static final char DELIMITER = '"';
    public static final LineDelimiter LINE_DELIMITER = LineDelimiter.LF;

    public static final String[] row = {
            "Simple field",
            "Example with separator " + SEPARATOR,
            "Example with delimiter " + DELIMITER,
            "Example with\nnewline",
            "Example with " + SEPARATOR + " and " + DELIMITER + " and \nnewline"
    };

    public static final String data;

    static {
        final StringWriter line = new StringWriter();
        try (CsvWriter appender = CsvWriter.builder()
                .fieldSeparator(SEPARATOR)
                .lineDelimiter(LINE_DELIMITER)
                .quoteCharacter(DELIMITER)
                .build(line)) {
            appender.writeRow(row);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        data = line.toString();
    }

}