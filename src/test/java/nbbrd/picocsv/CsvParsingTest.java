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
        assertThat(Csv.Parsing.DEFAULT)
                .isEqualTo(Csv.Parsing.DEFAULT)
                .hasSameHashCodeAs(Csv.Parsing.DEFAULT)
                .isNotEqualTo(Csv.Parsing.DEFAULT.toBuilder().lenientSeparator(true).build())
                .isNotEqualTo(strict10)
                .isEqualTo(Csv.Parsing.DEFAULT.toBuilder().build())
                .isNotEqualTo(null)
                .isNotEqualTo("");

        assertThat(Csv.Parsing.DEFAULT.equals(Csv.Parsing.DEFAULT)).isTrue();
        assertThat(Csv.Parsing.DEFAULT.equals(null)).isFalse();
        assertThat(Csv.Parsing.DEFAULT.equals(Csv.Parsing.DEFAULT.toBuilder().build())).isTrue();
    }

    @Test
    public void testToString() {
        assertThat(Csv.Parsing.DEFAULT.toString())
                .isEqualTo(Csv.Parsing.DEFAULT.toString())
                .isNotEqualTo(Csv.Parsing.DEFAULT.toBuilder().lenientSeparator(true).build().toString())
                .isNotEqualTo(strict10.toString())
                .isEqualTo(Csv.Parsing.DEFAULT.toString());
    }

    private final Csv.Parsing strict10 = Csv.Parsing.DEFAULT.toBuilder().maxCharsPerField(10).build();
}
