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
import java.util.List;

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
        assertThat(writeValue(sample.getRows(), Row::writeAll, sample.getFormat(), Csv.WriterOptions.DEFAULT))
                .describedAs(sample.asDescription("Writing"))
                .isEqualTo(sample.getContent() + getMissingEOL(sample));
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
    public void testWriteComment() {
        assertThat(writing(COMMENTED_NULL))
                .returns("#␍␊", Scenario::toWindows)
                .returns("#␊", Scenario::toUnix);

        assertThat(writing(COMMENTED_EMPTY))
                .returns("#␍␊", Scenario::toWindows)
                .returns("#␊", Scenario::toUnix);

        assertThat(writing(COMMENTED_COMMENT))
                .returns("##␍␊", Scenario::toWindows)
                .returns("##␊", Scenario::toUnix);

        assertThat(writing(COMMENTED_CHARS))
                .returns("#abc␍␊", Scenario::toWindows)
                .returns("#abc␊", Scenario::toUnix);

        assertThat(writing(COMMENTED_QUOTE))
                .returns("#'␍␊", Scenario::toWindows)
                .returns("#'␊", Scenario::toUnix);

        assertThat(writing(w -> w.writeComment("a␍␊bc")))
                .returns("#a␍␊#bc␍␊", Scenario::toWindows)
                .returns("#a␍␊#bc␊", Scenario::toUnix);

        assertThat(writing(w -> w.writeComment("a␍bc")))
                .returns("#a␍␊#bc␍␊", Scenario::toWindows)
                .returns("#a␍bc␊", Scenario::toUnix);

        assertThat(writing(w -> w.writeComment("a␊bc")))
                .returns("#a␍␊#bc␍␊", Scenario::toWindows)
                .returns("#a␊#bc␊", Scenario::toUnix);

        assertThat(writing(w -> w.writeComment("a␍␊")))
                .returns("#a␍␊#␍␊", Scenario::toWindows)
                .returns("#a␍␊#␊", Scenario::toUnix);

        assertThat(writing(w -> w.writeComment("a␍")))
                .returns("#a␍␊#␍␊", Scenario::toWindows)
                .returns("#a␍␊", Scenario::toUnix);

        assertThat(writing(w -> w.writeComment("a␊")))
                .returns("#a␍␊#␍␊", Scenario::toWindows)
                .returns("#a␊#␊", Scenario::toUnix);

        assertThat(writing(CHARS, COMMENTED_CHARS))
                .returns("hello␍␊#abc␍␊", Scenario::toWindows)
                .returns("hello␊#abc␊", Scenario::toUnix);

        assertThat(writing(CHARS, EOL, COMMENTED_CHARS))
                .returns("hello␍␊#abc␍␊", Scenario::toWindows)
                .returns("hello␊#abc␊", Scenario::toUnix);

        assertThat(writing(COMMENTED_CHARS, CHARS))
                .returns("#abc␍␊hello", Scenario::toWindows)
                .returns("#abc␊hello", Scenario::toUnix);

        assertThat(writing(NULL, COMMENTED_CHARS))
                .returns("''␍␊#abc␍␊", Scenario::toWindows)
                .returns("''␊#abc␊", Scenario::toUnix);
    }

    @Test
    public void testWriteField() {
        assertThat(writing())
                .returns("", Scenario::toWindows)
                .returns("", Scenario::toUnix);

        assertThat(writing(EOL))
                .returns("␍␊", Scenario::toWindows)
                .returns("␊", Scenario::toUnix);

        assertThat(writing(NULL))
                .returns("''", Scenario::toWindows)
                .returns("''", Scenario::toUnix);

        assertThat(writing(COMMENT))
                .returns("'#'", Scenario::toWindows)
                .returns("'#'", Scenario::toUnix);

        assertThat(writing(QUOTE))
                .returns("''''", Scenario::toWindows)
                .returns("''''", Scenario::toUnix);

        assertThat(writing(NULL, COMMENT))
                .returns(",#", Scenario::toWindows)
                .returns(",#", Scenario::toUnix);

        assertThat(writing(NULL, EOL))
                .returns("''␍␊", Scenario::toWindows)
                .returns("''␊", Scenario::toUnix);

        assertThat(writing(EOL, NULL))
                .returns("␍␊''", Scenario::toWindows)
                .returns("␊''", Scenario::toUnix);

        assertThat(writing(CHARS, NULL))
                .returns("hello,", Scenario::toWindows)
                .returns("hello,", Scenario::toUnix);

        assertThat(writing(CHARS, NULL, EOL))
                .returns("hello,␍␊", Scenario::toWindows)
                .returns("hello,␊", Scenario::toUnix);

        assertThat(writing(EOL, CHARS, NULL))
                .returns("␍␊hello,", Scenario::toWindows)
                .returns("␊hello,", Scenario::toUnix);

        assertThat(writing(NULL, CHARS))
                .returns(",hello", Scenario::toWindows)
                .returns(",hello", Scenario::toUnix);

        assertThat(writing(NULL, CHARS, EOL))
                .returns(",hello␍␊", Scenario::toWindows)
                .returns(",hello␊", Scenario::toUnix);

        assertThat(writing(NULL, NULL, EOL))
                .returns(",␍␊", Scenario::toWindows)
                .returns(",␊", Scenario::toUnix);
    }

    @Test
    public void testWriteQuotedField() {
        assertThat(writing(QUOTED_NULL))
                .returns("''", Scenario::toWindows)
                .returns("''", Scenario::toUnix);

        assertThat(writing(QUOTED_COMMENT))
                .returns("'#'", Scenario::toWindows)
                .returns("'#'", Scenario::toUnix);

        assertThat(writing(QUOTED_QUOTE))
                .returns("''''", Scenario::toWindows)
                .returns("''''", Scenario::toUnix);

        assertThat(writing(QUOTED_NULL, QUOTED_COMMENT))
                .returns("'','#'", Scenario::toWindows)
                .returns("'','#'", Scenario::toUnix);

        assertThat(writing(QUOTED_NULL, EOL))
                .returns("''␍␊", Scenario::toWindows)
                .returns("''␊", Scenario::toUnix);

        assertThat(writing(EOL, QUOTED_NULL))
                .returns("␍␊''", Scenario::toWindows)
                .returns("␊''", Scenario::toUnix);

        assertThat(writing(QUOTED_CHARS, QUOTED_NULL))
                .returns("'hello',''", Scenario::toWindows)
                .returns("'hello',''", Scenario::toUnix);

        assertThat(writing(QUOTED_CHARS, QUOTED_NULL, EOL))
                .returns("'hello',''␍␊", Scenario::toWindows)
                .returns("'hello',''␊", Scenario::toUnix);

        assertThat(writing(EOL, QUOTED_CHARS, QUOTED_NULL))
                .returns("␍␊'hello',''", Scenario::toWindows)
                .returns("␊'hello',''", Scenario::toUnix);

        assertThat(writing(QUOTED_NULL, QUOTED_CHARS))
                .returns("'','hello'", Scenario::toWindows)
                .returns("'','hello'", Scenario::toUnix);

        assertThat(writing(QUOTED_NULL, QUOTED_CHARS, EOL))
                .returns("'','hello'␍␊", Scenario::toWindows)
                .returns("'','hello'␊", Scenario::toUnix);

        assertThat(writing(QUOTED_NULL, QUOTED_NULL, EOL))
                .returns("'',''␍␊", Scenario::toWindows)
                .returns("'',''␊", Scenario::toUnix);

        assertThat(writing(NULL, QUOTED_NULL, EOL))
                .returns(",''␍␊", Scenario::toWindows)
                .returns(",''␊", Scenario::toUnix);

        assertThat(writing(NULL, QUOTED_CHARS, EOL))
                .returns(",'hello'␍␊", Scenario::toWindows)
                .returns(",'hello'␊", Scenario::toUnix);
    }

    @Test
    public void testFieldOverflow() {
        Sample sample = Sample.SIMPLE;

        Csv.WriterOptions valid = Csv.WriterOptions.DEFAULT.toBuilder().maxCharsPerField(2).build();
        assertThatCode(() -> writeValue(sample.getRows(), Row::writeAll, sample.getFormat(), valid))
                .describedAs(sample.asDescription("Writing"))
                .doesNotThrowAnyException();

        Csv.WriterOptions invalid = Csv.WriterOptions.DEFAULT.toBuilder().maxCharsPerField(1).build();
        assertThatIOException().isThrownBy(() -> writeValue(sample.getRows(), Row::writeAll, sample.getFormat(), invalid))
                .describedAs(sample.asDescription("Writing"))
                .withMessageContaining("Field overflow");
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

    @lombok.Value
    @lombok.Builder
    private static class Scenario {

        public static final Csv.Format WINDOWS = Csv.Format.RFC4180.toBuilder().quote('\'').separator("␍␊").build();
        public static final Csv.Format UNIX = Csv.Format.RFC4180.toBuilder().quote('\'').separator("␊").build();

        @lombok.Singular
        List<QuickWriter.VoidFormatter> formatters;

        void all(Csv.Writer w) throws IOException {
            for (QuickWriter.VoidFormatter o : formatters) {
                o.accept(w);
            }
        }

        @lombok.SneakyThrows
        String toWindows() {
            return write(this::all, WINDOWS, Csv.WriterOptions.DEFAULT);
        }

        @lombok.SneakyThrows
        String toUnix() {
            return write(this::all, UNIX, Csv.WriterOptions.DEFAULT);
        }
    }

    private static Scenario writing(QuickWriter.VoidFormatter... formatters) {
        return Scenario.builder().formatters(Arrays.asList(formatters)).build();
    }

    private static final CharSequence HELLO = new StringBuilder().append("hello");
    private static final CharSequence ABC = new StringBuilder().append("abc");

    private static final QuickWriter.VoidFormatter CHARS = writer -> writer.writeField(HELLO);
    private static final QuickWriter.VoidFormatter NULL = writer -> writer.writeField(null);
    private static final QuickWriter.VoidFormatter COMMENT = writer -> writer.writeField("#");
    private static final QuickWriter.VoidFormatter QUOTE = writer -> writer.writeField("'");

    private static final QuickWriter.VoidFormatter QUOTED_CHARS = writer -> writer.writeQuotedField(HELLO);
    private static final QuickWriter.VoidFormatter QUOTED_NULL = writer -> writer.writeQuotedField(null);
    private static final QuickWriter.VoidFormatter QUOTED_COMMENT = writer -> writer.writeQuotedField("#");
    private static final QuickWriter.VoidFormatter QUOTED_QUOTE = writer -> writer.writeQuotedField("'");

    private static final QuickWriter.VoidFormatter COMMENTED_CHARS = writer -> writer.writeComment(ABC);
    private static final QuickWriter.VoidFormatter COMMENTED_NULL = writer -> writer.writeComment(null);
    private static final QuickWriter.VoidFormatter COMMENTED_EMPTY = writer -> writer.writeComment("");
    private static final QuickWriter.VoidFormatter COMMENTED_COMMENT = writer -> writer.writeComment("#");
    private static final QuickWriter.VoidFormatter COMMENTED_QUOTE = writer -> writer.writeComment("'");

    private static final QuickWriter.VoidFormatter EOL = Csv.Writer::writeEndOfLine;

    private static String getMissingEOL(Sample sample) {
        return sample.isWithoutEOL() ? sample.getFormat().getSeparator() : "";
    }
}
