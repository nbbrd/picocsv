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
import _test.QuickReader.Parser;
import _test.QuickReader.VoidParser;
import _test.Row;
import _test.Sample;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import static nbbrd.picocsv.CsvFormat.RFC4180;
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
                .isThrownBy(() -> CsvReader.of((Path) null, UTF_8, RFC4180))
                .withMessageContaining("file");

        assertThatNullPointerException()
                .isThrownBy(() -> CsvReader.of(QuickReader.newInputFile("", UTF_8), null, RFC4180))
                .withMessageContaining("encoding");

        assertThatNullPointerException()
                .isThrownBy(() -> CsvReader.of(QuickReader.newInputFile("", UTF_8), UTF_8, null))
                .withMessageContaining("format");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> CsvReader.of(QuickReader.newInputFile("", UTF_8), UTF_8, illegalFormat))
                .withMessageContaining("format");
    }

    @Test
    public void testStreamFactory() {
        assertThatNullPointerException()
                .isThrownBy(() -> CsvReader.of((InputStream) null, UTF_8, RFC4180))
                .withMessageContaining("stream");

        assertThatNullPointerException()
                .isThrownBy(() -> CsvReader.of(QuickReader.newInputStream("", UTF_8), null, RFC4180))
                .withMessageContaining("encoding");

        assertThatNullPointerException()
                .isThrownBy(() -> CsvReader.of(QuickReader.newInputStream("", UTF_8), UTF_8, null))
                .withMessageContaining("format");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> CsvReader.of(QuickReader.newInputStream("", UTF_8), UTF_8, illegalFormat))
                .withMessageContaining("format");
    }

    @Test
    public void testReaderFactory() {
        assertThatNullPointerException()
                .isThrownBy(() -> CsvReader.of((Reader) null, RFC4180))
                .withMessageContaining("reader");

        assertThatNullPointerException()
                .isThrownBy(() -> CsvReader.of(QuickReader.newReader(""), null))
                .withMessageContaining("format");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> CsvReader.of(QuickReader.newReader(""), illegalFormat))
                .withMessageContaining("format");
    }

    @Test
    public void testNormal() throws IOException {
        for (QuickReader writer : QuickReader.values()) {
            for (Charset encoding : Sample.CHARSETS) {
                for (Sample sample : Sample.SAMPLES) {
                    assertThatCode(() -> writer.readValue(Row::read, encoding, sample.getFormat(), sample.getContent()))
                            .describedAs("Reading '%s' with '%s'", sample.getName(), writer)
                            .doesNotThrowAnyException();

                    assertThat(writer.readValue(Row::read, encoding, sample.getFormat(), sample.getContent()))
                            .describedAs("Reading '%s' with '%s'", sample.getName(), writer)
                            .containsExactlyElementsOf(sample.getRows());
                }
            }
        }
    }

    @Test
    public void testSkip() throws IOException {
        Parser<List<Row>> skipFirst = stream -> {
            stream.readLine();
            return Row.read(stream);
        };

        forEach((type, encoding, sample) -> {
            switch (sample.getRows().size()) {
                case 0:
                case 1:
                    assertThat(type.readValue(skipFirst, encoding, sample.getFormat(), sample.getContent()))
                            .isEmpty();
                    break;
                default:
                    assertThat(type.readValue(skipFirst, encoding, sample.getFormat(), sample.getContent()))
                            .element(0)
                            .isEqualTo(sample.getRows().get(1));
                    break;
            }
        });
    }

    @Test
    public void testReadFieldBeforeLine() throws IOException {
        VoidParser readFieldBeforeLine = CsvReader::readField;

        forEach((type, encoding, sample) -> {
            assertThatIllegalStateException()
                    .isThrownBy(() -> type.read(readFieldBeforeLine, encoding, sample.getFormat(), sample.getContent()));
        });
    }

    @Test
    public void testNonQuotedNonNewLineChar() throws IOException {
        Sample invalidButStillOk = Sample
                .builder()
                .name("Invalid but still ok")
                .format(CsvFormat.RFC4180)
                .content("\r\r\n")
                .row(Row.of("\r"))
                .build();

        for (QuickReader type : QuickReader.values()) {
            assertThat(type.readValue(Row::read, UTF_8, invalidButStillOk.getFormat(), invalidButStillOk.getContent()))
                    .containsExactlyElementsOf(invalidButStillOk.getRows());
        }
    }

    @Test
    public void testReusableFieldOverflow() throws IOException {
        String field1 = IntStream.range(0, 70).mapToObj(o -> "A").collect(Collectors.joining());
        String field2 = IntStream.range(0, 10).mapToObj(o -> "B").collect(Collectors.joining());
        Sample overflow = Sample
                .builder()
                .name("overflow")
                .format(CsvFormat.RFC4180)
                .content(field1 + "," + field2)
                .row(Row.of(field1, field2))
                .build();

        for (QuickReader type : QuickReader.values()) {
            assertThat(type.readValue(Row::read, UTF_8, overflow.getFormat(), overflow.getContent()))
                    .containsExactlyElementsOf(overflow.getRows());
        }
    }

    @lombok.Value
    private static final class Tuple {

        private QuickReader type;
        private Charset encoding;
        private Sample sample;

        static List<Tuple> getAll() {
            List<Tuple> result = new ArrayList<>();
            for (QuickReader type : QuickReader.values()) {
                for (Charset encoding : Sample.CHARSETS) {
                    for (Sample sample : Sample.SAMPLES) {
                        result.add(new Tuple(type, encoding, sample));
                    }
                }
            }
            return result;
        }

    }

    private static void forEach(TupleConsumer consumer) throws IOException {
        for (Tuple o : Tuple.getAll()) {
            consumer.apply(o.type, o.encoding, o.sample);
        }
    }

    private interface TupleConsumer {

        void apply(QuickReader type, Charset encoding, Sample sample) throws IOException;
    }

    private final CsvFormat illegalFormat = CsvFormat.DEFAULT.toBuilder().delimiter(':').quote(':').build();
}
