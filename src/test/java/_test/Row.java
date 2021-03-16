/*
 * Copyright 2019 National Bank of Belgium
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
package _test;

import nbbrd.picocsv.Csv;
import org.apache.commons.text.StringEscapeUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Philippe Charles
 */
@lombok.Value
public class Row {

    public static final Row EMPTY_ROW = Row.of();
    public static final Row EMPTY_FIELD = Row.of("");

    public static Row of(String... fields) {
        return new Row(Arrays.asList(fields));
    }

    @lombok.NonNull
    List<String> fields;

    @Override
    public String toString() {
        return fields
                .stream()
                .map(field -> "{" + StringEscapeUtils.escapeJava(field) + "}")
                .collect(Collectors.joining(","));
    }

    @FunctionalInterface
    public interface NonEmptyConsumer {

        void accept(Csv.Reader reader, List<Row> list) throws IOException;
    }

    @FunctionalInterface
    public interface EmptyConsumer {

        void accept(List<Row> list) throws IOException;

        static EmptyConsumer noOp() {
            return list -> {
            };
        }

        static EmptyConsumer constant(Row row) {
            return list -> list.add(row);
        }
    }

    public static Row read(Csv.Reader reader) throws IOException {
        List<String> fields = new ArrayList<>();
        do {
            fields.add(reader.toString());
        } while (reader.readField());
        return new Row(fields);
    }

    public static List<Row> readAll(Csv.Reader reader, EmptyConsumer onEmpty, NonEmptyConsumer onNonEmpty) throws IOException {
        List<Row> result = new ArrayList<>();
        while (reader.readLine()) {
            if (reader.readField()) {
                onNonEmpty.accept(reader, result);
            } else {
                onEmpty.accept(result);
            }
        }
        return result;
    }

    public static void writeAll(List<Row> rows, Csv.Writer writer) throws IOException {
        for (Row row : rows) {
            for (String field : row.getFields()) {
                writer.writeField(field);
            }
            writer.writeEndOfLine();
        }
    }
}
