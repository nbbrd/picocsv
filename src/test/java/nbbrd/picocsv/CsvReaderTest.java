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
import static _test.Sample.ILLEGAL_FORMAT;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import static nbbrd.picocsv.Csv.Format.RFC4180;
import static org.assertj.core.api.Assertions.*;
import org.junit.Test;

/**
 *
 * @author Philippe Charles
 */
public class CsvReaderTest {

    @Test
    public void testPathFactory() {
        assertThatNullPointerException()
                .isThrownBy(() -> Csv.Reader.of((Path) null, UTF_8, RFC4180))
                .withMessageContaining("file");

        assertThatNullPointerException()
                .isThrownBy(() -> Csv.Reader.of(QuickReader.newInputFile("", UTF_8), null, RFC4180))
                .withMessageContaining("encoding");

        assertThatNullPointerException()
                .isThrownBy(() -> Csv.Reader.of(QuickReader.newInputFile("", UTF_8), UTF_8, null))
                .withMessageContaining("format");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> Csv.Reader.of(QuickReader.newInputFile("", UTF_8), UTF_8, ILLEGAL_FORMAT))
                .withMessageContaining("format");
    }

    @Test
    public void testStreamFactory() {
        assertThatNullPointerException()
                .isThrownBy(() -> Csv.Reader.of((InputStream) null, UTF_8, RFC4180))
                .withMessageContaining("stream");

        assertThatNullPointerException()
                .isThrownBy(() -> Csv.Reader.of(QuickReader.newInputStream("", UTF_8), null, RFC4180))
                .withMessageContaining("encoding");

        assertThatNullPointerException()
                .isThrownBy(() -> Csv.Reader.of(QuickReader.newInputStream("", UTF_8), UTF_8, null))
                .withMessageContaining("format");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> Csv.Reader.of(QuickReader.newInputStream("", UTF_8), UTF_8, ILLEGAL_FORMAT))
                .withMessageContaining("format");
    }

    @Test
    public void testReaderFactory() {
        assertThatNullPointerException()
                .isThrownBy(() -> Csv.Reader.of((Reader) null, RFC4180))
                .withMessageContaining("charReader");

        assertThatNullPointerException()
                .isThrownBy(() -> Csv.Reader.of(QuickReader.newCharReader(""), null))
                .withMessageContaining("format");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> Csv.Reader.of(QuickReader.newCharReader(""), ILLEGAL_FORMAT))
                .withMessageContaining("format");
    }

    @Test
    public void testAllSamples() throws IOException {
        for (QuickReader reader : QuickReader.values()) {
            for (Charset encoding : Sample.CHARSETS) {
                for (Sample sample : Sample.SAMPLES) {
                    assertValid(reader, encoding, sample);
                }
            }
        }
    }

    @Test
    public void testSkip() throws IOException {
        for (QuickReader reader : QuickReader.values()) {
            for (Charset encoding : Sample.CHARSETS) {
                for (Sample sample : Sample.SAMPLES) {
                    switch (sample.getRows().size()) {
                        case 0:
                        case 1:
                            assertThat(reader.readValue(this::skipFirst, encoding, sample.getFormat(), sample.getContent()))
                                    .isEmpty();
                            break;
                        default:
                            assertThat(reader.readValue(this::skipFirst, encoding, sample.getFormat(), sample.getContent()))
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
                    assertThatIllegalStateException()
                            .isThrownBy(() -> reader.read(readFieldBeforeLine, encoding, sample.getFormat(), sample.getContent()));
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
            assertValid(type, UTF_8, invalidButStillOk);
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
            assertValid(type, UTF_8, overflow);
        }
    }

    @Test
    public void testEmptyLine() throws IOException {
        try (Reader charReader = new StringReader(Sample.EMPTY_LINES.getContent())) {
            try (Csv.Reader reader = Csv.Reader.of(charReader, Sample.EMPTY_LINES.getFormat())) {
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
        try (Reader charReader = new StringReader(Sample.SIMPLE.getContent())) {
            try (Csv.Reader reader = Csv.Reader.of(charReader, Sample.SIMPLE.getFormat())) {
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

    private static void assertValid(Sample sample) throws IOException {
        assertValid(QuickReader.CHAR_READER, UTF_8, sample);
    }

    private static void assertValid(QuickReader r, Charset encoding, Sample sample) throws IOException {
        assertThat(r.readValue(Row::read, encoding, sample.getFormat(), sample.getContent()))
                .describedAs("Reading '%s' with '%s'", sample.getName(), r)
                .containsExactlyElementsOf(sample.getRows());
    }
}
