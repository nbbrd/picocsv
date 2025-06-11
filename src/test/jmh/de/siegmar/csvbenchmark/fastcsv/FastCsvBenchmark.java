package de.siegmar.csvbenchmark.fastcsv;

import de.siegmar.csvbenchmark.CsvConstants;
import de.siegmar.csvbenchmark.ICsvReader;
import de.siegmar.csvbenchmark.ICsvWriter;
import de.siegmar.csvbenchmark.util.NullWriter;
import de.siegmar.csvbenchmark.util.RowSupplier;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;
import java.util.Collection;

public class FastCsvBenchmark {

    @State(Scope.Benchmark)
    public static class WriteState {

        private final RowSupplier rowSupplier = new RowSupplier(CsvConstants.RECORDS);
        private ICsvWriter writer;

        @Setup
        public void setup(final Blackhole bh) {
            writer = Factory.writer(new NullWriter(bh));
        }

        @TearDown
        public void teardown() throws IOException {
            writer.close();
        }

    }

    @Benchmark
    public void write(final WriteState state) throws Exception {
        state.writer.writeRecord(state.rowSupplier.get());
    }

    @State(Scope.Benchmark)
    public static class ReadState {

        private ICsvReader reader;

        @Setup
        public void setup() {
            reader = Factory.reader();
        }

        @TearDown
        public void teardown() throws IOException {
            reader.close();
        }

    }

    @Benchmark
    public Collection<String> read(final ReadState state) throws Exception {
        return state.reader.readRecord();
    }

}
