package _demo.api2;

import _demo.AbstractIterator;
import _demo.Cookbook;
import nbbrd.picocsv.Csv;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.function.IntPredicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@lombok.Value
@lombok.Builder(toBuilder = true, builderClassName = "Builder")
public class CsvRowParser {

    @lombok.Builder.Default
    String separator = Csv.Format.DEFAULT.getSeparator();

    @lombok.Builder.Default
    char delimiter = Csv.Format.DEFAULT.getDelimiter();

    @lombok.Builder.Default
    char quote = Csv.Format.DEFAULT.getQuote();

    @lombok.Builder.Default
    boolean lenientSeparator = Csv.ReaderOptions.DEFAULT.isLenientSeparator();

    @lombok.Builder.Default
    int maxCharsPerField = Csv.ReaderOptions.DEFAULT.getMaxCharsPerField();

    @lombok.Builder.Default
    boolean includeEmptyLines = false;

    @lombok.Builder.Default
    int skipLines = 0;

    @lombok.Builder.Default
    Columns columns = Columns.all(false);

    public Stream<Row> stream(Reader charReader) {
        return stream(charReader, Csv.DEFAULT_CHAR_BUFFER_SIZE);
    }

    public Stream<Row> stream(Reader charReader, int charBufferSize) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterate(charReader, charBufferSize), 0), false);
    }

    public Iterator<Row> iterate(Reader charReader) {
        return iterate(charReader, Csv.DEFAULT_CHAR_BUFFER_SIZE);
    }

    public Iterator<Row> iterate(Reader charReader, int charBufferSize) {
        try {
            return newIterator(toCsvReader(charReader, charBufferSize));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private Csv.Format toFormat() {
        return Csv.Format
                .builder()
                .separator(separator)
                .delimiter(delimiter)
                .quote(quote)
                .build();
    }

    private Csv.ReaderOptions toReaderOptions() {
        return Csv.ReaderOptions
                .builder()
                .maxCharsPerField(maxCharsPerField)
                .lenientSeparator(lenientSeparator)
                .build();
    }

    private Csv.Reader toCsvReader(Reader charReader, int charBufferSize) throws IOException {
        return Csv.Reader.of(toFormat(), toReaderOptions(), charReader, charBufferSize);
    }

    private Iterator<Row> newIterator(Csv.Reader reader) throws IOException {
        if (!Cookbook.skipLines(reader, skipLines)) return Collections.emptyIterator();

        IntPredicate fieldFilter = getFieldFilter(reader, columns);
        if (fieldFilter == null) return Collections.emptyIterator();

        int firstLineNumber = skipLines + (columns.hasHeader() ? 1 : 0);

        return new RowIterator(reader, fieldFilter, includeEmptyLines, firstLineNumber);
    }

    private static IntPredicate getFieldFilter(Csv.Reader reader, Columns selector) throws IOException {
        if (selector.hasHeader()) {
            if (!Cookbook.skipComments(reader)) {
                return null;
            }
            List<String> columnNames = new ArrayList<>();
            while (reader.readField()) {
                columnNames.add(reader.toString());
            }
            return selector.getFilter(columnNames);
        } else {
            return selector.getFilter(Collections.emptyList());
        }
    }

    private static boolean readFields(Csv.Reader reader, boolean includeEmptyLines, List<String> row, IntPredicate fieldFilter) throws IOException {
        int fieldIndex = 0;
        if (reader.readField()) {
            do {
                if (fieldFilter.test(fieldIndex)) {
                    row.add(reader.toString());
                }
                fieldIndex++;
            } while (reader.readField());
            return true;
        } else if (includeEmptyLines) {
            return true;
        }
        return false;
    }

    private static final class RowIterator extends AbstractIterator<Row> {

        private final Csv.Reader reader;
        private final IntPredicate fieldFilter;
        private final boolean includeEmptyLines;
        private List<String> fields;
        private int lineNumber;

        public RowIterator(Csv.Reader reader, IntPredicate fieldFilter, boolean includeEmptyLines, int firstLineNumber) {
            this.reader = reader;
            this.fieldFilter = fieldFilter;
            this.includeEmptyLines = includeEmptyLines;
            this.fields = new ArrayList<>();
            this.lineNumber = firstLineNumber;
        }

        @Override
        protected Row get() {
            return new Row(fields, lineNumber);
        }

        @Override
        protected boolean moveNext() {
            fields = new ArrayList<>();
            lineNumber++;
            try {
                while (Cookbook.skipComments(reader)) {
                    if (readFields(reader, includeEmptyLines, fields, fieldFilter)) return true;
                }
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
            return false;
        }
    }
}
