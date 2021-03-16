package _demo;

import nbbrd.picocsv.Csv;

import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public class StreamDemo {

    public static void main(String[] args) throws IOException {
        System.out.println("[All]");
        try (Csv.Reader reader = Top5GridMonthly.open()) {
            CsvStream
                    .asStream(reader, true, 0, CsvStream.Columns.all(false))
                    .map(row -> row.getLineNumber() + ": " + String.join("; ", row))
                    .forEach(System.out::println);
        }

        System.out.println("[ByIndex]");
        try (Csv.Reader reader = Top5GridMonthly.open()) {
            CsvStream
                    .asStream(reader, true, 0, CsvStream.Columns.byIndex(true, 0, 2, 4))
                    .map(Top5Row::parse)
                    .forEach(System.out::println);
        }

        System.out.println("[ByName]");
        try (Csv.Reader reader = Top5GridMonthly.open()) {
            CsvStream
                    .asStream(reader, true, 0, CsvStream.Columns.byName("", "Firefox", "Safari"))
                    .map(Top5Row::parse)
                    .forEach(System.out::println);
        }
    }

    @lombok.Value
    private static class Top5Row {

        int lineNumber;
        String date;
        double firefox;
        double safari;

        @Override
        public String toString() {
            return lineNumber + ": date=" + date + ", firefox=" + firefox + ", safari=" + safari;
        }

        static Top5Row parse(CsvStream.Row row) {
            try {
                return new Top5Row(
                        row.getLineNumber(),
                        row.getField(0),
                        FORMAT.parse(row.getField(1)).doubleValue(),
                        FORMAT.parse(row.getField(2)).doubleValue()
                );
            } catch (ParseException ex) {
                throw new RuntimeException(ex);
            }
        }

        private static final NumberFormat FORMAT = NumberFormat.getInstance(Locale.FRENCH);
    }
}
