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
public class GetByColumnIndex {

    public static void main(String[] args) throws IOException {
        try (Csv.Reader reader = Top5GridMonthly.open()) {
            getByColumnIndex(reader, Arrays.asList(2, 4))
                    .forEach(item -> System.out.println(Arrays.toString(item)));
        }
    }

    private static List<String[]> getByColumnIndex(Csv.Reader reader, List<Integer> columnIndexes) throws IOException {
        List<String[]> result = new ArrayList<>();
        int fieldIndex;

        if (reader.readLine()) {
            String[] columns = new String[columnIndexes.size()];
            fieldIndex = 0;
            while (reader.readField()) {
                int position = columnIndexes.indexOf(fieldIndex);
                if (position != -1) {
                    columns[position] = reader.toString();
                }
                fieldIndex++;
            }
            result.add(columns);

            while (reader.readLine()) {
                String[] row = new String[columnIndexes.size()];
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
