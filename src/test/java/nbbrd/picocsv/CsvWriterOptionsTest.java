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
public class CsvWriterOptionsTest {

    @Test
    public void testEqualsAndHashcode() {
        assertThat(auto)
                .isEqualTo(auto)
                .hasSameHashCodeAs(auto)
                .isEqualTo(auto.toBuilder().build())
                .isNotEqualTo(null)
                .isNotEqualTo("");

        assertThat(auto.equals(auto)).isTrue();
        assertThat(auto.equals(null)).isFalse();
        assertThat(auto.equals(auto.toBuilder().build())).isTrue();
    }

    @Test
    public void testToString() {
        assertThat(auto.toString())
                .isEqualTo(auto.toString())
                .isEqualTo(auto.toBuilder().build().toString())
                .isNotEqualTo(null)
                .isNotEqualTo("")
                .isEqualTo("WriterOptions()");
    }

    @Test
    public void testBuilder() {
        assertThat(Csv.WriterOptions.builder().build())
                .isEqualTo(Csv.WriterOptions.DEFAULT)
                .isEqualTo(Csv.WriterOptions.DEFAULT.toBuilder().build());
    }

    private final Csv.WriterOptions auto = Csv.WriterOptions.DEFAULT;
}
