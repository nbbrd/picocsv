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
package _demo;

import _test.QuickReader;
import _test.Row;
import _test.Sample;
import _test.fastcsv.FastCsvEntry;
import _test.fastcsv.FastCsvEntryConverter;
import _test.fastcsv.FastCsvEntryRowsParser;
import nbbrd.picocsv.Csv;

import java.io.IOException;
import java.util.List;

/**
 * @author Philippe Charles
 */
public class JavaCsvComparisonDemo {

    public static void main(String[] args) throws IOException {
        List<FastCsvEntry> entries = FastCsvEntry.loadAll();
        printConversionIssues(entries);
        printInvalidReading(entries);
    }

    private static void printInvalidReading(List<FastCsvEntry> entries) throws IOException {
        System.out.println("[InvalidReading]");

        Csv.ReaderOptions lenientSeparator = Csv.ReaderOptions.builder().lenientSeparator(true).build();

        for (FastCsvEntry entry : entries) {
            Sample sample = FastCsvEntryConverter.toSample(entry);
            QuickReader.Parser<List<Row>> parser = new FastCsvEntryRowsParser(entry);

            List<Row> rows = QuickReader.readValue(parser, sample.getContent(), sample.getFormat(), lenientSeparator);
            if (!rows.equals(sample.getRows())) {
                System.out.println("   Input: '" + entry.getInput() + "'");
                System.out.println("   Flags: '" + entry.getFlags() + "'");
                System.out.println("Expected: '" + sample.getRows() + "'");
                System.out.println("   Found: '" + rows + "'");
                System.out.println();
            }
        }
    }

    private static void printConversionIssues(List<FastCsvEntry> entries) {
        System.out.println("[ConversionIssues]");

        entries.stream()
                .filter(JavaCsvComparisonDemo::hasConversionIssue)
                .forEach(original -> {
                    Sample sample = FastCsvEntryConverter.toSample(original);
                    FastCsvEntry derived = FastCsvEntryConverter.fromSample(sample);

                    System.out.println("Original: " + original);
                    System.out.println("  Sample: " + sample);
                    System.out.println(" Derived: " + derived);
                    System.out.println();
                });
    }

    private static boolean hasConversionIssue(FastCsvEntry original) {
        Sample sample = FastCsvEntryConverter.toSample(original);
        FastCsvEntry derived = FastCsvEntryConverter.fromSample(sample);
        return !original.equals(derived);
    }
}
