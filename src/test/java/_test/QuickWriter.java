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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import nbbrd.picocsv.Csv;

/**
 *
 * @author Philippe Charles
 */
@lombok.AllArgsConstructor
public enum QuickWriter {

    BYTE_ARRAY(StreamType.STREAM) {
        @Override
        public <T> String writeValue(T value, QuickWriter.Formatter<T> formatter, Charset encoding, Csv.Format format) throws IOException {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            try (Csv.Writer writer = Csv.Writer.of(result, encoding, format)) {
                formatter.accept(value, writer);
            }
            return result.toString(encoding.name());
        }
    },
    FILE(StreamType.FILE) {
        @Override
        public <T> String writeValue(T value, QuickWriter.Formatter<T> formatter, Charset encoding, Csv.Format format) throws IOException {
            Path file = newOutputFile();
            try (Csv.Writer writer = Csv.Writer.of(file, encoding, format)) {
                formatter.accept(value, writer);
            }
            return readFile(file, encoding);
        }
    },
    FILE_STREAM(StreamType.STREAM) {
        @Override
        public <T> String writeValue(T value, QuickWriter.Formatter<T> formatter, Charset encoding, Csv.Format format) throws IOException {
            Path file = newOutputFile();
            try (OutputStream stream = Files.newOutputStream(file)) {
                try (Csv.Writer writer = Csv.Writer.of(stream, encoding, format)) {
                    formatter.accept(value, writer);
                }
                return readFile(file, encoding);
            }
        }
    },
    WRITER(StreamType.OBJECT) {
        @Override
        public <T> String writeValue(T value, QuickWriter.Formatter<T> formatter, Charset encoding, Csv.Format format) throws IOException {
            try (Writer result = newWriter()) {
                try (Csv.Writer writer = Csv.Writer.of(result, format)) {
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

    public interface Formatter<T> {

        void accept(T value, Csv.Writer writer) throws IOException;
    }

    public interface VoidFormatter {

        void accept(Csv.Writer writer) throws IOException;
    }

    public static Path newOutputFile() throws IOException {
        File temp = File.createTempFile("output", ".csv");
        temp.deleteOnExit();
        return temp.toPath();
    }

    public static OutputStream newOutputStream() {
        return new ByteArrayOutputStream();
    }

    public static Writer newWriter() {
        return new StringWriter();
    }

    public static String readFile(Path file, Charset charset) throws IOException {
        return new String(Files.readAllBytes(file), charset);
    }
}
