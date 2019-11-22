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

import _test.QuickWriter;
import _test.Row;
import _test.Sample;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.nio.file.Path;
import java.util.function.Function;
import static nbbrd.picocsv.Csv.BufferSizes.DEFAULT_CHAR_BUFFER_SIZE;
import org.junit.Test;
import static nbbrd.picocsv.Csv.Format.RFC4180;
import static org.assertj.core.api.Assertions.*;

/**
 *
 * @author Philippe Charles
 */
public class CsvWriterTest {

    @Test
    public void testPathFactory() {
        assertThatNullPointerException()
                .isThrownBy(() -> Csv.Writer.of((Path) null, UTF_8, RFC4180))
                .withMessageContaining("file");

        assertThatNullPointerException()
                .isThrownBy(() -> Csv.Writer.of(QuickWriter.newOutputFile(), null, RFC4180))
                .withMessageContaining("encoding");

        assertThatNullPointerException()
                .isThrownBy(() -> Csv.Writer.of(QuickWriter.newOutputFile(), UTF_8, null))
                .withMessageContaining("format");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> Csv.Writer.of(QuickWriter.newOutputFile(), UTF_8, illegalFormat))
                .withMessageContaining("format");
    }

    @Test
    public void testStreamFactory() {
        assertThatNullPointerException()
                .isThrownBy(() -> Csv.Writer.of((OutputStream) null, UTF_8, RFC4180))
                .withMessageContaining("stream");

        assertThatNullPointerException()
                .isThrownBy(() -> Csv.Writer.of(QuickWriter.newOutputStream(), null, RFC4180))
                .withMessageContaining("encoding");

        assertThatNullPointerException()
                .isThrownBy(() -> Csv.Writer.of(QuickWriter.newOutputStream(), UTF_8, null))
                .withMessageContaining("format");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> Csv.Writer.of(QuickWriter.newOutputStream(), UTF_8, illegalFormat))
                .withMessageContaining("format");
    }

    @Test
    public void testWriterFactory() {
        assertThatNullPointerException()
                .isThrownBy(() -> Csv.Writer.of((Writer) null, RFC4180))
                .withMessageContaining("charWriter");

        assertThatNullPointerException()
                .isThrownBy(() -> Csv.Writer.of(QuickWriter.newCharWriter(), null))
                .withMessageContaining("format");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> Csv.Writer.of(QuickWriter.newCharWriter(), illegalFormat))
                .withMessageContaining("format");
    }

    @Test
    public void testNormal() throws IOException {
        for (QuickWriter writer : QuickWriter.values()) {
            for (Charset encoding : Sample.CHARSETS) {
                for (Sample sample : Sample.SAMPLES) {
                    assertThatCode(() -> writer.writeValue(sample.getRows(), Row::write, encoding, sample.getFormat()))
                            .describedAs("Writing '%s' with '%s'", sample.getName(), writer)
                            .doesNotThrowAnyException();

                    assertThat(writer.writeValue(sample.getRows(), Row::write, encoding, sample.getFormat()))
                            .describedAs("Writing '%s' with '%s'", sample.getName(), writer)
                            .startsWith(sample.getContent())
                            .hasSize(expectedSize(sample));
                }
            }
        }
    }

    @Test
    public void testMissingEndLine() throws IOException {
        for (QuickWriter writer : QuickWriter.values()) {
            assertThat(
                    writer.write(o -> {
                        o.writeField("A1");
                        o.writeField("");
                        o.writeField("C1");
                    }, UTF_8, RFC4180)
            ).isEqualTo("A1,,C1");
        }
    }

    @Test
    public void testWriteField() throws IOException {
        CharSequence chars = new StringBuilder().append("hello");

        assertThat(writeToString(w -> {
        })).isEqualTo("");

        assertThat(writeToString(w -> {
            w.writeEndOfLine();
        })).isEqualTo("\r\n");

        assertThat(writeToString(w -> {
            w.writeField(null);
        })).isEqualTo("\"\"");

        assertThat(writeToString(w -> {
            w.writeField(null);
            w.writeEndOfLine();
        })).isEqualTo("\"\"\r\n");

        assertThat(writeToString(w -> {
            w.writeEndOfLine();
            w.writeField(null);
        })).isEqualTo("\r\n\"\"");

        assertThat(writeToString(w -> {
            w.writeField(chars);
            w.writeField(null);
        })).isEqualTo("hello,");

        assertThat(writeToString(w -> {
            w.writeField(chars);
            w.writeField(null);
            w.writeEndOfLine();
        })).isEqualTo("hello,\r\n");

        assertThat(writeToString(w -> {
            w.writeEndOfLine();
            w.writeField(chars);
            w.writeField(null);
        })).isEqualTo("\r\nhello,");

        assertThat(writeToString(w -> {
            w.writeField(null);
            w.writeField(chars);
        })).isEqualTo(",hello");

        assertThat(writeToString(w -> {
            w.writeField(null);
            w.writeField(chars);
            w.writeEndOfLine();
        })).isEqualTo(",hello\r\n");
    }

    @Test
    public void testOutputBuffer() throws IOException {
        Function<String[], Sample> toSample
                = fields -> Sample
                        .builder()
                        .name("overflow")
                        .format(Csv.Format.RFC4180)
                        .content(String.join(",", fields).replace("\"", "\"\"\"\"") + "\r\n")
                        .row(Row.of(fields))
                        .build();

        assertWrite(QuickWriter.BYTE_ARRAY, toSample.apply(new String[]{
            Sample.getField(DEFAULT_CHAR_BUFFER_SIZE - 1, 'A'),
            "\"",
            Sample.getField(10, 'C')}
        ));

        assertWrite(QuickWriter.BYTE_ARRAY, toSample.apply(new String[]{
            Sample.getField(DEFAULT_CHAR_BUFFER_SIZE, 'A'),
            "\"",
            Sample.getField(10, 'C')}
        ));

        assertWrite(QuickWriter.BYTE_ARRAY, toSample.apply(new String[]{
            Sample.getField(DEFAULT_CHAR_BUFFER_SIZE + 1, 'A'),
            "\"",
            Sample.getField(10, 'C')}
        ));
    }

    private static void assertWrite(QuickWriter writer, Sample sample) throws IOException {
        assertThat(writer.writeValue(sample.getRows(), Row::write, UTF_8, sample.getFormat()))
                .isEqualTo(sample.getContent());
    }

    private static int expectedSize(Sample sample) {
        int length = sample.getContent().length();
        if (length == 0) {
            return 0;
        }
        String eol = getEolString(sample.getFormat().getSeparator());
        return sample.getContent().endsWith(eol) ? length : length + eol.length();
    }

    private static String getEolString(Csv.NewLine newLine) {
        switch (newLine) {
            case MACINTOSH:
                return "\r";
            case UNIX:
                return "\n";
            case WINDOWS:
                return "\r\n";
            default:
                throw new RuntimeException();
        }
    }

    private final Csv.Format illegalFormat = Csv.Format.DEFAULT.toBuilder().delimiter(':').quote(':').build();

    private static String writeToString(QuickWriter.VoidFormatter formatter) throws IOException {
        return QuickWriter.CHAR_WRITER.write(formatter, null, Csv.Format.RFC4180);
    }
}
