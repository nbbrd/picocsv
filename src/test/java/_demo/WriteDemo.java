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

/**
 * @author Philippe Charles
 */
public class WriteDemo {

    public static void main(String[] args) throws IOException {
        StringWriter result = new StringWriter();
        try (Csv.Writer writer = Csv.Writer.of(Csv.Format.DEFAULT, Csv.WriterOptions.DEFAULT, result)) {
            writer.writeComment("This is a comment");
            writer.writeField("hello");
            writer.writeField("wo\"rld");
            writer.writeEndOfLine();
            writer.writeField("test");
        }
        System.out.println(result);
    }
}
