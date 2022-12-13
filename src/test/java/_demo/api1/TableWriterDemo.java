package _demo.api1;

import lombok.NonNull;
import nbbrd.picocsv.Csv.Format;
import nbbrd.picocsv.Csv.Writer;
import nbbrd.picocsv.Csv.WriterOptions;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Comparator;
import java.util.stream.Stream;

public class TableWriterDemo {

    public static void main(String[] args) throws IOException {
        demo(TableWriter.ofBean(Property.class));

        demo(TableWriter.builder(Property.class)
                .column(TableWriter.Column.of("Key", Property::getKey))
                .column(TableWriter.Column.of("Value", Property::getValue))
                .build()
        );
    }

    private static void demo(TableWriter<Property> tableWriter) throws IOException {
        System.out.println("--- " + "demo" + " ---");
        StringWriter charWriter = new StringWriter();
        try (Writer csv = Writer.of(Format.DEFAULT, WriterOptions.DEFAULT, charWriter)) {
            tableWriter.lines(csv,
                    Property.getSystemProperties()
                            .filter(property -> property.key.startsWith("java"))
                            .sorted(Comparator.comparing(Property::getKey))
                            .limit(5)
            );
        }
        System.out.println(charWriter);
    }

    @lombok.Value
    static class Property {

        public @NonNull String key;
        public @NonNull String value;

        static Stream<Property> getSystemProperties() {
            return System.getProperties()
                    .entrySet()
                    .stream()
                    .map(entry -> new Property(entry.getKey().toString(), entry.getValue().toString()));
        }
    }
}
