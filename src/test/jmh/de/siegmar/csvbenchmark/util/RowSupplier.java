package de.siegmar.csvbenchmark.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * This class is used to supply rows for the JMH benchmarks.
 */
public class RowSupplier implements Supplier<List<String>> {

    private final List<List<String>> rows;
    private int pos;

    /**
     * Initializes a new instance of the {@link RowSupplier} class
     * with the specified rows.
     *
     * @param rows the rows to supply
     */
    public RowSupplier(final List<List<String>> rows) {
        this.rows = new ArrayList<>(rows);
        pos = rows.size();
    }

    @Override
    public List<String> get() {
        final List<String> d = rows.get(--pos);
        if (pos == 0) {
            pos = rows.size();
        }
        return d;
    }

}
