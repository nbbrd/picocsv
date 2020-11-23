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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Philippe Charles
 */
public class CsvParsingTest {

    @Test
    public void testEqualsAndHashcode() {
        assertThat(Csv.Parsing.STRICT)
                .isEqualTo(Csv.Parsing.STRICT)
                .hasSameHashCodeAs(Csv.Parsing.STRICT)
                .isNotEqualTo(Csv.Parsing.LENIENT)
                .isNotEqualTo(strict10)
                .isEqualTo(Csv.Parsing.DEFAULT)
                .isNotEqualTo(null)
                .isNotEqualTo("");
    }

    @Test
    public void testToString() {
        assertThat(Csv.Parsing.STRICT.toString())
                .isEqualTo(Csv.Parsing.STRICT.toString())
                .isNotEqualTo(Csv.Parsing.LENIENT.toString())
                .isNotEqualTo(strict10.toString())
                .isEqualTo(Csv.Parsing.DEFAULT.toString());
    }

    private final Csv.Parsing strict10 = Csv.Parsing.STRICT.toBuilder().maxCharsPerField(10).build();
}
