package _demo;

import nbbrd.picocsv.Csv;

import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

public class ForEachDemo {

    public static void main(String[] args) throws IOException {
        System.out.println("[All]");
        try (Csv.Reader reader = Top5GridMonthly.open()) {
            CsvStream.forEach(reader, true, 0, CsvStream.Columns.all(false), print(ForEachDemo::toStringArray));
        }

        System.out.println("[ByIndex]");
        try (Csv.Reader reader = Top5GridMonthly.open()) {
            CsvStream.forEach(reader, true, 0, CsvStream.Columns.byIndex(true, 0, 2, 4), print(ForEachDemo::toTop5Row));
        }

        System.out.println("[ByName]");
        try (Csv.Reader reader = Top5GridMonthly.open()) {
            CsvStream.forEach(reader, true, 0, CsvStream.Columns.byName("", "Firefox", "Safari"), print(ForEachDemo::toTop5Row));
        }
    }

    private static Consumer<CsvStream.Row> print(Function<CsvStream.Row, ?> func) {
        return row -> System.out.println(row.getNumber() + ": " + func.apply(row));
    }

    private static String toStringArray(CsvStream.Row row) {
        return String.join(", ", row);
    }

    private static Top5Row toTop5Row(CsvStream.Row row) {
        try {
            return new Top5Row(row.get(0), FORMAT.parse(row.get(1)).doubleValue(), FORMAT.parse(row.get(2)).doubleValue());
        } catch (ParseException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static final NumberFormat FORMAT = NumberFormat.getInstance(Locale.FRENCH);

    @lombok.Value
    private static class Top5Row {
        String date;
        double firefox;
        double safari;
    }
}
