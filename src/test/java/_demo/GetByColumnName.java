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
package _demo;

import nbbrd.picocsv.Csv;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Philippe Charles
 */
public class GetByColumnName {

    public static void main(String[] args) throws IOException {
        try (Csv.Reader reader = Top5GridMonthly.open()) {
            getByColumnName(reader, Arrays.asList("Firefox", "Safari"))
                    .forEach(item -> System.out.println(Arrays.toString(item)));
        }
    }

    private static List<String[]> getByColumnName(Csv.Reader reader, List<String> columnNames) throws IOException {
        List<String[]> result = new ArrayList<>();
        int fieldIndex;

        if (reader.readLine()) {
            List<Integer> columnIndexes = new ArrayList<>();

            String[] columns = new String[columnNames.size()];
            fieldIndex = 0;
            while (reader.readField()) {
                String field = reader.toString();
                int position = columnNames.indexOf(field);
                if (position != -1) {
                    columnIndexes.add(fieldIndex);
                    columns[position] = field;
                }
                fieldIndex++;
            }
            result.add(columns);

            while (reader.readLine()) {
                String[] row = new String[columnNames.size()];
                fieldIndex = 0;
                while (reader.readField()) {
                    int position = columnIndexes.indexOf(fieldIndex);
                    if (position != -1) {
                        row[position] = reader.toString();
                    }
                    fieldIndex++;
                }
                result.add(row);
            }
        }
        return result;
    }
}
