package _demo;

import nbbrd.picocsv.Csv;

import java.io.IOException;
import java.nio.CharBuffer;

public class ReadableDemo {

    public static void main(String[] args) throws IOException {
        CharBuffer readable = CharBuffer.wrap("hello,world");
        try (Csv.Reader reader = Csv.Reader.of(Csv.Format.RFC4180, Csv.ReaderOptions.DEFAULT, Cookbook.asCharReader(readable))) {
            while (reader.readLine()) {
                while (reader.readField()) {
                    System.out.println(reader);
                }
            }
        }
    }
}
