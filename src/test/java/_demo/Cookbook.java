package _demo;

import lombok.NonNull;
import nbbrd.picocsv.Csv;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@lombok.experimental.UtilityClass
public class Cookbook {

    public static boolean skipComments(Csv.Reader reader) throws IOException {
        while (reader.readLine()) {
            if (!reader.isComment()) {
                return true;
            }
        }
        return false;
    }

    public static boolean skipLines(Csv.Reader reader, int skipLines) throws IOException {
        for (int i = 0; i < skipLines; i++) {
            if (!reader.readLine()) {
                return false;
            }
        }
        return true;
    }

    public static @NonNull String[] readHeaderLine(@NonNull Csv.Reader reader) throws IOException {
        if (!skipComments(reader)) {
            throw new IOException("Missing header");
        }
        return readFieldsOfUnknownSize(reader);
    }

    public static String[] readFieldsOfUnknownSize(Csv.LineReader reader) throws IOException {
        if (reader.readField()) {
            List<String> row = new ArrayList<>();
            do {
                row.add(reader.toString());
            } while (reader.readField());
            return row.toArray(EMPTY_STRING_ARRAY);
        }
        return EMPTY_STRING_ARRAY;
    }

    public static String[] readFieldsOfFixedSize(Csv.LineReader reader, int[] mapping, int size) throws IOException {
        String[] result = new String[size];
        for (int i = 0; reader.readField(); i++) {
            int position = mapping[i];
            if (isValidIndex(position)) {
                result[position] = reader.toString();
            }
        }
        return result;
    }

    public static Function<String[], int[]> mapperByIndex(int... indexes) {
        List<Integer> list = IntStream.of(indexes).boxed().collect(Collectors.toList());
        return header -> IntStream.range(0, header.length).map(list::indexOf).toArray();
    }

    public static Function<String[], int[]> mapperByName(String... names) {
        List<String> list = Arrays.asList(names);
        return header -> Stream.of(header).mapToInt(list::indexOf).toArray();
    }

    public static @NonNull Csv.LineReader asLineReader(@NonNull String... fields) {
        return new ArrayLineReader(fields);
    }

    @lombok.RequiredArgsConstructor
    private static final class ArrayLineReader implements Csv.LineReader {

        private final @NonNull String[] fields;
        private int cursor = -1;

        @Override
        public boolean readField() {
            return ++cursor < fields.length;
        }

        @Override
        public boolean isComment() {
            return false;
        }

        @Override
        public int length() {
            return fields[cursor].length();
        }

        @Override
        public char charAt(int index) {
            return fields[cursor].charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return fields[cursor].subSequence(start, end);
        }

        @Override
        public String toString() {
            return fields[cursor];
        }
    }

    private static final String[] EMPTY_STRING_ARRAY = {};


    public static boolean isValidIndex(int i) {
        return i != -1;
    }

    @lombok.RequiredArgsConstructor
    public static final class RowIterator extends AbstractIterator<Csv.LineReader> {

        private final Csv.Reader reader;

        @Override
        protected Csv.LineReader get() {
            return reader;
        }

        @Override
        protected boolean moveNext() {
            try {
                return skipComments(reader);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    @FunctionalInterface
    public interface RowReader<T> {

        @NonNull T applyWithIO(@NonNull Csv.LineReader line) throws IOException;

        default @NonNull Function<Csv.LineReader, T> asUnchecked() {
            return line -> {
                try {
                    return applyWithIO(line);
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            };
        }
    }

    @FunctionalInterface
    public interface RowWriter<T> {

        void acceptWithIO(@NonNull Csv.LineWriter line, @NonNull T value) throws IOException;

        default @NonNull BiConsumer<Csv.LineWriter, T> asUnchecked() {
            return (line, value) -> {
                try {
                    acceptWithIO(line, value);
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            };
        }
    }
}