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
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;

/**
 * @author Philippe Charles
 */
@lombok.AllArgsConstructor
public enum QuickWriter {

    CHAR_WRITER(StreamType.OBJECT) {
        @Override
        public <T> String writeValue(T value, QuickWriter.Formatter<T> formatter, Charset encoding, Csv.Format format) throws IOException {
            try (Writer result = new StringWriter()) {
                try (Csv.Writer writer = Csv.Writer.of(result, Csv.DEFAULT_CHAR_BUFFER_SIZE, Csv.Formatting.builder().format(format).build())) {
                    formatter.accept(value, writer);
                }
                return result.toString();
            }
        }
    };

    @lombok.Getter
    private final StreamType type;

    abstract public <T> String writeValue(T value, Formatter<T> formatter, Charset encoding, Csv.Format format) throws IOException;

    public String write(VoidFormatter formatter, Charset encoding, Csv.Format format) throws IOException {
        return writeValue(null, (value, stream) -> formatter.accept(stream), encoding, format);
    }

    @FunctionalInterface
    public interface Formatter<T> {

        void accept(T value, Csv.Writer writer) throws IOException;
    }

    @FunctionalInterface
    public interface VoidFormatter {

        void accept(Csv.Writer writer) throws IOException;
    }
}
