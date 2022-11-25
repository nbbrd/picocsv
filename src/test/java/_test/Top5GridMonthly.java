package _test;

import nbbrd.picocsv.Csv;

import java.io.*;
import java.nio.charset.Charset;

@lombok.experimental.UtilityClass
public class Top5GridMonthly {

    public static final String SEPARATOR = Csv.Format.WINDOWS_SEPARATOR;
    public static final char DELIMITER = ';';
    public static final char QUOTE = '"';
    public static final boolean LENIENT_SEPARATOR = true;
    public static final int CHAR_BUFFER_SIZE = Csv.DEFAULT_CHAR_BUFFER_SIZE;

    private final static byte[] CONTENT = getResourcesAsBytes(Top5GridMonthly.class, "/Top5-Grid-Monthly.csv");
    private final static Charset ENCODING = Charset.forName("windows-1252");
    private final static Csv.Format FORMAT = Csv.Format.builder().separator(SEPARATOR).delimiter(DELIMITER).quote(QUOTE).build();
    private final static Csv.ReaderOptions OPTIONS = Csv.ReaderOptions.DEFAULT.toBuilder().lenientSeparator(LENIENT_SEPARATOR).build();

    public static Csv.Reader open() throws IOException {
        return Csv.Reader.of(FORMAT, OPTIONS, openStreamReader(), CHAR_BUFFER_SIZE);
    }

    public static InputStreamReader openStreamReader() {
        return new InputStreamReader(new ByteArrayInputStream(CONTENT), ENCODING);
    }

    private static byte[] getResourcesAsBytes(Class<?> anchor, String name) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (InputStream stream = anchor.getResourceAsStream(name)) {
            if (stream == null) {
                throw new IOException("Missing resource: " + name);
            }
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
