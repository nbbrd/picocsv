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
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Arrays;

import static _test.Sample.ILLEGAL_FORMAT;
import static java.nio.charset.StandardCharsets.UTF_8;
import static nbbrd.picocsv.Csv.DEFAULT_CHAR_BUFFER_SIZE;
import static nbbrd.picocsv.Csv.Format.RFC4180;
import static org.assertj.core.api.Assertions.*;

public class CsvWriterTest {

    @Test
    public void testWriterFactory() {
        assertThatNullPointerException()
                .isThrownBy(() -> Csv.Writer.of((Writer) null, DEFAULT_CHAR_BUFFER_SIZE, Csv.Formatting.DEFAULT))
                .withMessageContaining("charWriter");

        assertThatNullPointerException()
                .isThrownBy(() -> Csv.Writer.of(new StringWriter(), DEFAULT_CHAR_BUFFER_SIZE, null))
                .withMessageContaining("options");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> Csv.Writer.of(new StringWriter(), DEFAULT_CHAR_BUFFER_SIZE, Csv.Formatting.DEFAULT.toBuilder().format(ILLEGAL_FORMAT).build()))
                .withMessageContaining("format");
    }

    @Test
    public void testAllSamples() throws IOException {
        for (QuickWriter writer : QuickWriter.values()) {
            for (Charset encoding : Sample.CHARSETS) {
                for (Sample sample : Sample.SAMPLES) {
                    assertValid(writer, encoding, sample);
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
        assertValid(QuickWriter.CHAR_WRITER, UTF_8, getOverflowSample(
                repeat('A', DEFAULT_CHAR_BUFFER_SIZE - 1),
                "\"",
                repeat('C', 10)
        ));

        assertValid(QuickWriter.CHAR_WRITER, UTF_8, getOverflowSample(
                repeat('A', DEFAULT_CHAR_BUFFER_SIZE),
                "\"",
                repeat('C', 10)
        ));

        assertValid(QuickWriter.CHAR_WRITER, UTF_8, getOverflowSample(
                repeat('A', DEFAULT_CHAR_BUFFER_SIZE + 1),
                "\"",
                repeat('C', 10)
        ));
    }

    private static Sample getOverflowSample(String... fields) {
        return Sample
                .builder()
                .name("overflow")
                .format(Csv.Format.RFC4180)
                .content(String.join(",", fields).replace("\"", "\"\"\"\"") + "\r\n")
                .rowOf(fields)
                .build();
    }

    private static String writeToString(QuickWriter.VoidFormatter formatter) throws IOException {
        return QuickWriter.CHAR_WRITER.write(formatter, null, Csv.Format.RFC4180);
    }

    private static String repeat(char c, int length) {
        char[] result = new char[length];
        Arrays.fill(result, c);
        return String.valueOf(result);
    }

    private static void assertValid(QuickWriter writer, Charset encoding, Sample sample) throws IOException {
        assertThat(writer.writeValue(sample.getRows(), Row::write, encoding, sample.getFormat()))
                .describedAs("Writing '%s' with '%s'", sample.getName(), writer)
                .isEqualTo(sample.getContent() + getMissingEOL(sample));
    }

    private static String getMissingEOL(Sample sample) {
        return sample.isWithoutEOL() ? getEOLString(sample.getFormat().getSeparator()) : "";
    }

    private static String getEOLString(Csv.NewLine newLine) {
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
}
