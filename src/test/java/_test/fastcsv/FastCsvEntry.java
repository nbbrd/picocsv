package _test.fastcsv;

import _demo.JavaCsvComparisonDemo;
import de.siegmar.fastcsv.reader.CommentStrategy;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@lombok.Value
public class FastCsvEntry {

    @lombok.NonNull
    String input;

    @lombok.NonNull
    String expected;

    String flags;

    public boolean isSkipEmptyLines() {
        return flags.contains(FastCsvEntry.SKIP_EMPTY_LINES_FLAG);
    }

    public CommentStrategy getCommentStrategy() {
        if (flags.contains(FastCsvEntry.READ_COMMENTS_FLAG)) return CommentStrategy.READ;
        if (flags.contains(FastCsvEntry.SKIP_COMMENTS_FLAG)) return CommentStrategy.SKIP;
        return CommentStrategy.NONE;
    }

    public static final String SKIP_EMPTY_LINES_FLAG = "skipEmptyLines";
    public static final String READ_COMMENTS_FLAG = "readComments";
    public static final String SKIP_COMMENTS_FLAG = "skipComments";

    public static FastCsvEntry parse(String line) {
        if (!line.isEmpty() && !line.startsWith("#")) {
            Matcher matcher = LINE_PATTERN.matcher(line);
            if (matcher.matches()) {
                return new FastCsvEntry(
                        matcher.group("input"),
                        matcher.group("expected"),
                        nullToEmpty(matcher.group("flags"))
                );
            }
        }
        return null;
    }

    public static List<FastCsvEntry> loadAll() throws IOException {
        try (InputStream stream = JavaCsvComparisonDemo.class.getResourceAsStream("/test.txt")) {
            try (BufferedReader charReader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                return charReader
                        .lines()
                        .map(FastCsvEntry::parse)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            } catch (UncheckedIOException ex) {
                throw ex;
            }
        }
    }

    private static final Pattern LINE_PATTERN = Pattern.compile("^(?<input>\\S+)(?:\\s+(?<expected>\\S+))(?:\\s+\\[(?<flags>\\w+)])?");

    private static String nullToEmpty(String text) {
        return text == null ? "" : text;
    }
}
