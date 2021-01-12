package nbbrd.picocsv;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * CSV produced by Excel is system-dependent. Its configuration is available at runtime.
 * https://superuser.com/questions/606272/how-to-get-excel-to-interpret-the-comma-as-a-default-delimiter-in-csv-files
 */
public final class ExcelCsv {

    private ExcelCsv() {
        // static class
    }

    /**
     * Gets the system-dependent format for Excel.
     *
     * @return a non-null format
     * @throws IOException if an I/O error occurs
     */
    public static Csv.Format getFormat() throws IOException {
        return Csv.Format.RFC4180
                .toBuilder()
                .separator(getSeparator())
                .delimiter(getDelimiter())
                .quote('"')
                .build();
    }

    /**
     * Gets the system-dependent encoding for Excel.
     *
     * @return a non-null encoding
     */
    public static Charset getEncoding() {
        return Charset.defaultCharset();
    }

    /**
     * Gets the system-dependent locale for Excel.
     *
     * @return a non-null locale
     */
    public static Locale getLocale() {
        return Locale.getDefault(Locale.Category.FORMAT);
    }

    private static Csv.NewLine getSeparator() {
        return Csv.NewLine.WINDOWS;
    }

    private static char getDelimiter() throws IOException {
        if (isWindows()) return getDecimalSeparator() == COMMA ? SEMICOLON : getWindowsListSeparator();
        if (isMac()) return getDecimalSeparator() == COMMA ? SEMICOLON : COMMA;
        return COMMA;
    }

    static final boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private static final boolean isMac() {
        return System.getProperty("os.name").toLowerCase().contains("mac");
    }

    private static final char COMMA = ',';
    private static final char SEMICOLON = ';';

    private static char getDecimalSeparator() {
        return new DecimalFormatSymbols(getLocale()).getDecimalSeparator();
    }

    static char getWindowsListSeparator() throws IOException {
        return parseWindowsListSeparator(execToString("reg query \"HKCU\\Control Panel\\International\" /v sList"));
    }

    private static char parseWindowsListSeparator(String result) throws IOException {
        String anchor = "    sList    REG_SZ    ";
        int idx = result.lastIndexOf(anchor);
        if (idx != -1 && result.length() > idx + anchor.length()) {
            return result.charAt(idx + anchor.length());
        }
        throw new IOException("Cannot parse windows list separator: " + result);
    }

    private static String execToString(String command) throws IOException {
        Process process = Runtime.getRuntime().exec(command);
        try (java.io.Reader reader = new InputStreamReader(process.getInputStream(), Charset.defaultCharset())) {
            return readerToString(reader);
        } finally {
            try {
                process.waitFor(1, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                throw new IOException(ex);
            }
        }
    }

    private static String readerToString(java.io.Reader reader) throws IOException {
        StringBuilder result = new StringBuilder();
        char[] buffer = new char[8 * 1024];
        int read = 0;
        while ((read = reader.read(buffer)) != -1) {
            result.append(buffer, 0, read);
        }
        return result.toString();
    }
}
