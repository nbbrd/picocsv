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

import _benchmark.de.siegmar.csvbenchmark.Constant;
import _benchmark.de.siegmar.csvbenchmark.util.InfiniteDataReader;
import de.siegmar.fastcsv.reader.CloseableIterator;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRow;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;

/**
 * @author Philippe Charles
 */
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 10)
@BenchmarkMode(Mode.Throughput)
public class FastCsvBenchmark {

    @State(Scope.Benchmark)
    public static class ReadState {

        CloseableIterator<CsvRow> input;

        @Setup
        public void setup() throws IOException {
            input = CsvReader.builder()
                    .fieldSeparator(Constant.SEPARATOR)
                    .quoteCharacter(Constant.DELIMITER)
                    .skipEmptyRows(false)
                    .build(new InfiniteDataReader(Constant.data))
                    .iterator();
        }

        @TearDown
        public void teardown() throws IOException {
            input.close();
        }
    }

    @Benchmark
    public void readLine(ReadState state, Blackhole blackhole) throws IOException {
        blackhole.consume(state.input.next());
    }
}
