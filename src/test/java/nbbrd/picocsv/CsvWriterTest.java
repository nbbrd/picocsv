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
import java.util.Arrays;

import static _test.QuickWriter.write;
import static _test.QuickWriter.writeValue;
import static _test.Sample.INVALID_FORMAT;
import static nbbrd.picocsv.Csv.DEFAULT_CHAR_BUFFER_SIZE;
import static nbbrd.picocsv.Csv.Format.RFC4180;
import static org.assertj.core.api.Assertions.*;

public class CsvWriterTest {

    @Test
    public void testWriterFactory() {
        assertThatNullPointerException()
                .isThrownBy(() -> Csv.Writer.of(Csv.Format.DEFAULT, Csv.Formatting.DEFAULT, null, DEFAULT_CHAR_BUFFER_SIZE))
                .withMessageContaining("charWriter");

        assertThatNullPointerException()
                .isThrownBy(() -> Csv.Writer.of(null, Csv.Formatting.DEFAULT, new StringWriter(), DEFAULT_CHAR_BUFFER_SIZE))
                .withMessageContaining("format");

        assertThatNullPointerException()
                .isThrownBy(() -> Csv.Writer.of(Csv.Format.DEFAULT, null, new StringWriter(), DEFAULT_CHAR_BUFFER_SIZE))
                .withMessageContaining("options");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> Csv.Writer.of(INVALID_FORMAT, Csv.Formatting.DEFAULT, new StringWriter(), DEFAULT_CHAR_BUFFER_SIZE))
                .withMessageContaining("Invalid format: " + INVALID_FORMAT);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> Csv.Writer.of(Csv.Format.DEFAULT, Csv.Formatting.DEFAULT, new StringWriter(), 0))
                .withMessageContaining("Invalid charBufferSize: 0");
    }

    @Test
    public void testAllSamples() throws IOException {
        for (Sample sample : Sample.SAMPLES) {
            assertValid(sample, Csv.Formatting.DEFAULT);
        }
    }

    @Test
    public void testMissingEndLine() throws IOException {
        assertThat(
                write(o -> {
                    o.writeField("A1");
                    o.writeField("");
                    o.writeField("C1");
                }, RFC4180, Csv.Formatting.DEFAULT)
        ).isEqualTo("A1,,C1");
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
        assertValid(getOverflowSample(
                repeat('A', DEFAULT_CHAR_BUFFER_SIZE - 1),
                "\"",
                repeat('C', 10)
        ), Csv.Formatting.DEFAULT);

        assertValid(getOverflowSample(
                repeat('A', DEFAULT_CHAR_BUFFER_SIZE),
                "\"",
                repeat('C', 10)
        ), Csv.Formatting.DEFAULT);

        assertValid(getOverflowSample(
                repeat('A', DEFAULT_CHAR_BUFFER_SIZE + 1),
                "\"",
                repeat('C', 10)
        ), Csv.Formatting.DEFAULT);
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
        return write(formatter, Csv.Format.RFC4180, Csv.Formatting.DEFAULT);
    }

    private static String repeat(char c, int length) {
        char[] result = new char[length];
        Arrays.fill(result, c);
        return String.valueOf(result);
    }

    private static void assertValid(Sample sample, Csv.Formatting options) throws IOException {
        assertThat(writeValue(sample.getRows(), Row::writeAll, sample.getFormat(), options))
                .describedAs(sample.asDescription("Writing"))
                .isEqualTo(sample.getContent() + getMissingEOL(sample));
    }

    private static String getMissingEOL(Sample sample) {
        return sample.isWithoutEOL() ? sample.getFormat().getSeparator() : "";
    }
}
