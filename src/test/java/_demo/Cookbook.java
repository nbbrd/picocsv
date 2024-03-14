package _demo;

import lombok.NonNull;
import nbbrd.picocsv.Csv;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@lombok.experimental.UtilityClass
public class Cookbook {

    public static boolean skipComments(@NonNull Csv.Reader reader) throws IOException {
        while (reader.readLine()) if (!reader.isComment()) return true;
        return false;
    }

    public static boolean skipLines(@NonNull Csv.Reader reader, int skipLines) throws IOException {
        for (int i = 0; i < skipLines; i++) if (!reader.readLine()) return false;
        return true;
    }

    public static @NonNull String[] readHeader(@NonNull Csv.Reader reader) throws IOException {
        if (!skipComments(reader)) {
            throw new IOException("Missing header");
        }
        return readLineOfUnknownSize(reader);
    }

    public static @NonNull String[] readLineOfUnknownSize(@NonNull Csv.LineReader reader) throws IOException {
        if (reader.readField()) {
            List<String> row = new ArrayList<>();
            do {
                row.add(reader.toString());
            } while (reader.readField());
            return row.toArray(EMPTY_STRING_ARRAY);
        }
        return EMPTY_STRING_ARRAY;
    }

    public static @NonNull String[] readLineOfFixedSize(@NonNull Csv.LineReader reader, @NonNull int[] mapping, int size) throws IOException {
        String[] result = new String[size];
        for (int i = 0; reader.readField(); i++) {
            int position = mapping[i];
            if (isValidIndex(position)) {
                result[position] = reader.toString();
            }
        }
        return result;
    }

    public static @NonNull Function<String[], int[]> mapperByIndex(@NonNull int... indexes) {
        List<Integer> list = IntStream.of(indexes).boxed().collect(Collectors.toList());
        return header -> IntStream.range(0, header.length).map(list::indexOf).toArray();
    }

    public static @NonNull Function<String[], int[]> mapperByName(@NonNull String... names) {
        List<String> list = Arrays.asList(names);
        return header -> Stream.of(header).mapToInt(list::indexOf).toArray();
    }

    public static @NonNull Csv.LineReader asLineReader(@NonNull String... fields) {
        return new ArrayLineReader(fields);
    }

    public static @NonNull Iterator<Csv.LineReader> asIterator(@NonNull Csv.Reader reader) {
        return new LineIterator(reader);
    }

    public static @NonNull Stream<Csv.LineReader> asStream(@NonNull Csv.Reader reader) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(asIterator(reader), Spliterator.ORDERED | Spliterator.NONNULL), false);
    }

    public static @NonNull <T> Stream<T> asStream(@NonNull Csv.Reader reader, @NonNull LineParser<T> lineParser) {
        return asStream(reader).map(lineParser.asUnchecked());
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
    private static final class LineIterator extends AbstractIterator<Csv.LineReader> {

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
    public interface LineParser<T> {

        @NonNull T parse(@NonNull Csv.LineReader line) throws IOException;

        default @NonNull Function<Csv.LineReader, T> asUnchecked() {
            return line -> {
                try {
                    return parse(line);
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            };
        }
    }

    @FunctionalInterface
    public interface LineFormatter<T> {

        void format(@NonNull Csv.LineWriter line, @NonNull T value) throws IOException;

        default @NonNull BiConsumer<Csv.LineWriter, T> asUnchecked() {
            return (line, value) -> {
                try {
                    format(line, value);
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            };
        }
    }
}
