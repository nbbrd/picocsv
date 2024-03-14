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

import nbbrd.picocsv.Csv;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Philippe Charles
 */
public class EmbeddedCSVDemo {

    public static void main(String[] args) throws IOException {

        Csv.Format mainFormat = Csv.Format.DEFAULT;
        Csv.Format embeddedFormat = Csv.Format.builder().delimiter('=').separator(",").build();

        StringWriter mainString = new StringWriter();
        try (Csv.Writer main = Csv.Writer.of(mainFormat, Csv.WriterOptions.DEFAULT, mainString)) {
            main.writeField("NAME");
            main.writeField("PROPERTIES");
            main.writeEndOfLine();

            for (Record record : Record.getData()) {
                main.writeField(record.getName());
                main.writeField(getEmbeddedCSV(embeddedFormat, record.properties));
                main.writeEndOfLine();
            }
        }
        System.out.println(mainString);
    }

    private static String getEmbeddedCSV(Csv.Format embeddedFormat, Map<String, String> properties) throws IOException {
        StringWriter embeddedString = new StringWriter();
        try (Csv.Writer embedded = Csv.Writer.of(embeddedFormat, Csv.WriterOptions.DEFAULT, embeddedString)) {
            Cookbook.writeRecords(embedded, properties);
        }
        return embeddedString.toString();
    }

    @lombok.Value
    @lombok.Builder
    public static class Record {

        String name;

        @lombok.Singular
        Map<String, String> properties;

        static List<Record> getData() {
            List<Record> result = new ArrayList<>();
            result.add(Record.builder().name("name1")
                    .property("key1", "value1")
                    .build());
            result.add(Record.builder().name("name2")
                    .property("key2", "value2")
                    .property("key3", "value3")
                    .build());
            return result;
        }
    }
}
