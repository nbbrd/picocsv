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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import org.junit.Test;

/**
 *
 * @author Philippe Charles
 */
public class CsvFormatTest {

    @Test
    public void testFactories() {
        assertThatNullPointerException()
                .isThrownBy(() -> Csv.Format.builder().build())
                .withMessageContaining("separator");
    }

    @Test
    public void testToBuilder() {
        assertThat(Csv.Format.DEFAULT.toBuilder().build())
                .isEqualTo(Csv.Format.DEFAULT);

        assertThat(Csv.Format.EXCEL.toBuilder().build())
                .isEqualTo(Csv.Format.EXCEL);
    }

    @Test
    public void testEqualsAndHashcode() {
        assertThat(Csv.Format.DEFAULT)
                .isEqualTo(Csv.Format.DEFAULT)
                .hasSameHashCodeAs(Csv.Format.DEFAULT)
                .isNotEqualTo(Csv.Format.EXCEL)
                .isNotEqualTo(Csv.Format.DEFAULT.toBuilder().quote('x').build())
                .isNotEqualTo(Csv.Format.DEFAULT.toBuilder().separator(Csv.NewLine.MACINTOSH).build())
                .isNotEqualTo(null)
                .isNotEqualTo("");

        assertThat(Csv.Format.DEFAULT.equals(Csv.Format.DEFAULT)).isTrue();
        assertThat(Csv.Format.DEFAULT.equals(null)).isFalse();
        assertThat(Csv.Format.DEFAULT.equals(Csv.Format.DEFAULT.toBuilder().build())).isTrue();
    }

    @Test
    public void testToString() {
        assertThat(Csv.Format.DEFAULT.toString())
                .isEqualTo(Csv.Format.DEFAULT.toString())
                .isNotEqualTo(Csv.Format.EXCEL.toString());
    }
}
