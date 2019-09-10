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
import nbbrd.picocsv.CsvReader;
import java.io.IOException;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import nbbrd.picocsv.CsvFormat;
import nbbrd.picocsv.NewLine;

/**
 *
 * @author Philippe Charles
 */
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 10)
@BenchmarkMode(Mode.Throughput)
public class CsvStreamBenchmark {

    @State(Scope.Benchmark)
    public static class ReadState {

        CsvReader input;

        @Setup
        public void setup() throws IOException {
            CsvFormat format = CsvFormat.DEFAULT
                    .toBuilder()
                    .delimiter(Constant.SEPARATOR)
                    .separator(NewLine.UNIX)
                    .quote(Constant.DELIMITER)
                    .build();

            input = CsvReader.of(new InfiniteDataReader(Constant.data), format);
        }

        @TearDown
        public void teardown() throws IOException {
            input.close();
        }
    }

    @Benchmark
    public void readLine(ReadState state, Blackhole blackhole) throws IOException {
        if (state.input.readLine()) {
            while (state.input.readField()) {
                blackhole.consume(state.input.getField().toString());
            }
        }
    }
}
