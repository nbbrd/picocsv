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
import java.util.Arrays;
import java.util.Optional;

/**
 * @author Philippe Charles
 */
public class ReadHeaderDemo {

    public static void main(String[] args) throws IOException {
        try (Csv.Reader reader = Top5GridMonthly.open()) {
            System.out.println(
                    readHeader(reader)
                            .map(Arrays::toString)
                            .orElse("No header")
            );
        }
    }

    private static Optional<String[]> readHeader(Csv.Reader reader) throws IOException {
        return Cookbook.skipComments(reader) ? Optional.of(Cookbook.readFieldsOfUnknownSize(reader)) : Optional.empty();
    }
}
