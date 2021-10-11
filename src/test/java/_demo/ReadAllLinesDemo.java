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
public class ReadAllLinesDemo {

    public static void main(String[] args) throws IOException {
        try (Csv.Reader reader = Top5GridMonthly.open()) {
            readAllLines(reader)
                    .forEach(item -> System.out.println(Arrays.toString(item)));
        }
    }

    private static List<String[]> readAllLines(Csv.Reader reader) throws IOException {
        List<String[]> result = new ArrayList<>();
        List<String> row = new ArrayList<>();

        while (reader.readLine()) {
            if (!reader.isComment()) {
                while (reader.readField()) {
                    row.add(reader.toString());
                }
                result.add(row.toArray(new String[row.size()]));
                row.clear();
            }
        }
        return result;
    }
}
