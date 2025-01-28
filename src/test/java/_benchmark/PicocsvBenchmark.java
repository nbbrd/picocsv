/*
 * Copyright 2018 National Bank of Belgium
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved
 * by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */
package _benchmark;

import _benchmark.de.siegmar.csvbenchmark.CsvConstants;
import _benchmark.de.siegmar.csvbenchmark.ICsvReader;
import _benchmark.de.siegmar.csvbenchmark.util.InfiniteDataReader;
import nbbrd.picocsv.Csv;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Philippe Charles
 */
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 3)
@BenchmarkMode(Mode.Throughput)
public class PicocsvBenchmark {

    @State(Scope.Benchmark)
    public static class ReadState {

        private ICsvReader reader;

        @Setup
        public void setup() throws IOException {
            reader = reader();
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

    public static ICsvReader reader() throws IOException {
        return new ICsvReader() {
            private final Csv.Reader csvReader = Csv.Reader.of(FORMAT, Csv.ReaderOptions.DEFAULT,
                    new InfiniteDataReader(CsvConstants.DATA), Csv.DEFAULT_CHAR_BUFFER_SIZE);

            @Override
            public List<String> readRecord() throws IOException {
                final List<String> result = new ArrayList<>();
                if (csvReader.readLine()) {
                    while (csvReader.readField()) {
                        result.add(csvReader.toString());
                    }
                }
                return result;
            }

            @Override
            public void close() throws IOException {
                csvReader.close();
            }
        };
    }

    private static final Csv.Format FORMAT = Csv.Format.DEFAULT
            .toBuilder()
            .delimiter(CsvConstants.SEPARATOR)
            .separator(Csv.Format.UNIX_SEPARATOR)
            .quote(CsvConstants.DELIMITER)
            .build();
}
