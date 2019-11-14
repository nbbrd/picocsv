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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import nbbrd.picocsv.Csv;

/**
 *
 * @author Philippe Charles
 */
@lombok.AllArgsConstructor
public enum QuickReader {

    BYTE_ARRAY(StreamType.STREAM) {
        @Override
        public <T> T readValue(QuickReader.Parser<T> parser, Charset encoding, Csv.Format format, String input) throws IOException {
            try (InputStream stream = newInputStream(input, encoding)) {
                try (Csv.Reader reader = Csv.Reader.of(stream, encoding, format)) {
                    return parser.accept(reader);
                }
            }
        }
    },
    FILE(StreamType.FILE) {
        @Override
        public <T> T readValue(QuickReader.Parser<T> parser, Charset encoding, Csv.Format format, String input) throws IOException {
            Path file = newInputFile(input, encoding);
            try (Csv.Reader reader = Csv.Reader.of(file, encoding, format)) {
                return parser.accept(reader);
            }
        }
    },
    FILE_STREAM(StreamType.STREAM) {
        @Override
        public <T> T readValue(QuickReader.Parser<T> parser, Charset encoding, Csv.Format format, String input) throws IOException {
            Path file = newInputFile(input, encoding);
            try (InputStream stream = Files.newInputStream(file)) {
                try (Csv.Reader reader = Csv.Reader.of(stream, encoding, format)) {
                    return parser.accept(reader);
                }
            }
        }
    },
    READER(StreamType.OBJECT) {
        @Override
        public <T> T readValue(QuickReader.Parser<T> parser, Charset encoding, Csv.Format format, String input) throws IOException {
            try (Reader object = newReader(input)) {
                try (Csv.Reader reader = Csv.Reader.of(object, format)) {
                    return parser.accept(reader);
                }
            }
        }
    };

    @lombok.Getter
    private final StreamType type;

    abstract public <T> T readValue(Parser<T> parser, Charset encoding, Csv.Format format, String input) throws IOException;

    public void read(VoidParser parser, Charset encoding, Csv.Format format, String input) throws IOException {
        readValue(stream -> {
            parser.accept(stream);
            return null;
        }, encoding, format, input);
    }

    public interface Parser<T> {

        T accept(Csv.Reader reader) throws IOException;
    }

    public interface VoidParser {

        void accept(Csv.Reader reader) throws IOException;
    }

    public static Path newInputFile(String content, Charset charset) throws IOException {
        File result = File.createTempFile("input", ".csv");
        result.deleteOnExit();
        Files.write(result.toPath(), content.getBytes(charset));
        return result.toPath();
    }

    public static InputStream newInputStream(String content, Charset charset) {
        return new ByteArrayInputStream(content.getBytes(charset));
    }

    public static Reader newReader(String content) {
        return new StringReader(content);
    }
}
