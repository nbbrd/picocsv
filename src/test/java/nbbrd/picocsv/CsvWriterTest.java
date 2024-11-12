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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;

import static _test.QuickWriter.write;
import static _test.QuickWriter.writeValue;
import static _test.Sample.INVALID_FORMAT;
import static nbbrd.picocsv.Csv.DEFAULT_CHAR_BUFFER_SIZE;
import static nbbrd.picocsv.Csv.Format.RFC4180;
import static nbbrd.picocsv.Csv.Format.UNIX_SEPARATOR;
import static org.assertj.core.api.Assertions.*;

public class CsvWriterTest {

    @Test
    public void testWriterFactory() {
        assertThatNullPointerException()
                .isThrownBy(() -> Csv.Writer.of(Csv.Format.DEFAULT, Csv.WriterOptions.DEFAULT, null, DEFAULT_CHAR_BUFFER_SIZE))
                .withMessageContaining("charWriter");

        assertThatNullPointerException()
                .isThrownBy(() -> Csv.Writer.of(null, Csv.WriterOptions.DEFAULT, new StringWriter(), DEFAULT_CHAR_BUFFER_SIZE))
                .withMessageContaining("format");

        assertThatNullPointerException()
                .isThrownBy(() -> Csv.Writer.of(Csv.Format.DEFAULT, null, new StringWriter(), DEFAULT_CHAR_BUFFER_SIZE))
                .withMessageContaining("options");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> Csv.Writer.of(INVALID_FORMAT, Csv.WriterOptions.DEFAULT, new StringWriter(), DEFAULT_CHAR_BUFFER_SIZE))
                .withMessageContaining("Invalid format: " + INVALID_FORMAT);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> Csv.Writer.of(Csv.Format.DEFAULT, Csv.WriterOptions.DEFAULT, new StringWriter(), 0))
                .withMessageContaining("Invalid charBufferSize: 0");
    }

    @ParameterizedTest
    @MethodSource("_test.Sample#getAllSamples")
    public void testAllSamples(Sample sample) throws IOException {
        assertValid(sample, Csv.WriterOptions.DEFAULT);
    }

    @Test
    public void testMissingEndLine() throws IOException {
        assertThat(
                write(o -> {
                    o.writeField("A1");
                    o.writeField("");
                    o.writeField("C1");
                }, RFC4180, Csv.WriterOptions.DEFAULT)
        ).isEqualTo("A1,,C1");
    }

    @Test
    public void testWriteComment() throws IOException {
        CharSequence chars = new StringBuilder().append("hello");

        assertThat(toWindows(w -> {
            w.writeComment(null);
        })).isEqualTo("#\r\n");
        assertThat(toUnix(w -> {
            w.writeComment(null);
        })).isEqualTo("#\n");

        assertThat(toWindows(w -> {
            w.writeComment("");
        })).isEqualTo("#\r\n");
        assertThat(toUnix(w -> {
            w.writeComment("");
        })).isEqualTo("#\n");

        assertThat(toWindows(w -> {
            w.writeComment("#");
        })).isEqualTo("##\r\n");
        assertThat(toUnix(w -> {
            w.writeComment("#");
        })).isEqualTo("##\n");

        assertThat(toWindows(w -> {
            w.writeComment("abc");
        })).isEqualTo("#abc\r\n");
        assertThat(toUnix(w -> {
            w.writeComment("abc");
        })).isEqualTo("#abc\n");

        assertThat(toWindows(w -> {
            w.writeComment("a\r\nbc");
        })).isEqualTo("#a\r\n#bc\r\n");
        assertThat(toUnix(w -> {
            w.writeComment("a\r\nbc");
        })).isEqualTo("#a\r\n#bc\n");

        assertThat(toWindows(w -> {
            w.writeComment("a\rbc");
        })).isEqualTo("#a\r\n#bc\r\n");
        assertThat(toUnix(w -> {
            w.writeComment("a\rbc");
        })).isEqualTo("#a\rbc\n");

        assertThat(toWindows(w -> {
            w.writeComment("a\nbc");
        })).isEqualTo("#a\r\n#bc\r\n");
        assertThat(toUnix(w -> {
            w.writeComment("a\nbc");
        })).isEqualTo("#a\n#bc\n");

        assertThat(toWindows(w -> {
            w.writeComment("a\r\n");
        })).isEqualTo("#a\r\n#\r\n");
        assertThat(toUnix(w -> {
            w.writeComment("a\r\n");
        })).isEqualTo("#a\r\n#\n");

        assertThat(toWindows(w -> {
            w.writeComment("a\r");
        })).isEqualTo("#a\r\n#\r\n");
        assertThat(toUnix(w -> {
            w.writeComment("a\r");
        })).isEqualTo("#a\r\n");

        assertThat(toWindows(w -> {
            w.writeComment("a\n");
        })).isEqualTo("#a\r\n#\r\n");
        assertThat(toUnix(w -> {
            w.writeComment("a\n");
        })).isEqualTo("#a\n#\n");

        assertThat(toWindows(w -> {
            w.writeField(chars);
            w.writeComment("abc");
        })).isEqualTo("hello\r\n#abc\r\n");
        assertThat(toUnix(w -> {
            w.writeField(chars);
            w.writeComment("abc");
        })).isEqualTo("hello\n#abc\n");

        assertThat(toWindows(w -> {
            w.writeField(chars);
            w.writeEndOfLine();
            w.writeComment("abc");
        })).isEqualTo("hello\r\n#abc\r\n");
        assertThat(toUnix(w -> {
            w.writeField(chars);
            w.writeEndOfLine();
            w.writeComment("abc");
        })).isEqualTo("hello\n#abc\n");

        assertThat(toWindows(w -> {
            w.writeComment("abc");
            w.writeField(chars);
        })).isEqualTo("#abc\r\nhello");
        assertThat(toUnix(w -> {
            w.writeComment("abc");
            w.writeField(chars);
        })).isEqualTo("#abc\nhello");

        assertThat(toWindows(w -> {
            w.writeField(null);
            w.writeComment("abc");
        })).isEqualTo("\"\"\r\n#abc\r\n");
        assertThat(toUnix(w -> {
            w.writeField(null);
            w.writeComment("abc");
        })).isEqualTo("\"\"\n#abc\n");
    }

    @Test
    public void testWriteField() throws IOException {
        CharSequence chars = new StringBuilder().append("hello");

        assertThat(toWindows(w -> {
        })).isEqualTo("");
        assertThat(toUnix(w -> {
        })).isEqualTo("");

        assertThat(toWindows(w -> {
            w.writeEndOfLine();
        })).isEqualTo("\r\n");
        assertThat(toUnix(w -> {
            w.writeEndOfLine();
        })).isEqualTo("\n");

        assertThat(toWindows(w -> {
            w.writeField(null);
        })).isEqualTo("\"\"");
        assertThat(toUnix(w -> {
            w.writeField(null);
        })).isEqualTo("\"\"");

        assertThat(toWindows(w -> {
            w.writeField("#");
        })).isEqualTo("\"#\"");
        assertThat(toUnix(w -> {
            w.writeField("#");
        })).isEqualTo("\"#\"");

        assertThat(toWindows(w -> {
            w.writeField("");
            w.writeField("#");
        })).isEqualTo(",#");
        assertThat(toUnix(w -> {
            w.writeField("");
            w.writeField("#");
        })).isEqualTo(",#");

        assertThat(toWindows(w -> {
            w.writeField(null);
            w.writeEndOfLine();
        })).isEqualTo("\"\"\r\n");
        assertThat(toUnix(w -> {
            w.writeField(null);
            w.writeEndOfLine();
        })).isEqualTo("\"\"\n");

        assertThat(toWindows(w -> {
            w.writeEndOfLine();
            w.writeField(null);
        })).isEqualTo("\r\n\"\"");
        assertThat(toUnix(w -> {
            w.writeEndOfLine();
            w.writeField(null);
        })).isEqualTo("\n\"\"");

        assertThat(toWindows(w -> {
            w.writeField(chars);
            w.writeField(null);
        })).isEqualTo("hello,");
        assertThat(toUnix(w -> {
            w.writeField(chars);
            w.writeField(null);
        })).isEqualTo("hello,");

        assertThat(toWindows(w -> {
            w.writeField(chars);
            w.writeField(null);
            w.writeEndOfLine();
        })).isEqualTo("hello,\r\n");
        assertThat(toUnix(w -> {
            w.writeField(chars);
            w.writeField(null);
            w.writeEndOfLine();
        })).isEqualTo("hello,\n");

        assertThat(toWindows(w -> {
            w.writeEndOfLine();
            w.writeField(chars);
            w.writeField(null);
        })).isEqualTo("\r\nhello,");
        assertThat(toUnix(w -> {
            w.writeEndOfLine();
            w.writeField(chars);
            w.writeField(null);
        })).isEqualTo("\nhello,");

        assertThat(toWindows(w -> {
            w.writeField(null);
            w.writeField(chars);
        })).isEqualTo(",hello");
        assertThat(toUnix(w -> {
            w.writeField(null);
            w.writeField(chars);
        })).isEqualTo(",hello");

        assertThat(toWindows(w -> {
            w.writeField(null);
            w.writeField(chars);
            w.writeEndOfLine();
        })).isEqualTo(",hello\r\n");
        assertThat(toUnix(w -> {
            w.writeField(null);
            w.writeField(chars);
            w.writeEndOfLine();
        })).isEqualTo(",hello\n");

        assertThat(toWindows(w -> {
            w.writeField(null);
            w.writeField(null);
            w.writeEndOfLine();
        })).isEqualTo(",\r\n");
        assertThat(toUnix(w -> {
            w.writeField(null);
            w.writeField(null);
            w.writeEndOfLine();
        })).isEqualTo(",\n");
    }

    @Test
    public void testOutputBuffer() throws IOException {
        assertValid(getOverflowSample(
                repeat('A', DEFAULT_CHAR_BUFFER_SIZE - 1),
                "\"",
                repeat('C', 10)
        ), Csv.WriterOptions.DEFAULT);

        assertValid(getOverflowSample(
                repeat('A', DEFAULT_CHAR_BUFFER_SIZE),
                "\"",
                repeat('C', 10)
        ), Csv.WriterOptions.DEFAULT);

        assertValid(getOverflowSample(
                repeat('A', DEFAULT_CHAR_BUFFER_SIZE + 1),
                "\"",
                repeat('C', 10)
        ), Csv.WriterOptions.DEFAULT);
    }

    @Test
    void flushesALineToTheUnderlyingWriter() throws IOException {
        StringWriter buf = new StringWriter();

        Csv.Writer csvWriter = Csv.Writer.of(Csv.Format.RFC4180, Csv.WriterOptions.DEFAULT, buf);
        csvWriter.writeField("foo");
        csvWriter.writeField("bar");
        csvWriter.writeField("baz");
        csvWriter.writeEndOfLine();
        csvWriter.flush();

        assertThat(buf.toString()).isEqualTo("foo,bar,baz\r\n");
    }

    private static Sample getOverflowSample(String... fields) {
        return Sample
                .builder()
                .name("overflow")
                .format(Csv.Format.RFC4180)
                .content(String.join(",", fields).replace("\"", "\"\"\"\"") + "\r\n")
                .rowFields(fields)
                .build();
    }

    private static String toWindows(QuickWriter.VoidFormatter formatter) throws IOException {
        return write(formatter, Csv.Format.RFC4180, Csv.WriterOptions.DEFAULT);
    }

    private static String toUnix(QuickWriter.VoidFormatter formatter) throws IOException {
        return write(formatter, Csv.Format.RFC4180.toBuilder().separator(UNIX_SEPARATOR).build(), Csv.WriterOptions.DEFAULT);
    }

    private static String repeat(char c, int length) {
        char[] result = new char[length];
        Arrays.fill(result, c);
        return String.valueOf(result);
    }

    private static void assertValid(Sample sample, Csv.WriterOptions options) throws IOException {
        assertThat(writeValue(sample.getRows(), Row::writeAll, sample.getFormat(), options))
                .describedAs(sample.asDescription("Writing"))
                .isEqualTo(sample.getContent() + getMissingEOL(sample));
    }

    private static String getMissingEOL(Sample sample) {
        return sample.isWithoutEOL() ? sample.getFormat().getSeparator() : "";
    }
}
