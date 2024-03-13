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

import _test.Sample;
import org.apache.commons.text.StringEscapeUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * @author Philippe Charles
 */
public class CsvFormatTest {

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void testFactories() {
        assertThatNullPointerException()
                .isThrownBy(() -> Csv.Format.builder().separator(null).build())
                .withMessageContaining("separator");

        assertThat(Csv.Format.builder().separator("ab").build().getSeparator())
                .isEqualTo("ab");

        assertThat(Csv.Format.builder().delimiter('c').build().getDelimiter())
                .isEqualTo('c');

        assertThat(Csv.Format.builder().quote('d').build().getQuote())
                .isEqualTo('d');

        assertThat(Csv.Format.builder().comment('e').build().getComment())
                .isEqualTo('e');

        assertThat(Csv.Format.builder().acceptMissingField(false).build().isAcceptMissingField())
                .isEqualTo(false);
    }

    @Test
    public void testToBuilder() {
        assertThat(Csv.Format.DEFAULT.toBuilder().build())
                .isEqualTo(Csv.Format.DEFAULT);

        assertThat(other.toBuilder().build())
                .isEqualTo(other);
    }

    @Test
    public void testEqualsAndHashcode() {
        assertThat(Csv.Format.DEFAULT)
                .isEqualTo(Csv.Format.DEFAULT)
                .isEqualTo(Csv.Format.DEFAULT.toBuilder().separator("\r\n").build())
                .hasSameHashCodeAs(Csv.Format.DEFAULT)
                .isNotEqualTo(other)
                .isNotEqualTo(Csv.Format.DEFAULT.toBuilder().quote('x').build())
                .isNotEqualTo(Csv.Format.DEFAULT.toBuilder().separator(Csv.Format.MACINTOSH_SEPARATOR).build())
                .isNotEqualTo(Csv.Format.DEFAULT.toBuilder().comment('x').build())
                .isNotEqualTo(Csv.Format.DEFAULT.toBuilder().acceptMissingField(false).build())
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
                .isNotEqualTo(other.toString())
                .isEqualTo("Format(separator=\\r\\n, delimiter=,, quote=\\\", comment=#, acceptMissingField=true)");

        for (char c : Sample.SPECIAL_CHARS) {
            assertThat(Csv.Format.builder().delimiter(c).build().toString())
                    .contains("delimiter=" + StringEscapeUtils.escapeJava(String.valueOf(c)));

            assertThat(Csv.Format.builder().quote(c).build().toString())
                    .contains("quote=" + StringEscapeUtils.escapeJava(String.valueOf(c)));

            assertThat(Csv.Format.builder().comment(c).build().toString())
                    .contains("comment=" + StringEscapeUtils.escapeJava(String.valueOf(c)));
        }
    }

    @Test
    public void testIsValid() {
        assertThat(Csv.Format.DEFAULT.isValid()).isTrue();

        assertThat(Csv.Format.builder().separator("").build().isValid()).isFalse();
        assertThat(Csv.Format.builder().separator("a").build().isValid()).isTrue();
        assertThat(Csv.Format.builder().separator("ab").build().isValid()).isTrue();
        assertThat(Csv.Format.builder().separator("abc").build().isValid()).isFalse();

        assertThat(Csv.Format.builder().quote('a').delimiter('b').build().isValid()).isTrue();
        assertThat(Csv.Format.builder().quote('a').delimiter('a').build().isValid()).isFalse();

        assertThat(Csv.Format.builder().comment('a').delimiter('b').build().isValid()).isTrue();
        assertThat(Csv.Format.builder().comment('a').delimiter('a').build().isValid()).isFalse();
        assertThat(Csv.Format.builder().comment('a').quote('b').build().isValid()).isTrue();
        assertThat(Csv.Format.builder().comment('a').quote('a').build().isValid()).isFalse();

        assertThat(Csv.Format.builder().quote('a').separator("a").build().isValid()).isFalse();
        assertThat(Csv.Format.builder().quote('a').separator("ab").build().isValid()).isFalse();
        assertThat(Csv.Format.builder().quote('a').separator("b").build().isValid()).isTrue();
        assertThat(Csv.Format.builder().quote('a').separator("bc").build().isValid()).isTrue();

        assertThat(Csv.Format.builder().delimiter('a').separator("a").build().isValid()).isFalse();
        assertThat(Csv.Format.builder().delimiter('a').separator("ab").build().isValid()).isFalse();
        assertThat(Csv.Format.builder().delimiter('a').separator("b").build().isValid()).isTrue();
        assertThat(Csv.Format.builder().delimiter('a').separator("bc").build().isValid()).isTrue();

        assertThat(Csv.Format.builder().comment('a').separator("a").build().isValid()).isFalse();
        assertThat(Csv.Format.builder().comment('a').separator("ab").build().isValid()).isFalse();
        assertThat(Csv.Format.builder().comment('a').separator("b").build().isValid()).isTrue();
        assertThat(Csv.Format.builder().comment('a').separator("bc").build().isValid()).isTrue();
    }

    private final Csv.Format other = Csv.Format.DEFAULT.toBuilder().delimiter('\t').build();
}
