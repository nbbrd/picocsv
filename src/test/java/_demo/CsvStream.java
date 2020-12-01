package _demo;

import nbbrd.picocsv.Csv;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntPredicate;

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

    interface Row extends Iterable<String> {

        int getNumber();

        int getSize();

        String get(int index);
    }

    private static final class RowImpl implements Row {

        private final List<String> data = new ArrayList<>();
        private int index = 0;

        @Override
        public int getNumber() {
            return index + 1;
        }

        @Override
        public int getSize() {
            return data.size();
        }

        @Override
        public String get(int index) {
            return data.get(index);
        }

        @Override
        public Iterator<String> iterator() {
            return data.iterator();
        }
    }

    static void forEach(Csv.Reader reader, boolean ignoreEmptyLines, int skipLines, Columns selector, Consumer<Row> func) throws IOException {
        RowImpl row = new RowImpl();

        for (int i = 0; i < skipLines; i++) {
            if (!reader.readLine()) {
                return;
            }
            row.index++;
        }

        IntPredicate fieldFilter;
        if (selector.hasHeader()) {
            if (!reader.readLine()) {
                return;
            }
            row.index++;
            List<String> columnNames = new ArrayList<>();
            while (reader.readField()) {
                columnNames.add(reader.toString());
            }
            fieldFilter = selector.getFilter(columnNames);
        } else {
            fieldFilter = selector.getFilter(Collections.emptyList());
        }

        while (reader.readLine()) {
            int fieldIndex = 0;
            if (reader.readField()) {
                do {
                    if (fieldFilter.test(fieldIndex)) {
                        row.data.add(reader.toString());
                    }
                    fieldIndex++;
                }
                while (reader.readField());
                func.accept(row);
            } else if (!ignoreEmptyLines) {
                func.accept(row);
            }
            row.data.clear();
            row.index++;
        }
    }
}
