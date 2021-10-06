package _test.fastcsv;

import _test.QuickReader;
import _test.Row;
import de.siegmar.fastcsv.reader.CommentStrategy;
import nbbrd.picocsv.Csv;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

public final class FastCsvEntryRowsParser implements QuickReader.Parser<List<Row>> {

    private final Consumer<List<Row>> onEmpty;
    private final BiConsumer<List<Row>, List<String>> onFields;

    public FastCsvEntryRowsParser(FastCsvEntry entry) {
        this.onEmpty = getEmptyConsumer(entry.isSkipEmptyLines());
        this.onFields = getNonEmptyConsumer(entry.getCommentStrategy(), ';', ',');
    }

    @Override
    public List<Row> accept(Csv.Reader reader) throws IOException {
        return Row.readAll(reader, onEmpty, Row::appendComment, onFields);
    }

    private static Consumer<List<Row>> getEmptyConsumer(boolean skipEmptyLines) {
        return skipEmptyLines
                ? Row::skipEmpty
                : Row::appendEmptyField;
    }

    private static BiConsumer<List<Row>, List<String>> getNonEmptyConsumer(CommentStrategy strategy, char comment, char delimiter) {
        switch (strategy) {
            case NONE:
                return Row::appendFields;
            case READ:
                return or(getCommentPredicate(comment), getCommentJoiner(delimiter), Row::appendFields);
            case SKIP:
                return or(getCommentPredicate(comment), Row::skipFields, Row::appendFields);
            default:
                throw new RuntimeException();
        }
    }

    private static BiPredicate<List<Row>, List<String>> getCommentPredicate(char comment) {
        return (list, fields) -> isComment(fields, comment);
    }

    private static BiConsumer<List<Row>, List<String>> getCommentJoiner(char delimiter) {
        return (list, fields) -> list.add(joinComment(fields, delimiter));
    }

    private static boolean isComment(List<String> fields, char commentChar) {
        return !fields.isEmpty() && fields.get(0).length() > 0 && fields.get(0).charAt(0) == commentChar;
    }

    private static Row.Fields joinComment(List<String> fields, char delimiter) {
        StringBuilder field = new StringBuilder();
        Iterator<String> iterator = fields.iterator();
        field.append(iterator.next().substring(1));
        while (iterator.hasNext()) {
            field.append(delimiter).append(iterator.next());
        }
        return new Row.Fields(Collections.singletonList(field.toString()));
    }

    private static <T, U> BiConsumer<T, U> or(BiPredicate<T, U> predicate, BiConsumer<T, U> l, BiConsumer<T, U> r) {
        return (T t, U u) -> {
            if (predicate.test(t, u))
                l.accept(t, u);
            else
                r.accept(t, u);
        };
    }
}
