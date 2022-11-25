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

import lombok.AccessLevel;
import lombok.NonNull;
import nbbrd.picocsv.Csv;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static _demo.Cookbook.*;

/**
 * @author Philippe Charles
 */
@lombok.AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TableReader {

    public static @NonNull TableReader byColumnIndex(@NonNull int... indexes) {
        return new TableReader(mapperByIndex(indexes), indexes.length, -1);
    }

    public static @NonNull TableReader byColumnIndexNoHeader(int maxColumnCount, @NonNull int... indexes) {
        return new TableReader(mapperByIndex(indexes), indexes.length, maxColumnCount);
    }

    public static @NonNull TableReader byColumnName(@NonNull String... names) {
        return new TableReader(mapperByName(names), names.length, -1);
    }

    @NonNull Function<String[], int[]> mapper;

    int columnCount;

    int maxColumnCount;

    private int[] getMapping(Csv.Reader reader) throws IOException {
        return mapper.apply(getHeader(reader));
    }

    private String[] getHeader(Csv.Reader reader) throws IOException {
        if (maxColumnCount != -1) {
            String[] result = new String[maxColumnCount];
            Arrays.fill(result, "");
            return result;
        }
        if (!skipComments(reader)) {
            throw new IOException("Missing header");
        }
        return readFieldsOfUnknownSize(reader);
    }

    public void forEach(@NonNull Csv.Reader reader, @NonNull Consumer<? super String[]> consumer) throws IOException {
        int[] mapping = getMapping(reader);
        while (skipComments(reader)) {
            consumer.accept(readFieldsOfFixedSize(reader, mapping, columnCount));
        }
    }

    public @NonNull List<String[]> toList(@NonNull Csv.Reader reader) throws IOException {
        List<String[]> result = new ArrayList<>();
        int[] mapping = getMapping(reader);
        while (skipComments(reader)) {
            result.add(readFieldsOfFixedSize(reader, mapping, columnCount));
        }
        return result;
    }

    public @NonNull Stream<String[]> lines(@NonNull Csv.Reader reader) {
        try {
            return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                    new RowIterator(reader, getMapping(reader), columnCount), Spliterator.ORDERED | Spliterator.NONNULL), false);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @lombok.RequiredArgsConstructor
    private static final class RowIterator implements Iterator<String[]> {

        private final Csv.Reader reader;

        private final int[] mapping;

        private final int columnCount;

        @Override
        public boolean hasNext() {
            try {
                return skipComments(reader);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public String[] next() {
            try {
                return readFieldsOfFixedSize(reader, mapping, columnCount);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
