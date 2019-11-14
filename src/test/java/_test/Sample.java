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

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nbbrd.picocsv.Csv;

/**
 *
 * @author Philippe Charles
 */
@lombok.Value
@lombok.Builder(builderClassName = "Builder", toBuilder = true)
@lombok.experimental.Wither
public class Sample {

    @lombok.NonNull
    private String name;

    @lombok.NonNull
    private Csv.Format format;

    @lombok.NonNull
    private String content;

    @lombok.NonNull
    @lombok.Singular
    private List<Row> rows;

    private static Sample of(Csv.Format format, Row... rows) {
        return Sample.builder()
                .content(toString(format, rows))
                .format(format)
                .name(format.toString())
                .rows(Arrays.asList(rows))
                .build();
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
            .row(Row.of("A1", "", "C1"))
            .build();

    public static final Sample SIMPLE = Sample
            .builder()
            .name("Simple")
            .format(Csv.Format.RFC4180)
            .content("A1,B1\r\nA2,B2\r\n")
            .row(Row.of("A1", "B1"))
            .row(Row.of("A2", "B2"))
            .build();

    public static final Sample SIMPLE_WITHOUT_LAST_EOL = Sample
            .builder()
            .name("Simple without last end-of-line")
            .format(Csv.Format.RFC4180)
            .content("A1,B1\r\nA2,B2")
            .row(Row.of("A1", "B1"))
            .row(Row.of("A2", "B2"))
            .build();

    public static final Sample ESCAPED_QUOTES = Sample
            .builder()
            .name("Escaped quotes")
            .format(Csv.Format.RFC4180)
            .content("A1,\"B\"\"1\"\"\"\r\nA2,B2\r\n")
            .row(Row.of("A1", "B\"1\""))
            .row(Row.of("A2", "B2"))
            .build();

    public static final Sample NEW_LINES = Sample
            .builder()
            .name("New lines")
            .format(Csv.Format.RFC4180)
            .content("A1,\"B\r\n1\",C1\r\nA2,B2,C2\r\n")
            .row(Row.of("A1", "B\r\n1", "C1"))
            .row(Row.of("A2", "B2", "C2"))
            .build();

    public static final Sample COMMA_IN_QUOTES = Sample
            .builder()
            .name("Comma in quotes")
            .format(Csv.Format.RFC4180)
            .content("A1,\"B,1\",C1\r\nA2,B2,C2\r\n")
            .row(Row.of("A1", "B,1", "C1"))
            .row(Row.of("A2", "B2", "C2"))
            .build();

    public static final Sample ESCAPED_QUOTES_AND_NEW_LINES = Sample
            .builder()
            .name("Escaped quotes and new lines")
            .format(Csv.Format.RFC4180)
            .content("A1,\"B\r\n\"\"1\"\"\"\r\nA2,\"B\"\"\r\n2\"\"\"\r\n")
            .row(Row.of("A1", "B\r\n\"1\""))
            .row(Row.of("A2", "B\"\r\n2\""))
            .build();

    public static final Sample SINGLE_FIELD = Sample
            .builder()
            .name("Single field")
            .format(Csv.Format.RFC4180)
            .content("A1\r\nA2")
            .row(Row.of("A1"))
            .row(Row.of("A2"))
            .build();

    public static final Sample EMPTY_LINES = Sample
            .builder()
            .name("Empty lines")
            .format(Csv.Format.RFC4180)
            .content("\r\n\r\n")
            .row(Row.of())
            .row(Row.of())
            .build();

    private static final char[] SPECIAL_CHARS = {',', '\t', ';', '\r', '\n', '\'', '"'};

    private static List<Csv.Format> generateFormats() {
        List<Csv.Format> result = new ArrayList<>();
        for (Csv.NewLine newLine : Csv.NewLine.values()) {
            for (char delimiter : SPECIAL_CHARS) {
                for (char quote : SPECIAL_CHARS) {
                    result.add(Csv.Format
                            .builder()
                            .separator(newLine)
                            .delimiter(delimiter)
                            .quote(quote)
                            .build()
                    );
                }
            }
        }
        return result;
    }

    private static Row generateRow() {
        return Row.of(String.valueOf(SPECIAL_CHARS)
                .chars()
                .mapToObj(c -> String.valueOf((char) c))
                .flatMap(c -> Stream
                .of(
                        c,
                        c + "2", "1" + c,
                        c + "23", "1" + c + "3", "12" + c
                ))
                .toArray(String[]::new));
    }

    private static String toString(Csv.Format format, Row... rows) {
        StringWriter result = new StringWriter();
        try (Csv.Writer writer = Csv.Writer.of(result, format)) {
            Row.write(Arrays.asList(rows), writer);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return result.toString();
    }

    private static List<Sample> getGeneratedSamples() {
        Row row = generateRow();
        return generateFormats()
                .stream()
                .filter(Csv.Format::isValid)
                .map(format -> of(format, row, row))
                .collect(Collectors.toList());
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
                EMPTY_LINES
        );
    }

    public static final List<Sample> SAMPLES = Stream.concat(getPredefinedSamples().stream(), getGeneratedSamples().stream()).collect(Collectors.toList());

    public static final String getField(int length, char c) {
        char[] result = new char[length];
        Arrays.fill(result, c);
        return String.valueOf(result);
    }

    public static final List<Charset> CHARSETS = Arrays.asList(StandardCharsets.UTF_8, StandardCharsets.UTF_16, StandardCharsets.US_ASCII);
}
