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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Philippe Charles
 */
@lombok.Value
public final class Row {

    public static Row of(String... fields) {
        return new Row(Arrays.asList(fields));
    }

    private final List<String> fields;

    public static List<Row> readAll(Csv.Reader reader) throws IOException {
        List<Row> result = new ArrayList<>();
        while (reader.readLine()) {
            List<String> fields = new ArrayList<>();
            while (reader.readField()) {
                fields.add(reader.toString());
            }
            result.add(new Row(fields));
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
