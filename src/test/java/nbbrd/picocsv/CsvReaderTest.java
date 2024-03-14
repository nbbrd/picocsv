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

import _demo.Cookbook;
import _test.QuickReader;
import _test.QuickReader.VoidParser;
import _test.Row;
import _test.Sample;
import _test.fastcsv.FastCsvEntry;
import _test.fastcsv.FastCsvEntryConverter;
import _test.fastcsv.FastCsvEntryRowsParser;
import org.assertj.core.api.Condition;
import org.assertj.core.condition.VerboseCondition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

import static _test.Sample.INVALID_FORMAT;
import static java.util.stream.Collectors.joining;
import static nbbrd.picocsv.Csv.DEFAULT_CHAR_BUFFER_SIZE;
import static nbbrd.picocsv.Csv.Format.*;
import static org.assertj.core.api.Assertions.*;

/**
 * @author Philippe Charles
 */
public class CsvReaderTest {

    @Test
    public void testReaderFactory() {
        assertThatNullPointerException()
                .isThrownBy(() -> Csv.Reader.of(Csv.Format.DEFAULT, Csv.ReaderOptions.DEFAULT, null, DEFAULT_CHAR_BUFFER_SIZE))
                .withMessageContaining("charReader");

        assertThatNullPointerException()
                .isThrownBy(() -> Csv.Reader.of(null, Csv.ReaderOptions.DEFAULT, new StringReader(""), DEFAULT_CHAR_BUFFER_SIZE))
                .withMessageContaining("format");

        assertThatNullPointerException()
                .isThrownBy(() -> Csv.Reader.of(Csv.Format.DEFAULT, null, new StringReader(""), DEFAULT_CHAR_BUFFER_SIZE))
                .withMessageContaining("options");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> Csv.Reader.of(INVALID_FORMAT, Csv.ReaderOptions.DEFAULT, new StringReader(""), DEFAULT_CHAR_BUFFER_SIZE))
                .withMessageContaining("Invalid format: " + INVALID_FORMAT);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> Csv.Reader.of(Csv.Format.DEFAULT, Csv.ReaderOptions.DEFAULT, new StringReader(""), 0))
                .withMessageContaining("Invalid charBufferSize: 0");

        Csv.ReaderOptions invalidOptions = Csv.ReaderOptions.DEFAULT.toBuilder().maxCharsPerField(0).build();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> Csv.Reader.of(Csv.Format.DEFAULT, invalidOptions, new StringReader(""), DEFAULT_CHAR_BUFFER_SIZE))
                .withMessageContaining("Invalid options: " + invalidOptions);
    }

    @ParameterizedTest
    @MethodSource("_test.Sample#getAllSamples")
    public void testAllSamples(Sample sample) {
        assertThat(sample)
                .is(validWithStrict)
                .is(validWithLenient);
    }

    @ParameterizedTest
    @MethodSource("_test.Sample#getAllSamples")
    public void testSkip(Sample sample) throws IOException {
        switch (sample.getRows().size()) {
            case 0:
            case 1:
                assertThat(readRows(sample, Csv.ReaderOptions.DEFAULT, RowParser.SKIP_FIRST))
                        .describedAs(sample.asDescription("Reading"))
                        .isEmpty();
                assertThat(readRows(sample, Csv.ReaderOptions.DEFAULT, RowParser.SKIP_NOT_LAST))
                        .describedAs(sample.asDescription("Reading"))
                        .isEmpty();
                break;
            default:
                assertThat(readRows(sample, Csv.ReaderOptions.DEFAULT, RowParser.SKIP_FIRST))
                        .describedAs(sample.asDescription("Reading"))
                        .containsExactlyElementsOf(sample.getRows().subList(1, sample.getRows().size()));
                assertThat(readRows(sample, Csv.ReaderOptions.DEFAULT, RowParser.SKIP_NOT_LAST))
                        .describedAs(sample.asDescription("Reading"))
                        .containsExactlyElementsOf(sample.getRows().subList(2, sample.getRows().size()));
                break;
        }
    }

    @ParameterizedTest
    @MethodSource("_test.Sample#getAllSamples")
    public void testReadFieldBeforeLine(Sample sample) {
        VoidParser readFieldBeforeLine = Csv.Reader::readField;

        assertThatIllegalStateException()
                .describedAs(sample.asDescription("Reading"))
                .isThrownBy(() -> QuickReader.read(readFieldBeforeLine, sample.getContent(), sample.getFormat(), Csv.ReaderOptions.DEFAULT));
    }

    @Test
    public void testNonQuotedNonNewLineChar() {
        assertThat(Sample
                .builder()
                .name("Invalid but still ok")
                .format(Csv.Format.RFC4180)
                .content("\r\r\n")
                .rowFields("\r")
                .build()
        ).is(validWithStrict);
    }

    @Test
    public void testReusableFieldOverflow() {
        String field1 = IntStream.range(0, 70).mapToObj(i -> "A").collect(joining());
        String field2 = IntStream.range(0, 10).mapToObj(i -> "B").collect(joining());

        assertThat(Sample
                .builder()
                .name("overflow")
                .format(Csv.Format.RFC4180)
                .content(field1 + "," + field2)
                .rowFields(field1, field2)
                .build()
        ).is(validWithStrict);
    }

    @Test
    public void testEmptyLine() throws IOException {
        Sample sample = Sample.EMPTY_LINES;
        try (Csv.Reader reader = Csv.Reader.of(sample.getFormat(), Csv.ReaderOptions.DEFAULT, new StringReader(sample.getContent()))) {
            assertThat(reader.readLine()).isTrue();
            assertThat(reader.readField()).isFalse();
            assertThat(reader.readLine()).isTrue();
            assertThat(reader.readField()).isFalse();
            assertThat(reader.readLine()).isFalse();
        }
    }

    @Test
    void testEveryLineHasAtLeastOneField() throws IOException {
        String csv = "A\r\n"
                + "\r\n"
                + "B\r\n";

        Csv.Format validRFC4180 = RFC4180.toBuilder().acceptMissingField(false).build();
        try (Csv.Reader reader = Csv.Reader.of(validRFC4180, Csv.ReaderOptions.DEFAULT, new StringReader(csv))) {
            assertThat(reader.readLine()).isTrue();
            assertThat(reader.readField()).isTrue();
            assertThat(reader.toString()).isEqualTo("A");
            assertThat(reader.readField()).isFalse();

            assertThat(reader.readLine()).isTrue();
            assertThat(reader.readField()).isTrue(); // here
            assertThat(reader).hasToString("");
            assertThat(reader.readField()).isFalse();

            assertThat(reader.readLine()).isTrue();
            assertThat(reader.readField()).isTrue();
            assertThat(reader.toString()).isEqualTo("B");
            assertThat(reader.readField()).isFalse();

            assertThat(reader.readLine()).isFalse();
        }
    }

    @Test
    public void testEmptyFirstField() {
        assertThat(Sample
                .builder()
                .name("Empty first field")
                .format(Csv.Format.RFC4180)
                .content(",B1\r\nA2,B2\r\n")
                .rowFields("", "B1")
                .rowFields("A2", "B2")
                .build()
        ).is(validWithStrict);
    }

    @Test
    public void testEmptyLastField() {
        assertThat(Sample
                .builder()
                .name("Empty last field")
                .format(Csv.Format.RFC4180)
                .content("A1,")
                .rowFields("A1", "")
                .build()
        ).is(validWithStrict);
    }

    @Test
    public void testCharSequence() throws IOException {
        Sample sample = Sample.SIMPLE;
        try (Csv.Reader reader = Csv.Reader.of(sample.getFormat(), Csv.ReaderOptions.DEFAULT, new StringReader(sample.getContent()))) {
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

        Csv.ReaderOptions valid = Csv.ReaderOptions.DEFAULT.toBuilder().maxCharsPerField(2).build();
        assertThatCode(() -> readRows(sample, valid, RowParser.READ_ALL))
                .describedAs(sample.asDescription("Reading"))
                .doesNotThrowAnyException();

        Csv.ReaderOptions invalid = Csv.ReaderOptions.DEFAULT.toBuilder().maxCharsPerField(1).build();
        assertThatIOException().isThrownBy(() -> readRows(sample, invalid, RowParser.READ_ALL))
                .describedAs(sample.asDescription("Reading"))
                .withMessageContaining("Field overflow");
    }

    @Test
    public void testLenientParsing() {
        Sample.Builder base = Sample.builder().name("lenient").format(Csv.Format.DEFAULT).rowFields("R1").rowFields("R2");

        assertThat(base.content("R1" + WINDOWS_SEPARATOR + "R2").build())
                .is(validWithStrict)
                .is(validWithLenient);

        assertThat(base.content("R1" + UNIX_SEPARATOR + "R2").build())
                .isNot(validWithStrict)
                .is(validWithLenient);

        assertThat(base.content("R1" + MACINTOSH_SEPARATOR + "R2").build())
                .isNot(validWithStrict)
                .is(validWithLenient);
    }

    @Test
    public void testKeyValuePairs() {
        assertThat(Sample
                .builder()
                .format(Csv.Format.builder().delimiter('=').separator(",").build())
                .content("k1=v1,k2=v2").rowFields("k1", "v1").rowFields("k2", "v2")
                .build()
        ).is(validWithStrict).is(validWithLenient);

        assertThat(Sample
                .builder()
                .format(Csv.Format.builder().delimiter('=').separator(", ").build())
                .content("k1=v1, k2=v2").rowFields("k1", "v1").rowFields("k2", "v2")
                .build()
        ).is(validWithStrict).is(validWithLenient);

        assertThat(Sample
                .builder()
                .format(Csv.Format.builder().delimiter('=').separator(", ").build())
                .content("k1=v1,k2=v2").rowFields("k1", "v1").rowFields("k2", "v2")
                .build()
        ).isNot(validWithStrict).is(validWithLenient);
    }

    @Test
    public void testComment() {
        assertThat(Sample
                .builder()
                .format(RFC4180)
                .content("A\n#B,C\nD")
                .rowFields("A").rowComment("B,C").rowFields("D")
                .build()
        ).isNot(validWithStrict).is(validWithLenient);

        assertThat(Sample
                .builder()
                .format(RFC4180)
                .content("A\r#B,C\rD")
                .rowFields("A").rowComment("B,C").rowFields("D")
                .build()
        ).isNot(validWithStrict).is(validWithLenient);

        assertThat(Sample
                .builder()
                .format(RFC4180)
                .content("A\r\n#B,C\r\nD")
                .rowFields("A").rowComment("B,C").rowFields("D")
                .build()
        ).is(validWithStrict).is(validWithLenient);

        assertThat(Sample
                .builder()
                .format(RFC4180)
                .content("#A\n#B\nC")
                .rowComment("A").rowComment("B").rowFields("C")
                .build()
        ).isNot(validWithStrict).is(validWithLenient);

        assertThat(Sample
                .builder()
                .format(RFC4180)
                .content("#A\n #B\nC")
                .rowComment("A").rowFields(" #B").rowFields("C")
                .build()
        ).isNot(validWithStrict).is(validWithLenient);

        assertThat(Sample
                .builder()
                .format(RFC4180)
                .content("#")
                .rowComment("")
                .build()
        ).is(validWithStrict).is(validWithLenient);

        assertThat(Sample
                .builder()
                .format(RFC4180)
                .content("#\n#")
                .rowComment("").rowComment("")
                .build()
        ).isNot(validWithStrict).is(validWithLenient);

        assertThat(Sample
                .builder()
                .format(RFC4180)
                .content("#A\n#")
                .rowComment("A").rowComment("")
                .build()
        ).isNot(validWithStrict).is(validWithLenient);

        assertThat(Sample
                .builder()
                .format(RFC4180)
                .content("#\n#A")
                .rowComment("").rowComment("A")
                .build()
        ).isNot(validWithStrict).is(validWithLenient);

        assertThat(Sample
                .builder()
                .format(RFC4180)
                .content("#\n#\r\n#")
                .rowComment("").rowComment("").rowComment("")
                .build()
        ).isNot(validWithStrict).is(validWithLenient);

        assertThat(Sample
                .builder()
                .format(RFC4180)
                .content("A,#B")
                .rowFields("A", "#B")
                .build()
        ).is(validWithStrict).is(validWithLenient);
    }

    @Test
    public void testIsComment() throws IOException {
        Sample sample = Sample.COMMENTED;

        QuickReader.Parser<Boolean> isCommentBeforeLine = Csv.Reader::isComment;
        assertThat(QuickReader.readValue(isCommentBeforeLine, sample.getContent(), sample.getFormat(), Csv.ReaderOptions.DEFAULT))
                .isFalse();

        QuickReader.Parser<Boolean> isCommentAfterLine = reader -> {
            reader.readLine();
            return reader.isComment();
        };
        assertThat(QuickReader.readValue(isCommentAfterLine, sample.getContent(), sample.getFormat(), Csv.ReaderOptions.DEFAULT))
                .isTrue();

        QuickReader.Parser<Boolean> isCommentAfterField = reader -> {
            reader.readLine();
            reader.readField();
            return reader.isComment();
        };
        assertThat(QuickReader.readValue(isCommentAfterField, sample.getContent(), sample.getFormat(), Csv.ReaderOptions.DEFAULT))
                .isTrue();
    }

    @Test
    public void testSurrogatePair() {
        String grinning = "😀";
        String wink = "😉";
        assertThat(Sample
                .builder()
                .format(RFC4180)
                .content(grinning + ",hello\r\nworld," + wink + "\r\n")
                .rowFields(grinning, "hello").rowFields("world", wink)
                .build()
        ).is(validWithStrict).is(validWithLenient);
    }

    @Test
    public void testEndOfLine() throws IOException {
        Csv.ReaderOptions strict = Csv.ReaderOptions.DEFAULT;
        Csv.ReaderOptions lenient = Csv.ReaderOptions.DEFAULT.toBuilder().lenientSeparator(true).build();

        Csv.Format single = RFC4180.toBuilder().separator("␊").quote('=').comment('!').build();
        Csv.Format dual = RFC4180.toBuilder().separator("␍␊").quote('=').comment('!').build();

        // FIELD_TYPE_QUOTED
        {
            // 1. EOL_TYPE_SINGLE
            assertThat(format("=A=␊B", single, strict)).isEqualTo("A⏎B");
            assertThat(format("=A=␊", single, strict)).isEqualTo("A");
            assertThat(format("=A=␊B", single, lenient)).isEqualTo("A⏎B");
            assertThat(format("=A=␊", single, lenient)).isEqualTo("A");
            // 2. EOL_TYPE_DUAL_STRICT
            assertThat(format("=A=␍␊B", dual, strict)).isEqualTo("A⏎B");
            assertThat(format("=A=␍␊", dual, strict)).isEqualTo("A");
            assertThat(format("=A=␍B", dual, strict)).isEqualTo("A␍B");
            assertThat(format("=A=␍", dual, strict)).isEqualTo("A␍");
            // 3. EOL_TYPE_DUAL_LENIENT first
            assertThat(format("=A=␍B", dual, lenient)).isEqualTo("A⏎B");
            assertThat(format("=A=␍", dual, lenient)).isEqualTo("A");
            // 4. EOL_TYPE_DUAL_LENIENT full
            assertThat(format("=A=␍␊B", dual, lenient)).isEqualTo("A⏎B");
            assertThat(format("=A=␍␊", dual, lenient)).isEqualTo("A");
            // 5. EOL_TYPE_DUAL_LENIENT second
            assertThat(format("=A=␊B", dual, lenient)).isEqualTo("A⏎B");
            assertThat(format("=A=␊", dual, lenient)).isEqualTo("A");
        }

        // FIELD_TYPE_COMMENTED
        {
            // 1. EOL_TYPE_SINGLE
            assertThat(format("!A␊B", single, strict)).isEqualTo("B");
            assertThat(format("!A␊", single, strict)).isEqualTo("");
            assertThat(format("!A␊B", single, lenient)).isEqualTo("B");
            assertThat(format("!A␊", single, lenient)).isEqualTo("");
            // 2. EOL_TYPE_DUAL_STRICT
            assertThat(format("!A␍␊B", dual, strict)).isEqualTo("B");
            assertThat(format("!A␍␊", dual, strict)).isEqualTo("");
            assertThat(format("!A␍B", dual, strict)).isEqualTo("");
            assertThat(format("!A␍", dual, strict)).isEqualTo("");
            // 3. EOL_TYPE_DUAL_LENIENT first
            assertThat(format("!A␍B", dual, lenient)).isEqualTo("B");
            assertThat(format("!A␍", dual, lenient)).isEqualTo("");
            // 4. EOL_TYPE_DUAL_LENIENT full
            assertThat(format("!A␍␊B", dual, lenient)).isEqualTo("B");
            assertThat(format("!A␍␊", dual, lenient)).isEqualTo("");
            // 5. EOL_TYPE_DUAL_LENIENT second
            assertThat(format("!A␊B", dual, lenient)).isEqualTo("B");
            assertThat(format("!A␊", dual, lenient)).isEqualTo("");
        }

        // FIELD_TYPE_NORMAL empty
        {
            // 1. EOL_TYPE_SINGLE
            assertThat(format("␊B", single, strict)).isEqualTo("⏎B");
            assertThat(format("␊", single, strict)).isEqualTo("");
            assertThat(format("␊B", single, lenient)).isEqualTo("⏎B");
            assertThat(format("␊", single, lenient)).isEqualTo("");
            // 2. EOL_TYPE_DUAL_STRICT
            assertThat(format("␍␊B", dual, strict)).isEqualTo("⏎B");
            assertThat(format("␍␊", dual, strict)).isEqualTo("");
            assertThat(format("␍B", dual, strict)).isEqualTo("␍B");
            assertThat(format("␍", dual, strict)).isEqualTo("␍");
            // 3. EOL_TYPE_DUAL_LENIENT first
            assertThat(format("␍B", dual, lenient)).isEqualTo("⏎B");
            assertThat(format("␍", dual, lenient)).isEqualTo("");
            // 4. EOL_TYPE_DUAL_LENIENT full
            assertThat(format("␍␊B", dual, lenient)).isEqualTo("⏎B");
            assertThat(format("␍␊", dual, lenient)).isEqualTo("");
            // 5. EOL_TYPE_DUAL_LENIENT second
            assertThat(format("␊B", dual, lenient)).isEqualTo("⏎B");
            assertThat(format("␊", dual, lenient)).isEqualTo("");
        }

        // FIELD_TYPE_NORMAL non-empty
        {
            // 1. EOL_TYPE_SINGLE
            assertThat(format("A␊B", single, strict)).isEqualTo("A⏎B");
            assertThat(format("A␊", single, strict)).isEqualTo("A");
            assertThat(format("A␊B", single, lenient)).isEqualTo("A⏎B");
            assertThat(format("A␊", single, lenient)).isEqualTo("A");
            // 2. EOL_TYPE_DUAL_STRICT
            assertThat(format("A␍␊B", dual, strict)).isEqualTo("A⏎B");
            assertThat(format("A␍␊", dual, strict)).isEqualTo("A");
            assertThat(format("A␍B", dual, strict)).isEqualTo("A␍B");
            assertThat(format("A␍", dual, strict)).isEqualTo("A␍");
            // 3. EOL_TYPE_DUAL_LENIENT first
            assertThat(format("A␍B", dual, lenient)).isEqualTo("A⏎B");
            assertThat(format("A␍", dual, lenient)).isEqualTo("A");
            // 4. EOL_TYPE_DUAL_LENIENT full
            assertThat(format("A␍␊B", dual, lenient)).isEqualTo("A⏎B");
            assertThat(format("A␍␊", dual, lenient)).isEqualTo("A");
            // 5. EOL_TYPE_DUAL_LENIENT second
            assertThat(format("A␊B", dual, lenient)).isEqualTo("A⏎B");
            assertThat(format("A␊", dual, lenient)).isEqualTo("A");
        }
    }

    @ParameterizedTest
    @MethodSource("_test.fastcsv.FastCsvEntry#loadAll")
    public void testFastCsvSamples(FastCsvEntry entry) throws IOException {
        Sample sample = FastCsvEntryConverter.toSample(entry);
        QuickReader.Parser<List<Row>> rowsParser = new FastCsvEntryRowsParser(entry);

        assertThat(readRows(sample, Csv.ReaderOptions.builder().lenientSeparator(true).build(), rowsParser))
                .containsExactlyElementsOf(sample.getRows());
    }

    private final Condition<Sample> validWithStrict = validWith(Csv.ReaderOptions.builder().lenientSeparator(false).build());
    private final Condition<Sample> validWithLenient = validWith(Csv.ReaderOptions.builder().lenientSeparator(true).build());

    private static Condition<Sample> validWith(Csv.ReaderOptions options) {
        Function<Sample, List<Row>> rowFunc = sample -> {
            try {
                return readRows(sample, options, RowParser.READ_ALL);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        };
        return VerboseCondition.verboseCondition(
                sample -> rowFunc.apply(sample).equals(sample.getRows()),
                "generating the expected rows",
                o -> " but generated '" + rowFunc.apply(o) + "' instead"
        );
    }

    private static List<Row> readRows(Sample sample, Csv.ReaderOptions options, QuickReader.Parser<List<Row>> rowsParser) throws IOException {
        return QuickReader.readValue(rowsParser, sample.getContent(), sample.getFormat(), options);
    }

    private enum RowParser implements QuickReader.Parser<List<Row>> {

        READ_ALL {
            @Override
            public List<Row> accept(Csv.Reader reader) throws IOException {
                return Row.readAll(reader, Row::appendEmpty, Row::appendComment, Row::appendFields);
            }
        }, SKIP_FIRST {
            @Override
            public List<Row> accept(Csv.Reader reader) throws IOException {
                reader.readLine();
                return Row.readAll(reader, Row::appendEmpty, Row::appendComment, Row::appendFields);
            }
        }, SKIP_NOT_LAST {
            @Override
            public List<Row> accept(Csv.Reader reader) throws IOException {
                reader.readLine();
                reader.readField();
                reader.readLine();
                return Row.readAll(reader, Row::appendEmpty, Row::appendComment, Row::appendFields);
            }
        }
    }

    private static String format(String input, Csv.Format format, Csv.ReaderOptions options) throws IOException {
        return QuickReader.readValue(CsvReaderTest::format, input, format, options);
    }

    private static String format(Csv.Reader reader) throws IOException {
        try {
            return Cookbook.asStream(reader)
                    .filter(lineReader -> !lineReader.isComment())
                    .map(((Cookbook.LineParser<String[]>) Cookbook::readLineOfUnknownSize).asUnchecked())
                    .map(array -> String.join("↷", array))
                    .collect(joining("⏎"));
        } catch (UncheckedIOException ex) {
            throw ex.getCause();
        }
    }
}
