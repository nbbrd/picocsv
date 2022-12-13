package _demo.api2;

import java.io.IOException;
import java.io.Reader;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static _test.Top5GridMonthly.*;

public class CsvRowParserDemo {

    public static void main(String[] args) throws IOException {
        parseAll();
        parseByIndex();
        parseByName();
    }

    private static void parseAll() throws IOException {
        System.out.println("[All]");

        CsvRowParser parser = CsvRowParser
                .builder()
                .separator(SEPARATOR)
                .delimiter(DELIMITER)
                .quote(QUOTE)
                .includeEmptyLines(false)
                .skipLines(0)
                .columns(Columns.all(false))
                .build();

        try (Reader reader = openStreamReader()) {
            for (Row row : (Iterable<Row>) () -> parser.iterate(reader)) {
                System.out.println(row.getLineNumber() + ": " + String.join("; ", row));
            }
        }
    }

    private static void parseByIndex() throws IOException {
        System.out.println("[ByIndex]");

        CsvRowParser parser = CsvRowParser
                .builder()
                .separator(SEPARATOR)
                .delimiter(DELIMITER)
                .quote(QUOTE)
                .includeEmptyLines(false)
                .skipLines(0)
                .columns(Columns.byIndex(true, 0, 2, 4))
                .build();

        try (Reader reader = openStreamReader()) {
            parser
                    .stream(reader)
                    .map(Top5Row::parse)
                    .forEach(System.out::println);
        }
    }

    private static void parseByName() throws IOException {
        System.out.println("[ByName]");

        CsvRowParser parser = CsvRowParser
                .builder()
                .separator(SEPARATOR)
                .delimiter(DELIMITER)
                .quote(QUOTE)
                .includeEmptyLines(false)
                .skipLines(0)
                .columns(Columns.byName("", "Firefox", "Safari"))
                .build();

        try (Reader reader = openStreamReader()) {
            parser
                    .stream(reader)
                    .map(Top5Row::parse)
                    .forEach(System.out::println);
        }
    }

    @lombok.Value
    private static class Top5Row {

        int lineNumber;
        YearMonth date;
        double firefox;
        double safari;

        @Override
        public String toString() {
            return lineNumber + ": date=" + date + ", firefox=" + firefox + ", safari=" + safari;
        }

        static Top5Row parse(Row row) {
            try {
                return new Top5Row(
                        row.getLineNumber(),
                        DATE.parse(row.getField(0), YearMonth::from),
                        NUMBER.parse(row.getField(1)).doubleValue(),
                        NUMBER.parse(row.getField(2)).doubleValue()
                );
            } catch (ParseException ex) {
                throw new RuntimeException(ex);
            }
        }

        private static final NumberFormat NUMBER = NumberFormat.getInstance(Locale.FRENCH);
        private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d/MM/yyyy", Locale.FRENCH);
    }
}
