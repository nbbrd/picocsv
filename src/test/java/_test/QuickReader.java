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
import java.io.StringReader;

/**
 * @author Philippe Charles
 */
@lombok.experimental.UtilityClass
public class QuickReader {

    public static <T> T readValue(Parser<T> parser, String input, Csv.Format format, Csv.Parsing options) throws IOException {
        try (java.io.Reader charReader = new StringReader(input)) {
            try (Csv.Reader reader = Csv.Reader.of(format, options, charReader, Csv.DEFAULT_CHAR_BUFFER_SIZE)) {
                return parser.accept(reader);
            }
        }
    }

    public static void read(VoidParser parser, String input, Csv.Format format, Csv.Parsing options) throws IOException {
        readValue(stream -> {
            parser.accept(stream);
            return null;
        }, input, format, options);
    }

    @FunctionalInterface
    public interface Parser<T> {

        T accept(Csv.Reader reader) throws IOException;
    }

    @FunctionalInterface
    public interface VoidParser {

        void accept(Csv.Reader reader) throws IOException;
    }
}
