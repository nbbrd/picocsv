package nbbrd.picocsv;

import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.assertj.core.api.Assumptions.assumeThat;

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

    @Test
    public void testGetWindowsListSeparator() {
        assumeThat(ExcelCsv.isWindows()).isTrue();
        assertThatCode(() -> ExcelCsv.getWindowsListSeparator())
                .doesNotThrowAnyException();
    }
}
