package _demo;

import nbbrd.picocsv.Csv;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.function.IntPredicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@lombok.experimental.UtilityClass
public class CsvStream {

    public interface Columns {

        boolean hasHeader();

        IntPredicate getFilter(List<String> columns);

        static Columns all(boolean header) {
            return new Columns() {
                @Override
                public boolean hasHeader() {
                    return header;
                }

                @Override
                public IntPredicate getFilter(List<String> columns) {
                    return i -> true;
                }
            };
        }

        static Columns byName(String... names) {
            List<String> list = new ArrayList<>();
            for (String name : names) {
                list.add(name);
            }
            return new Columns() {
                @Override
                public boolean hasHeader() {
                    return true;
                }

                @Override
                public IntPredicate getFilter(List<String> columns) {
                    List<Integer> indexes = new ArrayList<>();
                    for (int i = 0; i < columns.size(); i++) {
                        if (list.contains(columns.get(i))) {
                            indexes.add(i);
                        }
                    }
                    return indexes::contains;
                }
            };
        }

        static Columns byIndex(boolean header, int... indexes) {
            List<Integer> list = new ArrayList<>();
            for (int index : indexes) {
                list.add(index);
            }
            return new Columns() {
                @Override
                public boolean hasHeader() {
                    return header;
                }

                @Override
                public IntPredicate getFilter(List<String> columns) {
                    return list::contains;
                }
            };
        }
    }

    public static final class Row implements Iterable<String> {

        private final List<String> fields = new ArrayList<>();
        private int lineNumber = 0;

        public int getFieldCount() {
            return fields.size();
        }

        public String getField(int index) {
            return fields.get(index);
        }

        @Override
        public Iterator<String> iterator() {
            return fields.iterator();
        }

        public int getLineNumber() {
            return lineNumber;
        }
    }

    public static Stream<Row> asStream(Csv.Reader reader, boolean ignoreEmptyLines, int skipLines, Columns selector) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(asIterator(reader, ignoreEmptyLines, skipLines, selector), 0), false);
    }

    public static Iterator<Row> asIterator(Csv.Reader reader, boolean ignoreEmptyLines, int skipLines, Columns selector) {
        Row row = new Row();

        try {
            if (!skipLines(reader, skipLines, row)) return Collections.emptyIterator();

            IntPredicate fieldFilter = getFieldFilter(reader, selector, row);
            if (fieldFilter == null) return Collections.emptyIterator();

            return new AbstractIterator<Row>() {
                @Override
                protected Row get() {
                    return row;
                }

                @Override
                protected boolean moveNext() {
                    try {
                        while (reader.readLine()) {
                            if (readFields(reader, ignoreEmptyLines, row, fieldFilter)) return true;
                        }
                    } catch (IOException ex) {
                        throw new UncheckedIOException(ex);
                    }
                    return false;
                }
            };
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static boolean skipLines(Csv.Reader reader, int skipLines, Row row) throws IOException {
        for (int i = 0; i < skipLines; i++) {
            if (!reader.readLine()) {
                return false;
            }
            row.lineNumber++;
        }
        return true;
    }

    private static IntPredicate getFieldFilter(Csv.Reader reader, Columns selector, Row row) throws IOException {
        if (selector.hasHeader()) {
            if (!reader.readLine()) {
                return null;
            }
            row.lineNumber++;
            List<String> columnNames = new ArrayList<>();
            while (reader.readField()) {
                columnNames.add(reader.toString());
            }
            return selector.getFilter(columnNames);
        } else {
            return selector.getFilter(Collections.emptyList());
        }
    }

    private static boolean readFields(Csv.Reader reader, boolean ignoreEmptyLines, Row row, IntPredicate fieldFilter) throws IOException {
        row.fields.clear();
        row.lineNumber++;

        int fieldIndex = 0;
        if (reader.readField()) {
            do {
                if (fieldFilter.test(fieldIndex)) {
                    row.fields.add(reader.toString());
                }
                fieldIndex++;
            } while (reader.readField());
            return true;
        } else if (!ignoreEmptyLines) {
            return true;
        }
        return false;
    }

    private static abstract class AbstractIterator<E> implements Iterator<E> {

        abstract protected E get();

        abstract protected boolean moveNext();

        private enum State {
            COMPUTED, NOT_COMPUTED, DONE
        }

        private State state = State.NOT_COMPUTED;

        @Override
        public boolean hasNext() {
            switch (state) {
                case COMPUTED:
                    return true;
                case DONE:
                    return false;
                default:
                    if (moveNext()) {
                        state = State.COMPUTED;
                        return true;
                    }
                    state = State.DONE;
                    return false;
            }
        }

        @Override
        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            state = State.NOT_COMPUTED;
            return get();
        }
    }
}
