package _test;

import lombok.EqualsAndHashCode;
import nbbrd.picocsv.Csv;
import org.apache.commons.text.StringEscapeUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class Row {

    private Row() {
    }

    @EqualsAndHashCode(callSuper = false)
    @lombok.Value
    public static class Empty extends Row {

        @Override
        public String toString() {
            return "Empty";
        }
    }

    @EqualsAndHashCode(callSuper = false)
    @lombok.Value
    public static class Comment extends Row {

        @lombok.NonNull
        String comment;

        @Override
        public String toString() {
            return "#" + StringEscapeUtils.escapeJava(comment);
        }
    }

    @EqualsAndHashCode(callSuper = false)
    @lombok.Value
    public static class Fields extends Row {

        public static final Fields EMPTY_ROW = new Fields(Collections.emptyList());
        public static final Fields EMPTY_FIELD = new Fields(Collections.singletonList(""));

        @lombok.NonNull
        List<? extends CharSequence> fields;

        @Override
        public String toString() {
            return "{" + fields.stream().map(Object::toString).map(StringEscapeUtils::escapeJava).collect(Collectors.joining("|")) + "}";
        }
    }

    public static List<Row> readAll(Csv.Reader reader, Consumer<List<Row>> onEmpty, BiConsumer<List<Row>, String> onComment, BiConsumer<List<Row>, List<String>> onFields) throws IOException {
        List<Row> result = new ArrayList<>();
        while (reader.readLine()) {
            if (reader.readField()) {
                if (reader.isComment()) {
                    onComment.accept(result, reader.toString());
                } else {
                    List<String> fields = new ArrayList<>();
                    do {
                        fields.add(reader.toString());
                    } while (reader.readField());
                    onFields.accept(result, fields);
                }
            } else {
                onEmpty.accept(result);
            }
        }
        return result;
    }

    public static void writeAll(List<Row> rows, Csv.Writer writer) throws IOException {
        for (Row row : rows) {
            if (row instanceof Empty) {
                writer.writeEndOfLine();
            } else if (row instanceof Comment) {
                writer.writeComment(((Comment) row).getComment());
            } else if (row instanceof Fields) {
                for (CharSequence field : ((Fields) row).getFields()) {
                    writer.writeField(field);
                }
                writer.writeEndOfLine();
            }
        }
    }

    public static void skipEmpty(List<Row> list) {
    }

    public static void appendEmpty(List<Row> list) {
        list.add(new Empty());
    }

    public static void appendEmptyRow(List<Row> list) {
        list.add(Fields.EMPTY_ROW);
    }

    public static void appendEmptyField(List<Row> list) {
        list.add(Fields.EMPTY_FIELD);
    }

    public static void appendComment(List<Row> list, String comment) {
        list.add(new Comment(comment));
    }

    public static void skipComment(List<Row> list, String comment) {
    }

    public static void skipFields(List<Row> list, List<String> fields) {
    }

    public static void appendFields(List<Row> list, List<String> fields) {
        list.add(new Fields(fields));
    }
}
