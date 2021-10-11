package _demo;

import nbbrd.picocsv.Csv;

import java.io.IOException;

public class Utils {

    public static boolean skipComments(Csv.Reader reader) throws IOException {
        while (reader.readLine()) {
            if (!reader.isComment()) {
                return true;
            }
        }
        return false;
    }

    public static boolean skipLines(Csv.Reader reader, int skipLines) throws IOException {
        for (int i = 0; i < skipLines; i++) {
            if (!reader.readLine()) {
                return false;
            }
        }
        return true;
    }
}
