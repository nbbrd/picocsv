/*
 * Copyright 2022 National Bank of Belgium
 *
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved
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
package _demo.api1;

import lombok.NonNull;
import nbbrd.picocsv.Csv;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Function;
import java.util.stream.Stream;

import static _demo.Cookbook.*;

/**
 * @author Philippe Charles
 */
public interface TableReader<T> {

    Function<Csv.LineReader, T> getRowReader(String[] header);

    static @NonNull TableReader<String[]> onStringArray(@NonNull Function<String[], int[]> mapper, int columnCount) {
        return header -> {
            int[] mapping = mapper.apply(header);
            return line -> {
                try {
                    return readFieldsOfFixedSize(line, mapping, columnCount);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            };
        };
    }

    static @NonNull TableReader<String[]> byColumnIndex(@NonNull int... indexes) {
        return onStringArray(mapperByIndex(indexes), indexes.length);
    }

    static @NonNull TableReader<String[]> byColumnName(@NonNull String... names) {
        return onStringArray(mapperByName(names), names.length);
    }

    static <X> @NonNull TableReader<X> byLine(@NonNull Function<Csv.LineReader, X> function) {
        return ignore -> function;
    }

    default @NonNull Stream<T> lines(@NonNull Csv.Reader reader) {
        try {
            return lines(reader, readHeaderLine(reader));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    default @NonNull Stream<T> lines(@NonNull Csv.Reader reader, @NonNull String[] header) {
        return stream(reader, getRowReader(header));
    }
}
