package _test.fastcsv;

import _test.Row;
import _test.Sample;
import de.siegmar.fastcsv.reader.CommentStrategy;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRow;
import nbbrd.picocsv.Csv;

import java.io.StringReader;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@lombok.experimental.UtilityClass
public class FastCsvEntryConverter {

    public static Sample toSample(FastCsvEntry entry) {
        return Sample
                .builder()
                .content(toContent(entry.getInput()))
                .rows(toRows(entry.getExpected(), entry.isSkipEmptyLines(), entry.getCommentStrategy()))
                .name(entry.getFlags())
                .format(Csv.Format.RFC4180)
                .build();
    }

    public static FastCsvEntry fromSample(Sample sample) {
        return new FastCsvEntry(
                fromContent(sample.getContent()),
                fromRows(sample.getRows()),
                sample.getName());
    }

    private static String toContent(String text) {
        return text
                .replace('␣', ' ')
                .replace('␍', '\r')
                .replace('␊', '\n');
    }

    private static String fromContent(String text) {
        return text
                .replace(' ', '␣')
                .replace('\r', '␍')
                .replace('\n', '␊');
    }

    private static List<Row> toRows(String expected, boolean skipEmptyLines, CommentStrategy commentStrategy) {
        if (expected.equals("∅")) {
            return Collections.emptyList();
        }

        String content = expected
                .replace('\"', 'x')
                .replace("⏎", Csv.Format.WINDOWS_SEPARATOR)
                .replace("◯", "\"\"");

        return CsvReader
                .builder()
                .skipEmptyRows(skipEmptyLines)
                .fieldSeparator('↷')
                .commentCharacter(';')
                .commentStrategy(commentStrategy)
                .build(new StringReader(content))
                .stream()
                .map(FastCsvEntryConverter::toRow)
                .collect(Collectors.toList());
    }

    private static Row toRow(CsvRow row) {
        return new Row(row.getFields().stream().map(field -> toContent(field).replace('x', '\"')).collect(Collectors.toList()));
    }

    private static String fromRows(List<Row> rows) {
        if (rows.isEmpty()) {
            return "∅";
        }
        return rows
                .stream()
                .map(FastCsvEntryConverter::fromRow)
                .collect(Collectors.joining("⏎"));
    }

    private static String fromRow(Row row) {
        return row.getFields()
                .stream()
                .map(field -> field.isEmpty() ? "◯" : fromContent(field))
                .collect(Collectors.joining("↷"));
    }
}
