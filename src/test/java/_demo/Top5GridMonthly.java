package _demo;

import nbbrd.picocsv.Csv;

import java.io.*;
import java.nio.charset.Charset;

@lombok.experimental.UtilityClass
class Top5GridMonthly {

    private final static byte[] CONTENT = getResourcesAsBytes(Top5GridMonthly.class, "/Top5-Grid-Monthly.csv");
    private final static Charset ENCODING = Charset.forName("windows-1252");
    private final static Csv.Format FORMAT = Csv.Format.builder().separator(Csv.NewLine.WINDOWS).delimiter(';').quote('"').build();
    private final static Csv.Parsing OPTIONS = Csv.Parsing.DEFAULT.toBuilder().lenientSeparator(true).build();

    static Csv.Reader open() throws IOException {
        return Csv.Reader.of(FORMAT, OPTIONS, new InputStreamReader(new ByteArrayInputStream(CONTENT), ENCODING));
    }

    private static byte[] getResourcesAsBytes(Class<?> anchor, String name) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (InputStream stream = anchor.getResourceAsStream(name)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = stream.read(buffer, 0, buffer.length)) >= 0) {
                bytes.write(buffer, 0, read);
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        return bytes.toByteArray();
    }
}
