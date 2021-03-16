package _test.fastcsv;

import _test.QuickReader;
import _test.Row;
import de.siegmar.fastcsv.reader.CommentStrategy;
import nbbrd.picocsv.Csv;

import java.io.IOException;
import java.util.List;

public final class FastCsvEntryRowsParser implements QuickReader.Parser<List<Row>> {

    private final Row.EmptyConsumer onEmpty;
    private final Row.NonEmptyConsumer onNonEmpty;

    public FastCsvEntryRowsParser(FastCsvEntry entry) {
        this.onEmpty = getEmptyConsumer(entry.isSkipEmptyLines());
        this.onNonEmpty = getNonEmptyConsumer(entry.getCommentStrategy(), ';', ',');
    }

    @Override
    public List<Row> accept(Csv.Reader reader) throws IOException {
        return Row.readAll(reader, onEmpty, onNonEmpty);
    }

    private static Row.EmptyConsumer getEmptyConsumer(boolean skipEmptyLines) {
        return skipEmptyLines
                ? Row.EmptyConsumer.noOp()
                : Row.EmptyConsumer.constant(Row.EMPTY_FIELD);
    }

    private static Row.NonEmptyConsumer getNonEmptyConsumer(CommentStrategy strategy, char comment, char delimiter) {
        switch (strategy) {
            case NONE:
                return (reader, list) -> list.add(Row.read(reader));
            case READ:
                return (reader, list) -> {
                    if (isComment(reader, comment)) {
                        list.add(joinComment(reader, delimiter));
                    } else {
                        list.add(Row.read(reader));
                    }
                };
            case SKIP:
                return (reader, list) -> {
                    if (!isComment(reader, comment)) {
                        list.add(Row.read(reader));
                    }
                };
            default:
                throw new RuntimeException();
        }
    }

    private static boolean isComment(CharSequence text, char commentChar) {
        return text.length() > 0 && text.charAt(0) == commentChar;
    }

    private static Row joinComment(Csv.Reader reader, char delimiter) throws IOException {
        StringBuilder row = new StringBuilder();
        row.append(reader, 1, reader.length());
        while (reader.readField()) {
            row.append(delimiter).append(reader);
        }
        return Row.of(row.toString());
    }
}
