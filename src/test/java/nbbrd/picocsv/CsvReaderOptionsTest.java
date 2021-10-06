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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Philippe Charles
 */
public class CsvReaderOptionsTest {

    @Test
    public void testEqualsAndHashcode() {
        assertThat(strict)
                .isEqualTo(strict)
                .hasSameHashCodeAs(strict)
                .isNotEqualTo(lenient)
                .isNotEqualTo(strict10)
                .isEqualTo(strict.toBuilder().build())
                .isNotEqualTo(null)
                .isNotEqualTo("");

        assertThat(strict.equals(strict)).isTrue();
        assertThat(strict.equals(null)).isFalse();
        assertThat(strict.equals(strict.toBuilder().build())).isTrue();
    }

    @Test
    public void testToString() {
        assertThat(strict.toString())
                .isEqualTo(strict.toString())
                .isNotEqualTo(lenient.toString())
                .isNotEqualTo(strict10.toString())
                .isEqualTo(strict.toBuilder().build().toString())
                .isNotEqualTo(null)
                .isNotEqualTo("")
                .isEqualTo("ReaderOptions(lenientSeparator=false, maxCharsPerField=4096)");
    }

    @Test
    public void testBuilder() {
        assertThat(Csv.ReaderOptions.builder().build())
                .isEqualTo(Csv.ReaderOptions.DEFAULT)
                .isEqualTo(Csv.ReaderOptions.DEFAULT.toBuilder().build());
    }

    private final Csv.ReaderOptions strict = Csv.ReaderOptions.DEFAULT;
    private final Csv.ReaderOptions lenient = Csv.ReaderOptions.DEFAULT.toBuilder().lenientSeparator(true).build();
    private final Csv.ReaderOptions strict10 = Csv.ReaderOptions.DEFAULT.toBuilder().maxCharsPerField(10).build();
}
