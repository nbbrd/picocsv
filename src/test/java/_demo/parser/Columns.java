package _demo.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntPredicate;

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
        List<String> list = new ArrayList<>(Arrays.asList(names));
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
