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
package nbbrd.picocsv;

import _test.QuickReader;
import _test.QuickReader.VoidParser;
import _test.Row;
import _test.Sample;
import org.junit.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static _test.Sample.ILLEGAL_FORMAT;
import static java.nio.charset.StandardCharsets.UTF_8;
import static nbbrd.picocsv.Csv.DEFAULT_CHAR_BUFFER_SIZE;
import static org.assertj.core.api.Assertions.*;

/**
 * @author Philippe Charles
 */
public class CsvReaderTest {

    @Test
    public void testReaderFactory() {
        assertThatNullPointerException()
                .isThrownBy(() -> Csv.Reader.of((Reader) null, DEFAULT_CHAR_BUFFER_SIZE, Csv.Parsing.DEFAULT))
                .withMessageContaining("charReader");

        assertThatNullPointerException()
                .isThrownBy(() -> Csv.Reader.of(new StringReader(""), DEFAULT_CHAR_BUFFER_SIZE, null))
                .withMessageContaining("options");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> Csv.Reader.of(new StringReader(""), DEFAULT_CHAR_BUFFER_SIZE, Csv.Parsing.DEFAULT.toBuilder().format(ILLEGAL_FORMAT).build()))
                .withMessageContaining("format");
    }

    @Test
    public void testAllSamples() throws IOException {
        for (QuickReader reader : QuickReader.values()) {
            for (Charset encoding : Sample.CHARSETS) {
                for (Sample sample : Sample.SAMPLES) {
                    assertValid(reader, encoding, sample, Csv.Parsing.DEFAULT);
                    for (Csv.NewLine newLine : Csv.NewLine.values()) {
                        assertValid(reader, encoding, sample.withNewLine(newLine), Csv.Parsing.DEFAULT.toBuilder().lenientSeparator(true).build());
                    }
                }
            }
        }
    }

    @Test
    public void testSkip() throws IOException {
        for (QuickReader reader : QuickReader.values()) {
            for (Charset encoding : Sample.CHARSETS) {
                for (Sample sample : Sample.SAMPLES) {
                    Csv.Parsing options = Csv.Parsing.DEFAULT.toBuilder().format(sample.getFormat()).build();
                    switch (sample.getRows().size()) {
                        case 0:
                        case 1:
                            assertThat(reader.readValue(this::skipFirst, encoding, sample.getContent(), options))
                                    .isEmpty();
                            break;
                        default:
                            assertThat(reader.readValue(this::skipFirst, encoding, sample.getContent(), options))
                                    .element(0)
                                    .isEqualTo(sample.getRows().get(1));
                            break;
                    }
                }
            }
        }
    }

    private List<Row> skipFirst(Csv.Reader reader) throws IOException {
        reader.readLine();
        return Row.read(reader);
    }

    @Test
    public void testReadFieldBeforeLine() throws IOException {
        VoidParser readFieldBeforeLine = Csv.Reader::readField;

        for (QuickReader reader : QuickReader.values()) {
            for (Charset encoding : Sample.CHARSETS) {
                for (Sample sample : Sample.SAMPLES) {
                    Csv.Parsing options = Csv.Parsing.DEFAULT.toBuilder().format(sample.getFormat()).build();
                    assertThatIllegalStateException()
                            .isThrownBy(() -> reader.read(readFieldBeforeLine, encoding, sample.getContent(), options));
                }
            }
        }
    }

    @Test
    public void testNonQuotedNonNewLineChar() throws IOException {
        Sample invalidButStillOk = Sample
                .builder()
                .name("Invalid but still ok")
                .format(Csv.Format.RFC4180)
                .content("\r\r\n")
                .rowOf("\r")
                .build();

        for (QuickReader type : QuickReader.values()) {
            assertValid(type, UTF_8, invalidButStillOk, Csv.Parsing.DEFAULT);
        }
    }

    @Test
    public void testReusableFieldOverflow() throws IOException {
        String field1 = IntStream.range(0, 70).mapToObj(i -> "A").collect(Collectors.joining());
        String field2 = IntStream.range(0, 10).mapToObj(i -> "B").collect(Collectors.joining());
        Sample overflow = Sample
                .builder()
                .name("overflow")
                .format(Csv.Format.RFC4180)
                .content(field1 + "," + field2)
                .rowOf(field1, field2)
                .build();

        for (QuickReader type : QuickReader.values()) {
            assertValid(type, UTF_8, overflow, Csv.Parsing.DEFAULT);
        }
    }

    @Test
    public void testEmptyLine() throws IOException {
        Csv.Parsing options = Csv.Parsing.DEFAULT.toBuilder().format(Sample.EMPTY_LINES.getFormat()).build();
        try (Reader charReader = new StringReader(Sample.EMPTY_LINES.getContent())) {
            try (Csv.Reader reader = Csv.Reader.of(charReader, DEFAULT_CHAR_BUFFER_SIZE, options)) {
                assertThat(reader.readLine()).isTrue();
                assertThat(reader.readField()).isFalse();
                assertThat(reader.readLine()).isTrue();
                assertThat(reader.readField()).isFalse();
                assertThat(reader.readLine()).isFalse();
            }
        }
    }

    @Test
    public void testEmptyFirstField() throws IOException {
        assertValid(Sample
                .builder()
                .name("Empty first field")
                .format(Csv.Format.RFC4180)
                .content(",B1\r\nA2,B2\r\n")
                .rowOf("", "B1")
                .rowOf("A2", "B2")
                .build());
    }

    @Test
    public void testCharSequence() throws IOException {
        Csv.Parsing options = Csv.Parsing.DEFAULT.toBuilder().format(Sample.SIMPLE.getFormat()).build();
        try (Reader charReader = new StringReader(Sample.SIMPLE.getContent())) {
            try (Csv.Reader reader = Csv.Reader.of(charReader,DEFAULT_CHAR_BUFFER_SIZE, options)) {
                CharSequence chars = reader;
                reader.readLine();
                reader.readField();

                assertThat(chars).hasSize(2);

                assertThat(chars.charAt(0)).isEqualTo('A');
                assertThat(chars.charAt(1)).isEqualTo('1');
                assertThatExceptionOfType(IndexOutOfBoundsException.class)
                        .isThrownBy(() -> chars.charAt(-1));
                assertThatExceptionOfType(IndexOutOfBoundsException.class)
                        .isThrownBy(() -> chars.charAt(2));

                assertThat(chars.subSequence(0, 2)).isEqualTo("A1");
                assertThat(chars.subSequence(1, 2)).isEqualTo("1");
                assertThat(chars.subSequence(2, 2)).isEmpty();
                assertThatExceptionOfType(IndexOutOfBoundsException.class)
                        .isThrownBy(() -> chars.subSequence(-1, 2));
                assertThatExceptionOfType(IndexOutOfBoundsException.class)
                        .isThrownBy(() -> chars.subSequence(0, 3));
                assertThatExceptionOfType(IndexOutOfBoundsException.class)
                        .isThrownBy(() -> chars.subSequence(1, 0));
            }
        }
    }

    @Test
    public void testFieldOverflow() throws IOException {
        Csv.Parsing valid = Csv.Parsing.DEFAULT.toBuilder().format(Sample.SIMPLE.getFormat()).maxCharsPerField(2).build();
        try (Reader charReader = new StringReader(Sample.SIMPLE.getContent())) {
            assertThatCode(() -> {
                try (Csv.Reader reader = Csv.Reader.of(charReader,DEFAULT_CHAR_BUFFER_SIZE, valid)) {
                    Row.read(reader);
                }
            }).doesNotThrowAnyException();
        }

        Csv.Parsing invalid = Csv.Parsing.DEFAULT.toBuilder().format(Sample.SIMPLE.getFormat()).maxCharsPerField(1).build();
        try (Reader charReader = new StringReader(Sample.SIMPLE.getContent())) {
            assertThatIOException().isThrownBy(() -> {
                try (Csv.Reader reader = Csv.Reader.of(charReader,DEFAULT_CHAR_BUFFER_SIZE, invalid)) {
                    Row.read(reader);
                }
            }).withMessageContaining("Field overflow");
        }
    }

    private static void assertValid(Sample sample) throws IOException {
        assertValid(QuickReader.CHAR_READER, UTF_8, sample, Csv.Parsing.DEFAULT);
    }

    private static void assertValid(QuickReader r, Charset encoding, Sample sample, Csv.Parsing options) throws IOException {
        assertThat(r.readValue(Row::read, encoding, sample.getContent(), options.toBuilder().format(sample.getFormat()).build()))
                .describedAs("Reading '%s' with '%s'", sample.getName(), r)
                .containsExactlyElementsOf(sample.getRows());
    }
}
