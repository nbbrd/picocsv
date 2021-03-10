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
import nbbrd.picocsv.Csv;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;

/**
 * @author Philippe Charles
 */
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 3)
@BenchmarkMode(Mode.Throughput)
public class ByFieldBenchmark {

    @State(Scope.Benchmark)
    public static class ReadState {

        Csv.Reader input;

        @Setup
        public void setup() throws IOException {
            Csv.Format format = Csv.Format.DEFAULT
                    .toBuilder()
                    .delimiter(Constant.SEPARATOR)
                    .separator(Csv.Format.UNIX_SEPARATOR)
                    .quote(Constant.DELIMITER)
                    .build();

            input = Csv.Reader.of(format, Csv.Parsing.DEFAULT, new InfiniteDataReader(Constant.data), Constant.data.length() * 32);
        }

        @TearDown
        public void teardown() throws IOException {
            input.close();
        }
    }

    @Benchmark
    public void readLine(ReadState state, Blackhole blackhole) throws IOException {
        state.input.readLine();
        while (state.input.readField()) {
            blackhole.consume(state.input.toString());
        }
    }
}
