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
import _test.Resources;
import _test.Row;
import _test.Sample;
import _test.fastcsv.FastCsvEntry;
import _test.fastcsv.FastCsvEntryConverter;
import _test.fastcsv.FastCsvEntryRowsParser;
import lombok.NonNull;
import org.assertj.core.api.Condition;
import org.assertj.core.condition.VerboseCondition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static _test.Sample.INVALID_FORMAT;
import static java.nio.charset.StandardCharsets.UTF_8;
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
    public void testFullSupportOfQuoting(Sample sample) throws IOException {
        try (Csv.Reader reader = Csv.Reader.of(sample.getFormat(), Csv.ReaderOptions.DEFAULT, new StringReader(sample.getContent()))) {
            StringWriter actual = new StringWriter();
            try (Csv.Writer writer = Csv.Writer.of(sample.getFormat(), Csv.WriterOptions.DEFAULT, actual)) {
                if (reader.readLine()) {
                    boolean wasComment = false;
                    if (reader.isComment()) {
                        wasComment = true;
                        writer.writeComment(reader);
                    } else {
                        while (reader.readField()) {
                            if (reader.isQuoted()) writer.writeQuotedField(reader);
                            else writer.writeField(reader);
                        }
                    }
                    while (reader.readLine()) {
                        if (!wasComment) writer.writeEndOfLine();
                        if (reader.isComment()) {
                            wasComment = true;
                            writer.writeComment(reader);
                        } else {
                            wasComment = false;
                            while (reader.readField()) {
                                if (reader.isQuoted()) writer.writeQuotedField(reader);
                                else writer.writeField(reader);
                            }
                        }
                    }
                    if (!sample.isWithoutEOL()) writer.writeEndOfLine();
                }
            }
            assertThat(actual.toString()).isEqualTo(sample.getContent());
        }
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
    public void testIsQuoted() throws IOException {
        String sample = "\"A\",B";
        QuickReader.read(r -> {
            assertThat(r.readLine()).isTrue();
            assertThat(r.readField()).isTrue();
            assertThat(r.isQuoted()).isTrue();
            assertThat(r.readField()).isTrue();
            assertThat(r.isQuoted()).isFalse();
            assertThat(r.readField()).isFalse();
            assertThat(r.readLine()).isFalse();
        }, sample, RFC4180, Csv.ReaderOptions.DEFAULT);
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

    @ParameterizedTest
    @ValueSource(ints = {1, 2, DEFAULT_CHAR_BUFFER_SIZE})
    public void testEndOfLine(int charBufferSize) throws IOException {
        Csv.ReaderOptions strict = Csv.ReaderOptions.DEFAULT.toBuilder().lenientSeparator(false).build();
        Csv.ReaderOptions lenient = Csv.ReaderOptions.DEFAULT.toBuilder().lenientSeparator(true).build();

        Csv.Format single = RFC4180.toBuilder().separator(UNIX_SEPARATOR).build();
        Csv.Format dual = RFC4180.toBuilder().separator(WINDOWS_SEPARATOR).build();

        SymbolParser singleStrict = new SymbolParser(single, strict, charBufferSize);
        SymbolParser singleLenient = new SymbolParser(single, lenient, charBufferSize);
        SymbolParser dualStrict = new SymbolParser(dual, strict, charBufferSize);
        SymbolParser dualLenient = new SymbolParser(dual, lenient, charBufferSize);

        // FIELD_TYPE_QUOTED
        {
            // 1. EOL_TYPE_SINGLE
            assertThat(singleStrict.parse("=A=␊B")).isEqualTo("A⏎B");
            assertThat(singleStrict.parse("=A=␊")).isEqualTo("A");
            assertThat(singleLenient.parse("=A=␊B")).isEqualTo("A⏎B");
            assertThat(singleLenient.parse("=A=␊")).isEqualTo("A");
            // 2. EOL_TYPE_DUAL_STRICT
            assertThat(dualStrict.parse("=A=␍␊B")).isEqualTo("A⏎B");
            assertThat(dualStrict.parse("=A=␍␊")).isEqualTo("A");
            assertThat(dualStrict.parse("=A=␍B")).isEqualTo("A␍B");
            assertThat(dualStrict.parse("=A=␍")).isEqualTo("A␍");
            // 3. EOL_TYPE_DUAL_LENIENT first
            assertThat(dualLenient.parse("=A=␍B")).isEqualTo("A⏎B");
            assertThat(dualLenient.parse("=A=␍")).isEqualTo("A");
            // 4. EOL_TYPE_DUAL_LENIENT full
            assertThat(dualLenient.parse("=A=␍␊B")).isEqualTo("A⏎B");
            assertThat(dualLenient.parse("=A=␍␊")).isEqualTo("A");
            // 5. EOL_TYPE_DUAL_LENIENT second
            assertThat(dualLenient.parse("=A=␊B")).isEqualTo("A⏎B");
            assertThat(dualLenient.parse("=A=␊")).isEqualTo("A");
        }

        // FIELD_TYPE_COMMENTED
        {
            // 1. EOL_TYPE_SINGLE
            assertThat(singleStrict.parse("!A␊B")).isEqualTo("…A⏎B");
            assertThat(singleStrict.parse("!A␊")).isEqualTo("…A");
            assertThat(singleLenient.parse("!A␊B")).isEqualTo("…A⏎B");
            assertThat(singleLenient.parse("!A␊")).isEqualTo("…A");
            // 2. EOL_TYPE_DUAL_STRICT
            assertThat(dualStrict.parse("!A␍␊B")).isEqualTo("…A⏎B");
            assertThat(dualStrict.parse("!A␍␊")).isEqualTo("…A");
            assertThat(dualStrict.parse("!A␍B")).isEqualTo("…A␍B");
            assertThat(dualStrict.parse("!A␍")).isEqualTo("…A␍");
            // 3. EOL_TYPE_DUAL_LENIENT first
            assertThat(dualLenient.parse("!A␍B")).isEqualTo("…A⏎B");
            assertThat(dualLenient.parse("!A␍")).isEqualTo("…A");
            // 4. EOL_TYPE_DUAL_LENIENT full
            assertThat(dualLenient.parse("!A␍␊B")).isEqualTo("…A⏎B");
            assertThat(dualLenient.parse("!A␍␊")).isEqualTo("…A");
            // 5. EOL_TYPE_DUAL_LENIENT second
            assertThat(dualLenient.parse("!A␊B")).isEqualTo("…A⏎B");
            assertThat(dualLenient.parse("!A␊")).isEqualTo("…A");
        }

        // FIELD_TYPE_NORMAL empty
        {
            // 1. EOL_TYPE_SINGLE
            assertThat(singleStrict.parse("␊B")).isEqualTo("⏎B");
            assertThat(singleStrict.parse("␊")).isEqualTo("");
            assertThat(singleLenient.parse("␊B")).isEqualTo("⏎B");
            assertThat(singleLenient.parse("␊")).isEqualTo("");
            // 2. EOL_TYPE_DUAL_STRICT
            assertThat(dualStrict.parse("␍␊B")).isEqualTo("⏎B");
            assertThat(dualStrict.parse("␍␊")).isEqualTo("");
            assertThat(dualStrict.parse("␍B")).isEqualTo("␍B");
            assertThat(dualStrict.parse("␍")).isEqualTo("␍");
            // 3. EOL_TYPE_DUAL_LENIENT first
            assertThat(dualLenient.parse("␍B")).isEqualTo("⏎B");
            assertThat(dualLenient.parse("␍")).isEqualTo("");
            // 4. EOL_TYPE_DUAL_LENIENT full
            assertThat(dualLenient.parse("␍␊B")).isEqualTo("⏎B");
            assertThat(dualLenient.parse("␍␊")).isEqualTo("");
            // 5. EOL_TYPE_DUAL_LENIENT second
            assertThat(dualLenient.parse("␊B")).isEqualTo("⏎B");
            assertThat(dualLenient.parse("␊")).isEqualTo("");
        }

        // FIELD_TYPE_NORMAL non-empty
        {
            // 1. EOL_TYPE_SINGLE
            assertThat(singleStrict.parse("A␊B")).isEqualTo("A⏎B");
            assertThat(singleStrict.parse("A␊")).isEqualTo("A");
            assertThat(singleLenient.parse("A␊B")).isEqualTo("A⏎B");
            assertThat(singleLenient.parse("A␊")).isEqualTo("A");
            // 2. EOL_TYPE_DUAL_STRICT
            assertThat(dualStrict.parse("A␍␊B")).isEqualTo("A⏎B");
            assertThat(dualStrict.parse("A␍␊")).isEqualTo("A");
            assertThat(dualStrict.parse("A␍B")).isEqualTo("A␍B");
            assertThat(dualStrict.parse("A␍")).isEqualTo("A␍");
            // 3. EOL_TYPE_DUAL_LENIENT first
            assertThat(dualLenient.parse("A␍B")).isEqualTo("A⏎B");
            assertThat(dualLenient.parse("A␍")).isEqualTo("A");
            // 4. EOL_TYPE_DUAL_LENIENT full
            assertThat(dualLenient.parse("A␍␊B")).isEqualTo("A⏎B");
            assertThat(dualLenient.parse("A␍␊")).isEqualTo("A");
            // 5. EOL_TYPE_DUAL_LENIENT second
            assertThat(dualLenient.parse("A␊B")).isEqualTo("A⏎B");
            assertThat(dualLenient.parse("A␊")).isEqualTo("A");
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

    @Test
    public void testBufferRelocationBufferLength() throws IOException {
        String content = Resources.contentOf(CsvReaderTest.class, "/Top5Stmt.tsv", UTF_8);
        Csv.Format format = Csv.Format.builder().delimiter('\t').build();
        Csv.ReaderOptions options = Csv.ReaderOptions.builder().lenientSeparator(true).build();

        assertThat(QuickReader.readValue(RowParser.READ_ALL, content, format, options))
                .hasSize(332)
                .map(Row.Fields.class::cast)
                .map(row -> row.getFields().size())
                .allMatch(fieldsCount -> fieldsCount == 4);
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

    @lombok.AllArgsConstructor
    private static final class SymbolParser {

        private final Csv.Format format;
        private final Csv.ReaderOptions options;
        private final int charBufferSize;

        public @NonNull String parse(@NonNull String input) throws IOException {
            return QuickReader.readValue(SymbolParser::parse, fromSymbols(input), format, options, charBufferSize);
        }

        private static String fromSymbols(String input) {
            return input
                    .replace('!', '#')
                    .replace('=', '"')
                    .replace('␍', '\r')
                    .replace('␊', '\n');
        }


        private static String toSymbols(String input) {
            return input
                    .replace('#', '!')
                    .replace('"', '=')
                    .replace('\r', '␍')
                    .replace('\n', '␊');
        }

        private static String parse(Csv.Reader reader) throws IOException {
            try {
                return Cookbook.asStream(reader)
                        .map(Cookbook.LineParser.unchecked(SymbolParser::parse))
                        .collect(joining("⏎"));
            } catch (UncheckedIOException ex) {
                throw ex.getCause();
            }
        }

        private static String parse(Csv.LineReader lineReader) throws IOException {
            return (lineReader.isComment() ? "…" : "") + Stream.of(Cookbook.readLineOfUnknownSize(lineReader)).map(SymbolParser::toSymbols).collect(joining("↷"));
        }
    }
}
