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

import _test.Top5GridMonthly;
import nbbrd.picocsv.Csv;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * @author Philippe Charles
 */
public class ReadColumnsDemo {

    public static void main(String[] args) throws IOException {
        try (Csv.Reader reader = Top5GridMonthly.open()) {
            readColumns(reader, Cookbook.mapperByIndex(2, 4))
                    .stream()
                    .limit(3)
                    .map(Arrays::toString)
                    .forEach(System.out::println);
        }

        System.out.println("---");

        try (Csv.Reader reader = Top5GridMonthly.open()) {
            readColumns(reader, Cookbook.mapperByName("Firefox", "Safari"))
                    .stream()
                    .limit(3)
                    .map(Arrays::toString)
                    .forEach(System.out::println);
        }
    }

    private static List<String[]> readColumns(Csv.Reader reader, Function<String[], int[]> mapper) throws IOException {
        if (Cookbook.skipComments(reader)) {
            List<String[]> result = new ArrayList<>();

            String[] header = Cookbook.readLineOfUnknownSize(reader);

            int[] mapping = mapper.apply(header);
            int size = getFieldsSize(mapping);

            result.add(Cookbook.readLineOfFixedSize(Cookbook.asLineReader(header), mapping, size));
            while (Cookbook.skipComments(reader)) {
                result.add(Cookbook.readLineOfFixedSize(reader, mapping, size));
            }

            return result;
        }
        return Collections.emptyList();
    }

    private static int getFieldsSize(int[] mapping) {
        return (int) IntStream.of(mapping).filter(Cookbook::isValidIndex).count();
    }
}
