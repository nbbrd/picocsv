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
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static _demo.Cookbook.*;

/**
 * @author Philippe Charles
 */
@lombok.Value
@lombok.Builder(toBuilder = true)
public class TableReader<T> {

    @NonNull Function<String[], RowReader<T>> rowFactory;

    @lombok.With
    @lombok.Builder.Default
    int skipLines = 0;

    public static <X> @NonNull TableReaderBuilder<X> builder(@NonNull Function<String[], RowReader<X>> factory) {
        return new TableReaderBuilder<X>().rowFactory(factory);
    }

    public static @NonNull TableReader<String[]> byColumnIndex(@NonNull int... indexes) {
        return builder(rowFactoryOfMapper(mapperByIndex(indexes), indexes.length)).build();
    }

    public static @NonNull TableReader<String[]> byColumnName(@NonNull String... names) {
        return builder(rowFactoryOfMapper(mapperByName(names), names.length)).build();
    }

    public static <X> @NonNull TableReader<X> byLine(@NonNull RowReader<X> function) {
        return builder(ignore -> function).build();
    }

    public @NonNull Stream<T> lines(@NonNull Csv.Reader reader) {
        try {
            return linesWithoutHeader(reader, readHeaderLine(reader));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public @NonNull Stream<T> linesWithoutHeader(@NonNull Csv.Reader reader, @NonNull String... header) {
        try {
            skipLines(reader, skipLines);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        RowReader<T> rowReader = rowFactory.apply(header);
        return StreamSupport
                .stream(Spliterators.spliteratorUnknownSize(new RowIterator(reader), Spliterator.ORDERED | Spliterator.NONNULL), false)
                .map(rowReader.asUnchecked());
    }

    private static Function<String[], RowReader<String[]>> rowFactoryOfMapper(Function<String[], int[]> mapper, int columnCount) {
        return header -> rowReaderOfMapping(mapper.apply(header), columnCount);
    }

    private static RowReader<String[]> rowReaderOfMapping(int[] mapping, int columnCount) {
        return line -> readFieldsOfFixedSize(line, mapping, columnCount);
    }
}
