package _demo.api1;

import _test.Top5GridMonthly;
import nbbrd.picocsv.Csv;

import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;

public class TableReaderDemo {

    public static void main(String[] args) throws IOException {
        try (Csv.Reader reader = Top5GridMonthly.open()) {
            TableReader.byColumnName("Firefox", "Safari")
                    .lines(reader)
                    .limit(3)
                    .map(Arrays::toString)
                    .forEach(System.out::println);
        }

        System.out.println("---");

        try (Csv.Reader reader = Top5GridMonthly.open()) {
            TableReader.byColumnIndex(2, 4)
                    .toList(reader)
                    .stream()
                    .limit(3)
                    .map(Arrays::toString)
                    .forEach(System.out::println);
        }

        System.out.println("---");

        try (Csv.Reader reader = Top5GridMonthly.open()) {
            TableReader.byColumnIndexNoHeader(7, 2, 4)
                    .toList(reader)
                    .stream()
                    .skip(1)
                    .limit(3)
                    .map(Arrays::toString)
                    .forEach(System.out::println);
        }

        System.out.println("---");

        try (Csv.Reader reader = Top5GridMonthly.open()) {
            System.out.println(
                    TableReader.byColumnName("", "Firefox")
                            .lines(reader)
                            .limit(3)
                            .map(line -> Browser.parse("Firefox", line))
                            .mapToDouble(Browser::getValue)
                            .average()
                            .orElseThrow(RuntimeException::new)
            );
        }
    }

    @lombok.Value
    private static class Browser {

        static Browser parse(String name, String[] line) {
            try {
                return new Browser(name, DATE.parse(line[0], YearMonth::from), NUMBER.parse(line[1]).doubleValue());
            } catch (ParseException ex) {
                throw new RuntimeException(ex);
            }
        }

        String name;
        YearMonth period;
        double value;
    }

    private static final NumberFormat NUMBER = NumberFormat.getInstance(Locale.FRENCH);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d/MM/yyyy", Locale.FRENCH);
}
