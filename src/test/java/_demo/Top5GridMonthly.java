package _demo;

import nbbrd.picocsv.Csv;

import java.io.*;
import java.nio.charset.Charset;

@lombok.experimental.UtilityClass
class Top5GridMonthly {

    private final static byte[] content;

    static {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (InputStream stream = Top5GridMonthly.class.getResourceAsStream("/Top5-Grid-Monthly.csv")) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = stream.read(buffer, 0, buffer.length)) >= 0) {
                bytes.write(buffer, 0, read);
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        content = bytes.toByteArray();
    }

    static Csv.Reader open() throws IOException {
        return Csv.Reader.of(new ByteArrayInputStream(content), Charset.forName("windows-1252"), Csv.Format.EXCEL, Csv.Parsing.LENIENT);
    }
}
