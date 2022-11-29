package _demo.api1;

import _test.Top5GridMonthly;
import nbbrd.picocsv.Csv;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static _demo.api1.TableReader.byColumnIndex;
import static _demo.api1.TableReader.byColumnName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.atIndex;

public class TableReaderTest {

    @Test
    public void testByColumnName() throws IOException {
        try (Csv.Reader reader = Top5GridMonthly.open()) {
            assertThat(byColumnName("Firefox", "Safari").lines(reader))
                    .hasSize(13)
                    .contains(new String[]{"30,69", "4,09"}, atIndex(0))
                    .contains(new String[]{null, null}, atIndex(6))
                    .contains(new String[]{"28,34", "5,07"}, atIndex(12));
        }

        try (Csv.Reader reader = Top5GridMonthly.open()) {
            assertThat(byColumnName("Safari", "Firefox").lines(reader))
                    .hasSize(13)
                    .contains(new String[]{"4,09", "30,69"}, atIndex(0))
                    .contains(new String[]{null, null}, atIndex(6))
                    .contains(new String[]{"5,07", "28,34"}, atIndex(12));
        }
    }

    @Test
    public void testByColumnIndex() throws IOException {
        try (Csv.Reader reader = Top5GridMonthly.open()) {
            assertThat(byColumnIndex(2, 4).lines(reader))
                    .hasSize(13)
                    .contains(new String[]{"30,69", "4,09"}, atIndex(0))
                    .contains(new String[]{null, null}, atIndex(6))
                    .contains(new String[]{"28,34", "5,07"}, atIndex(12));
        }

        try (Csv.Reader reader = Top5GridMonthly.open()) {
            assertThat(byColumnIndex(4, 2).lines(reader))
                    .hasSize(13)
                    .contains(new String[]{"4,09", "30,69"}, atIndex(0))
                    .contains(new String[]{null, null}, atIndex(6))
                    .contains(new String[]{"5,07", "28,34"}, atIndex(12));
        }
    }
}
