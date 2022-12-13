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
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Philippe Charles
 */
@lombok.Value
@lombok.Builder
public class TableWriter<T> {

    @lombok.Singular
    @NonNull List<Column<T>> columns;

    @lombok.With
    boolean ignoreHeader;

    public static <X> @NonNull TableWriterBuilder<X> builder(@NonNull Class<X> type) {
        return new TableWriterBuilder<X>();
    }

    public static <X> @NonNull TableWriter<X> ofBean(@NonNull Class<X> type) {
        return new TableWriter<>(Column.ofBean(type), false);
    }

    public void lines(@NonNull Csv.Writer writer, @NonNull Stream<T> lines) {
        try {
            if (!ignoreHeader) {
                for (Column<T> column : columns) {
                    writer.writeField(column.getName());
                }
                writer.writeEndOfLine();
            }
            for (T value : (Iterable<T>) lines::iterator) {
                for (Column<T> column : columns) {
                    writer.writeField(column.getFormatter().apply(value));
                }
                writer.writeEndOfLine();
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @lombok.Value(staticConstructor = "of")
    public static class Column<T> {

        @NonNull String name;

        @NonNull Function<? super T, ? extends CharSequence> formatter;

        public static <X> List<Column<X>> ofBean(Class<X> type) {
            return Stream.of(type.getDeclaredFields())
                    .map(field -> Column.<X>of(field.getName(), value -> {
                        try {
                            return Objects.toString(field.get(value));
                        } catch (IllegalAccessException ex) {
                            throw new RuntimeException(ex);
                        }
                    }))
                    .collect(Collectors.toList());
        }
    }
}
