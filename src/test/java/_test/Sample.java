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
package _test;

import nbbrd.picocsv.Csv;
import org.apache.commons.text.StringEscapeUtils;
import org.assertj.core.description.Description;
import org.assertj.core.description.TextDescription;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static nbbrd.picocsv.Csv.DEFAULT_CHAR_BUFFER_SIZE;

/**
 * @author Philippe Charles
 */
@lombok.Value
@lombok.Builder(builderClassName = "Builder", toBuilder = true)
@lombok.With
public class Sample {

    @lombok.NonNull
    @lombok.Builder.Default
    String name = "";

    @lombok.NonNull
    Csv.Format format;

    @lombok.NonNull
    String content;

    @lombok.Singular
    List<Row> rows;

    boolean withoutEOL;

    @Override
    public String toString() {
        return "Sample(name=" + name
                + ", format=" + format
                + ", content=" + StringEscapeUtils.escapeJava(content)
                + ", rows=" + getRowsAsString()
                + ", withoutEOL=" + withoutEOL
                + ")";
    }

    public Description asDescription(String prefix) {
        return new TextDescription(prefix + " '%s'", getName());
    }

    public String getRowsAsString() {
        return rows.stream().map(row -> "[" + row + "]").collect(Collectors.joining(","));
    }

    public static final class Builder {

        public Builder rowEmpty() {
            return row(new Row.Empty());
        }

        public Builder rowComment(String comment) {
            return row(new Row.Comment(comment));
        }

        public Builder rowFields(String... fields) {
            return row(new Row.Fields(Arrays.asList(fields)));
        }
    }

    public static final Sample EMPTY = Sample
            .builder()
            .name("Empty")
            .format(Csv.Format.RFC4180)
            .content("")
            .build();

    public static final Sample BLANK = Sample
            .builder()
            .name("Blank")
            .format(Csv.Format.RFC4180)
            .content("A1,,C1\r\n")
            .rowFields("A1", "", "C1")
            .build();

    public static final Sample SIMPLE = Sample
            .builder()
            .name("Simple")
            .format(Csv.Format.RFC4180)
            .content("A1,B1\r\nA2,B2\r\n")
            .rowFields("A1", "B1")
            .rowFields("A2", "B2")
            .build();

    public static final Sample SIMPLE_WITHOUT_LAST_EOL = Sample
            .builder()
            .name("Simple without last end-of-line")
            .format(Csv.Format.RFC4180)
            .content("A1,B1\r\nA2,B2")
            .rowFields("A1", "B1")
            .rowFields("A2", "B2")
            .withoutEOL(true)
            .build();

    public static final Sample ESCAPED_QUOTES = Sample
            .builder()
            .name("Escaped quotes")
            .format(Csv.Format.RFC4180)
            .content("A1,\"B\"\"1\"\"\"\r\nA2,B2\r\n")
            .rowFields("A1", "B\"1\"")
            .rowFields("A2", "B2")
            .build();

    public static final Sample NEW_LINES = Sample
            .builder()
            .name("New lines")
            .format(Csv.Format.RFC4180)
            .content("A1,\"B\r\n1\",C1\r\nA2,B2,C2\r\n")
            .rowFields("A1", "B\r\n1", "C1")
            .rowFields("A2", "B2", "C2")
            .build();

    public static final Sample COMMA_IN_QUOTES = Sample
            .builder()
            .name("Comma in quotes")
            .format(Csv.Format.RFC4180)
            .content("A1,\"B,1\",C1\r\nA2,B2,C2\r\n")
            .rowFields("A1", "B,1", "C1")
            .rowFields("A2", "B2", "C2")
            .build();

    public static final Sample ESCAPED_QUOTES_AND_NEW_LINES = Sample
            .builder()
            .name("Escaped quotes and new lines")
            .format(Csv.Format.RFC4180)
            .content("A1,\"B\r\n\"\"1\"\"\"\r\nA2,\"B\"\"\r\n2\"\"\"\r\n")
            .rowFields("A1", "B\r\n\"1\"")
            .rowFields("A2", "B\"\r\n2\"")
            .build();

    public static final Sample SINGLE_FIELD = Sample
            .builder()
            .name("Single field")
            .format(Csv.Format.RFC4180)
            .content("A1\r\nA2")
            .rowFields("A1")
            .rowFields("A2")
            .withoutEOL(true)
            .build();

    public static final Sample EMPTY_LINES = Sample
            .builder()
            .name("Empty lines")
            .format(Csv.Format.RFC4180)
            .content("\r\n\r\n")
            .rowEmpty()
            .rowEmpty()
            .build();

    public static final Sample UNQUOTED_EMPTY_THEN_LINE = Sample
            .builder()
            .name("Single empty field & Line")
            .format(Csv.Format.RFC4180)
            .content("\r\nA2\r\n")
            .rowEmpty()
            .rowFields("A2")
            .build();

    public static final Sample QUOTED_EMPTY_THEN_LINE = Sample
            .builder()
            .name("Quoted single empty field & Line")
            .format(Csv.Format.RFC4180)
            .content("\"\"\r\nA2\r\n")
            .rowFields("")
            .rowFields("A2")
            .build();

    public static final Sample LINE_THEN_UNQUOTED_EMPTY = Sample
            .builder()
            .name("Line & Single empty field")
            .format(Csv.Format.RFC4180)
            .content("A1\r\n\r\n")
            .rowFields("A1")
            .rowEmpty()
            .build();

    public static final Sample LINE_THEN_QUOTED_EMPTY = Sample
            .builder()
            .name("Line & Quoted single empty field")
            .format(Csv.Format.RFC4180)
            .content("A1\r\n\"\"\r\n")
            .rowFields("A1")
            .rowFields("")
            .build();

    public static final Sample UNQUOTED_EMPTY = Sample
            .builder()
            .name("Single empty field")
            .format(Csv.Format.RFC4180)
            .content("\r\n")
            .rowEmpty()
            .build();

    public static final Sample QUOTED_EMPTY = Sample
            .builder()
            .name("Quoted single empty field")
            .format(Csv.Format.RFC4180)
            .content("\"\"\r\n")
            .rowFields("")
            .build();

    public static final Sample QUOTED_EMPTY_WITHOUT_LAST_EOL = Sample
            .builder()
            .name("Quoted single empty field without last EOL")
            .format(Csv.Format.RFC4180)
            .content("\"\"")
            .rowFields("")
            .withoutEOL(true)
            .build();

    public static final Sample COMMENTED = Sample
            .builder()
            .name("Commented")
            .format(Csv.Format.RFC4180)
            .content("#hello\r\nA1,B1\r\nA2,B2\r\n")
            .rowComment("hello")
            .rowFields("A1", "B1")
            .rowFields("A2", "B2")
            .build();

    public static final List<Character> SPECIAL_CHARS = Arrays.asList(',', '\t', ';', '\r', '\n', '\'', '"', '\f', '\b', '\\');

    public static final List<String> SEPARATORS = Arrays.asList(
            Csv.Format.WINDOWS_SEPARATOR,
            Csv.Format.UNIX_SEPARATOR,
            Csv.Format.MACINTOSH_SEPARATOR
    );

    private static List<Csv.Format> generateFormats() {
        List<Csv.Format> result = new ArrayList<>();
        for (String separator : SEPARATORS) {
            for (char delimiter : SPECIAL_CHARS) {
                for (char quote : SPECIAL_CHARS) {
                    for (char comment : SPECIAL_CHARS) {
                        result.add(Csv.Format
                                .builder()
                                .separator(separator)
                                .delimiter(delimiter)
                                .quote(quote)
                                .comment(comment)
                                .build()
                        );
                    }
                }
            }
        }
        return result;
    }

    private static List<String> generateSpecialCharsFields() {
        return SPECIAL_CHARS
                .stream()
                .map(String::valueOf)
                .flatMap(Sample::getFieldsContainingSpecialChar)
                .collect(Collectors.toList());
    }

    private static Stream<String> getFieldsContainingSpecialChar(String c) {
        return Stream.of(
                c,
                c + "2", "1" + c,
                c + "23", "1" + c + "3", "12" + c
        );
    }

    private static String toContent(Csv.Format format, Row.Fields... rows) {
        StringWriter result = new StringWriter();
        try (Csv.Writer writer = Csv.Writer.of(format, Csv.WriterOptions.DEFAULT, result, DEFAULT_CHAR_BUFFER_SIZE)) {
            Row.writeAll(Arrays.asList(rows), writer);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        return result.toString();
    }

    private static List<Sample> generateSamples() {
        List<String> generatedFields = generateSpecialCharsFields();
        return generateFormats()
                .stream()
                .filter(Csv.Format::isValid)
                .map(format -> generateSample(format, generatedFields))
                .collect(Collectors.toList());
    }

    private static Sample generateSample(Csv.Format generatedFormat, List<String> generatedFields) {
        Row.Fields generatedRow = new Row.Fields(generatedFields);
        return Sample.builder()
                .name(generateName(generatedFormat))
                .format(generatedFormat)
                .content(toContent(generatedFormat, generatedRow, generatedRow))
                .row(generatedRow)
                .row(generatedRow)
                .build();
    }

    private static String generateName(Csv.Format format) {
        return "[" + StringEscapeUtils.escapeJava(format.getSeparator())
                + "" + StringEscapeUtils.escapeJava(String.valueOf(format.getDelimiter()))
                + "" + StringEscapeUtils.escapeJava(String.valueOf(format.getQuote()))
                + "" + StringEscapeUtils.escapeJava(String.valueOf(format.getComment()))
                + "]";
    }

    private static List<Sample> getPredefinedSamples() {
        return Arrays.asList(EMPTY,
                BLANK,
                SIMPLE,
                SIMPLE_WITHOUT_LAST_EOL,
                ESCAPED_QUOTES,
                NEW_LINES,
                COMMA_IN_QUOTES,
                ESCAPED_QUOTES_AND_NEW_LINES,
                SINGLE_FIELD,
                EMPTY_LINES,
                UNQUOTED_EMPTY_THEN_LINE,
                QUOTED_EMPTY_THEN_LINE,
                LINE_THEN_UNQUOTED_EMPTY,
                LINE_THEN_QUOTED_EMPTY,
                UNQUOTED_EMPTY,
                QUOTED_EMPTY,
                QUOTED_EMPTY_WITHOUT_LAST_EOL,
                COMMENTED
        );
    }

    private static final List<Sample> SAMPLES = Stream.concat(getPredefinedSamples().stream(), generateSamples().stream()).collect(Collectors.toList());

    public static List<Sample> getAllSamples() {
        return SAMPLES;
    }

    public static final Csv.Format INVALID_FORMAT = Csv.Format.DEFAULT.toBuilder().delimiter(':').quote(':').build();
}
