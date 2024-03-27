package _demo;

import nbbrd.picocsv.Csv;

import java.io.IOException;

public class AppendableDemo {

    public static void main(String[] args) throws IOException {
        StringBuilder appendable = new StringBuilder();
        try (Csv.Writer writer = Csv.Writer.of(Csv.Format.RFC4180, Csv.WriterOptions.DEFAULT, Cookbook.asCharWriter(appendable))) {
            writer.writeField("hello");
            writer.writeField("world");
        }
        System.out.println(appendable);
    }
}
