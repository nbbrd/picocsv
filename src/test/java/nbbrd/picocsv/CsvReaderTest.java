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

import _test.QuickReader.VoidParser;
import _test.Row;
import _test.Sample;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static _test.QuickReader.read;
import static _test.QuickReader.readValue;
import static _test.Sample.INVALID_FORMAT;
import static nbbrd.picocsv.Csv.DEFAULT_CHAR_BUFFER_SIZE;
import static org.assertj.core.api.Assertions.*;

/**
 * @author Philippe Charles
 */
public class CsvReaderTest {

    @Test
    public void testReaderFactory() {
        assertThatNullPointerException()
                .isThrownBy(() -> Csv.Reader.of(Csv.Format.DEFAULT, Csv.Parsing.DEFAULT, null, DEFAULT_CHAR_BUFFER_SIZE))
                .withMessageContaining("charReader");

        assertThatNullPointerException()
                .isThrownBy(() -> Csv.Reader.of(null, Csv.Parsing.DEFAULT, new StringReader(""), DEFAULT_CHAR_BUFFER_SIZE))
                .withMessageContaining("format");

        assertThatNullPointerException()
                .isThrownBy(() -> Csv.Reader.of(Csv.Format.DEFAULT, null, new StringReader(""), DEFAULT_CHAR_BUFFER_SIZE))
                .withMessageContaining("options");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> Csv.Reader.of(INVALID_FORMAT, Csv.Parsing.DEFAULT, new StringReader(""), DEFAULT_CHAR_BUFFER_SIZE))
                .withMessageContaining("format");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> Csv.Reader.of(Csv.Format.DEFAULT, Csv.Parsing.DEFAULT, new StringReader(""), 0))
                .withMessageContaining("charBufferSize");

        Csv.Parsing invalidOptions = Csv.Parsing.DEFAULT.toBuilder().maxCharsPerField(0).build();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> Csv.Reader.of(Csv.Format.DEFAULT, invalidOptions, new StringReader(""), DEFAULT_CHAR_BUFFER_SIZE))
                .withMessageContaining("options must be valid");
    }

    @Test
    public void testAllSamples() throws IOException {
        for (Sample sample : Sample.SAMPLES) {
            assertValid(sample, Csv.Parsing.DEFAULT);
            for (Csv.NewLine newLine : Csv.NewLine.values()) {
                assertValid(sample.withNewLine(newLine), Csv.Parsing.DEFAULT.toBuilder().lenientSeparator(true).build());
            }
        }
    }

    @Test
    public void testSkip() throws IOException {
        for (Sample sample : Sample.SAMPLES) {
            switch (sample.getRows().size()) {
                case 0:
                case 1:
                    assertThat(readValue(this::skipFirst, sample.getContent(), sample.getFormat(), Csv.Parsing.DEFAULT))
                            .describedAs(sample.asDescription("Reading"))
                            .isEmpty();
                    break;
                default:
                    assertThat(readValue(this::skipFirst, sample.getContent(), sample.getFormat(), Csv.Parsing.DEFAULT))
                            .describedAs(sample.asDescription("Reading"))
                            .element(0)
                            .isEqualTo(sample.getRows().get(1));
                    break;
            }
        }
    }

    private List<Row> skipFirst(Csv.Reader reader) throws IOException {
        reader.readLine();
        return Row.readAll(reader);
    }

    @Test
    public void testReadFieldBeforeLine() throws IOException {
        VoidParser readFieldBeforeLine = Csv.Reader::readField;

        for (Sample sample : Sample.SAMPLES) {
            assertThatIllegalStateException()
                    .describedAs(sample.asDescription("Reading"))
                    .isThrownBy(() -> read(readFieldBeforeLine, sample.getContent(), sample.getFormat(), Csv.Parsing.DEFAULT));
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

        assertValid(invalidButStillOk, Csv.Parsing.DEFAULT);
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

        assertValid(overflow, Csv.Parsing.DEFAULT);
    }

    @Test
    public void testEmptyLine() throws IOException {
        Sample sample = Sample.EMPTY_LINES;
        try (Csv.Reader reader = Csv.Reader.of(sample.getFormat(), Csv.Parsing.DEFAULT, new StringReader(sample.getContent()))) {
            assertThat(reader.readLine()).isTrue();
            assertThat(reader.readField()).isFalse();
            assertThat(reader.readLine()).isTrue();
            assertThat(reader.readField()).isFalse();
            assertThat(reader.readLine()).isFalse();
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
                        .build(),
                Csv.Parsing.DEFAULT);
    }

    @Test
    public void testCharSequence() throws IOException {
        Sample sample = Sample.SIMPLE;
        try (Csv.Reader reader = Csv.Reader.of(sample.getFormat(), Csv.Parsing.DEFAULT, new StringReader(sample.getContent()))) {
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

    @Test
    public void testFieldOverflow() {
        Sample sample = Sample.SIMPLE;

        Csv.Parsing valid = Csv.Parsing.DEFAULT.toBuilder().maxCharsPerField(2).build();
        assertThatCode(() -> readValue(Row::readAll, sample.getContent(), sample.getFormat(), valid))
                .describedAs(sample.asDescription("Reading"))
                .doesNotThrowAnyException();

        Csv.Parsing invalid = Csv.Parsing.DEFAULT.toBuilder().maxCharsPerField(1).build();
        assertThatIOException().isThrownBy(() -> readValue(Row::readAll, sample.getContent(), sample.getFormat(), invalid))
                .describedAs(sample.asDescription("Reading"))
                .withMessageContaining("Field overflow");
    }

    private static void assertValid(Sample sample, Csv.Parsing options) throws IOException {
        assertThat(readValue(Row::readAll, sample.getContent(), sample.getFormat(), options))
                .describedAs(sample.asDescription("Reading"))
                .containsExactlyElementsOf(sample.getRows());
    }
}
