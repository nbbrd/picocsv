package nbbrd.picocsv;

import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class ExcelCsvTest {

    @Test
    public void testGetFormat() throws IOException {
        assertThat(ExcelCsv.getFormat()).isNotNull();
        assertThat(ExcelCsv.getFormat().isValid()).isTrue();
    }

    @Test
    public void testGetEncoding() {
        assertThat(ExcelCsv.getEncoding()).isNotNull();
    }

    @Test
    public void testGetLocale() {
        assertThat(ExcelCsv.getLocale()).isNotNull();
    }
}
