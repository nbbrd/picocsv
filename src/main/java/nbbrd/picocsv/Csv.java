/*
 * Copyright 2019 National Bank of Belgium
 * 
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved 
 * by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * 
 * http://ec.europa.eu/idabc/eupl
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and 
 * limitations under the Licence.
 */
package nbbrd.picocsv;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.channels.Channels;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.IntConsumer;

/**
 *
 * @author Philippe Charles
 */
public final class Csv {

    private Csv() {
        // static class
    }

    /**
     * Character used to signify the end of a line.
     *
     * @see https://en.wikipedia.org/wiki/Newline
     */
    public enum NewLine {

        WINDOWS, UNIX, MACINTOSH;

        static final char CR = '\r';
        static final char LF = '\n';
        static final String CRLF = String.valueOf(new char[]{CR, LF});

        static boolean isNewLine(char c) {
            return c == CR || c == LF;
        }
    }

    /**
     * Specifies the format of a CSV file.
     */
    public static final class Format {

        /**
         * Predefined format as defined by RFC 4180.
         */
        public static final Format RFC4180 = Format
                .builder()
                .separator(NewLine.WINDOWS)
                .delimiter(',')
                .quote('"')
                .build();

        /**
         * Predefined format as alias to RFC 4180.
         */
        public static final Format DEFAULT = RFC4180;

        public static final Format EXCEL = Format
                .builder()
                .separator(NewLine.WINDOWS) // FIXME: ?
                .delimiter(';')
                .quote('"')
                .build();

        private final NewLine separator;
        private final char delimiter;
        private final char quote;

        private Format(NewLine separator, char delimiter, char quote) {
            this.separator = Objects.requireNonNull(separator, "separator");
            this.delimiter = delimiter;
            this.quote = quote;
        }

        /**
         * Character used to separate lines in the input.
         *
         * @return a non-null line separator
         */
        public NewLine getSeparator() {
            return separator;
        }

        /**
         * Delimiting character of the input.
         *
         * @return
         */
        public char getDelimiter() {
            return delimiter;
        }

        /**
         * Character used to quote strings in the input.
         *
         * @return
         */
        public char getQuote() {
            return quote;
        }

        /**
         * Checks if the current format follows theses rules:
         * <ul>
         * <li>delimiter != quote
         * <li>delimiter and quote are not NewLine chars
         * </ul>
         *
         * @return true if valid, false otherwise
         */
        public boolean isValid() {
            return delimiter != quote
                    && !NewLine.isNewLine(delimiter)
                    && !NewLine.isNewLine(quote);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 37 * hash + Objects.hashCode(this.separator);
            hash = 37 * hash + this.delimiter;
            hash = 37 * hash + this.quote;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Format other = (Format) obj;
            if (this.delimiter != other.delimiter) {
                return false;
            }
            if (this.quote != other.quote) {
                return false;
            }
            if (this.separator != other.separator) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "CsvFormat{" + "separator=" + separator + ", delimiter=" + delimiter + ", quote=" + quote + '}';
        }

        public Builder toBuilder() {
            return builder()
                    .separator(separator)
                    .delimiter(delimiter)
                    .quote(quote);
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {

            private NewLine separator;
            private char delimiter;
            private char quote;

            private Builder() {
            }

            public Builder separator(NewLine separator) {
                this.separator = separator;
                return this;
            }

            public Builder delimiter(char delimiter) {
                this.delimiter = delimiter;
                return this;
            }

            public Builder quote(char quote) {
                this.quote = quote;
                return this;
            }

            public Format build() {
                return new Format(separator, delimiter, quote);
            }
        }
    }

    /**
     * Reads CSV files.
     */
    public static final class Reader implements Closeable, CharSequence {

        /**
         * Creates a new instance from a file.
         *
         * @param file a non-null file
         * @param encoding a non-null encoding
         * @param format a non-null format
         * @return a new CSV reader
         * @throws IllegalArgumentException if the format contains an invalid
         * combination of options
         * @throws IOException if an I/O error occurs
         */
        public static Reader of(Path file, Charset encoding, Format format) throws IllegalArgumentException, IOException {
            Objects.requireNonNull(file, "file");
            Objects.requireNonNull(encoding, "encoding");
            Objects.requireNonNull(format, "format");

            if (!format.isValid()) {
                throw new IllegalArgumentException("format");
            }

            CharsetDecoder decoder = encoding.newDecoder();
            BufferSizes sizes = BufferSizes.of(file, decoder);
            return make(format, sizes.chars, newCharReader(file, decoder, sizes.bytes));
        }

        /**
         * Creates a new instance from a stream.
         *
         * @param stream a non-null stream
         * @param encoding a non-null encoding
         * @param format a non-null format
         * @return a new CSV reader
         * @throws IllegalArgumentException if the format contains an invalid
         * combination of options
         * @throws IOException if an I/O error occurs
         */
        public static Reader of(InputStream stream, Charset encoding, Format format) throws IllegalArgumentException, IOException {
            Objects.requireNonNull(stream, "stream");
            Objects.requireNonNull(encoding, "encoding");
            Objects.requireNonNull(format, "format");

            if (!format.isValid()) {
                throw new IllegalArgumentException("format");
            }

            CharsetDecoder decoder = encoding.newDecoder();
            BufferSizes sizes = BufferSizes.of(stream, decoder);
            return make(format, sizes.chars, new InputStreamReader(stream, decoder));
        }

        /**
         * Creates a new instance from a char reader.
         *
         * @param charReader a non-null char reader
         * @param format a non-null format
         * @return a new CSV reader
         * @throws IllegalArgumentException if the format contains an invalid
         * combination of options
         * @throws IOException if an I/O error occurs
         */
        public static Reader of(java.io.Reader charReader, Format format) throws IllegalArgumentException, IOException {
            Objects.requireNonNull(charReader, "charReader");
            Objects.requireNonNull(format, "format");

            if (!format.isValid()) {
                throw new IllegalArgumentException("format");
            }

            BufferSizes sizes = BufferSizes.EMPTY;
            return make(format, sizes.chars, charReader);
        }

        private static Reader make(Format format, OptionalInt charBufferSize, java.io.Reader charReader) {
            int size = BufferSizes.getSize(charBufferSize, BufferSizes.DEFAULT_CHAR_BUFFER_SIZE);
            return new Reader(
                    format.getSeparator() == NewLine.WINDOWS ? new ReadAheadInput(charReader, size) : new Input(charReader, size),
                    format.getQuote(), format.getDelimiter(),
                    EndOfLineReader.of(format.getSeparator()));
        }

        private final Input input;
        private final int quote;
        private final int delimiter;
        private final EndOfLineReader endOfLine;
        private char[] fieldChars;
        private int fieldLength;
        private boolean fieldQuoted;
        private State state;
        private boolean parsedByLine;

        private Reader(Input input, int quote, int delimiter, EndOfLineReader endOfLine) {
            this.input = input;
            this.quote = quote;
            this.delimiter = delimiter;
            this.endOfLine = endOfLine;
            this.fieldChars = new char[64];
            this.fieldLength = 0;
            this.fieldQuoted = false;
            this.state = State.READY;
            this.parsedByLine = false;
        }

        private enum State {
            READY, NOT_LAST, LAST, DONE;
        }

        /**
         * Reads the next line.
         *
         * @return true if not at the end of file
         * @throws IOException if an I/O error occurs
         */
        public boolean readLine() throws IOException {
            switch (state) {
                case DONE:
                    return false;
                case READY:
                case LAST:
                    state = parseNextField(false);
                    parsedByLine = true;
                    return state != State.DONE;
                case NOT_LAST:
                    while ((state = parseNextField(true)) == State.NOT_LAST) {
                    }
                    state = parseNextField(false);
                    parsedByLine = true;
                    return state != State.DONE;
                default:
                    throw new RuntimeException();
            }
        }

        /**
         * Reads the next field.
         *
         * @return true if not at the end of line
         * @throws IOException if an I/O error occurs
         */
        public boolean readField() throws IOException {
            switch (state) {
                case LAST:
                    if (parsedByLine) {
                        parsedByLine = false;
                        return fieldLength > 0 || fieldQuoted;
                    }
                    return false;
                case NOT_LAST:
                    if (parsedByLine) {
                        parsedByLine = false;
                        return true;
                    }
                    state = parseNextField(false);
                    return state != State.DONE;
                case DONE:
                case READY:
                    throw new IllegalStateException();
                default:
                    throw new RuntimeException();
            }
        }

        @Override
        public void close() throws IOException {
            input.close();
        }

        private State parseNextField(boolean skip) throws IOException {
            int val;
            resetField();

            IntConsumer fieldBuilder = skip
                    ? this::swallow
                    : this::append;

            // first char        
            fieldQuoted = false;
            if ((val = input.read()) != Input.EOF) {
                if (val == quote) {
                    fieldQuoted = true;
                } else {
                    if (val == delimiter) {
                        return State.NOT_LAST;
                    }
                    if (endOfLine.isEndOfLine(val, input)) {
                        return State.LAST;
                    }
                    fieldBuilder.accept(val);
                }
            } else {
                return State.DONE;
            }

            if (fieldQuoted) {
                // subsequent chars with escape
                boolean escaped = false;
                while ((val = input.read()) != Input.EOF) {
                    if (val == quote) {
                        if (!escaped) {
                            escaped = true;
                        } else {
                            escaped = false;
                            fieldBuilder.accept(val);
                        }
                        continue;
                    }
                    if (escaped) {
                        if (val == delimiter) {
                            return State.NOT_LAST;
                        }
                        if (endOfLine.isEndOfLine(val, input)) {
                            return State.LAST;
                        }
                    }
                    fieldBuilder.accept(val);
                }
            } else {
                // subsequent chars without escape
                while ((val = input.read()) != Input.EOF) {
                    if (val == delimiter) {
                        return State.NOT_LAST;
                    }
                    if (endOfLine.isEndOfLine(val, input)) {
                        return State.LAST;
                    }
                    fieldBuilder.accept(val);
                }
            }

            return fieldLength > 0 ? State.LAST : State.DONE;
        }

        private void ensureFieldSize() {
            if (fieldLength == fieldChars.length) {
                fieldChars = Arrays.copyOf(fieldChars, fieldLength * 2);
            }
        }

        private void resetField() {
            fieldLength = 0;
        }

        private void swallow(int c) {
            // do nothing
        }

        private void append(int c) {
            ensureFieldSize();
            fieldChars[fieldLength++] = (char) c;
        }

        @Override
        public String toString() {
            return fieldLength == 0 ? "" : new String(fieldChars, 0, fieldLength);
        }

        @Override
        public int length() {
            return fieldLength;
        }

        @Override
        public char charAt(int index) {
            return fieldChars[index];
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return new String(fieldChars, start, end - start);
        }

        private static class Input implements Closeable {

            public static final int EOF = -1;

            private final java.io.Reader charReader;
            private final char[] buffer;
            private int length;
            private int index;

            private Input(java.io.Reader charReader, int bufferSize) {
                this.charReader = charReader;
                this.buffer = new char[bufferSize];
                this.length = 0;
                this.index = 0;
            }

            @Override
            public void close() throws IOException {
                charReader.close();
            }

            public int read() throws IOException {
                if (index < length) {
                    return buffer[index++];
                }
                if ((length = charReader.read(buffer)) == EOF) {
                    return EOF;
                }
                index = 1;
                return buffer[0];
            }
        }

        private static final class ReadAheadInput extends Input {

            private static final int NULL = -2;
            private int readAhead;

            private ReadAheadInput(java.io.Reader charReader, int bufferSize) {
                super(charReader, bufferSize);
                this.readAhead = NULL;
            }

            @Override
            public int read() throws IOException {
                if (readAhead == NULL) {
                    return super.read();
                }
                int result = readAhead;
                readAhead = NULL;
                return result;
            }

            public boolean peek(int expected) throws IOException {
                return (readAhead = super.read()) == expected;
            }

            public void discardAheadOfTimeChar() throws IOException {
                readAhead = NULL;
            }
        }

        @FunctionalInterface
        private interface EndOfLineReader {

            boolean isEndOfLine(int c, Input input) throws IOException;

            static EndOfLineReader of(NewLine newLine) {
                switch (newLine) {
                    case MACINTOSH:
                        return (c, input) -> c == NewLine.CR;
                    case UNIX:
                        return (c, input) -> c == NewLine.LF;
                    case WINDOWS:
                        return (c, input) -> {
                            if (c == NewLine.CR && ((ReadAheadInput) input).peek(NewLine.LF)) {
                                ((ReadAheadInput) input).discardAheadOfTimeChar();
                                return true;
                            }
                            return false;
                        };
                    default:
                        throw new RuntimeException();
                }
            }
        }

        private static java.io.Reader newCharReader(Path file, CharsetDecoder decoder, OptionalInt byteBufferSize) throws IOException {
            return Channels.newReader(Files.newByteChannel(file, StandardOpenOption.READ), decoder, byteBufferSize.orElse(-1));
        }
    }

    /**
     * Writes CSV files.
     */
    public static final class Writer implements Closeable {

        /**
         * Creates a new instance from a file.
         *
         * @param file a non-null file
         * @param encoding a non-null encoding
         * @param format a non-null format
         * @return a new CSV writer
         * @throws IllegalArgumentException if the format contains an invalid
         * combination of options
         * @throws IOException if an I/O error occurs
         */
        public static Writer of(Path file, Charset encoding, Format format) throws IOException {
            Objects.requireNonNull(file, "file");
            Objects.requireNonNull(encoding, "encoding");
            Objects.requireNonNull(format, "format");

            if (!format.isValid()) {
                throw new IllegalArgumentException("format");
            }

            CharsetEncoder encoder = encoding.newEncoder();
            BufferSizes sizes = BufferSizes.of(file, encoder);
            return make(format, sizes.chars, newCharWriter(file, encoder, sizes.bytes));
        }

        /**
         * Creates a new instance from a stream.
         *
         * @param stream a non-null stream
         * @param encoding a non-null encoding
         * @param format a non-null format
         * @return a new CSV writer
         * @throws IllegalArgumentException if the format contains an invalid
         * combination of options
         * @throws IOException if an I/O error occurs
         */
        public static Writer of(OutputStream stream, Charset encoding, Format format) throws IOException {
            Objects.requireNonNull(stream, "stream");
            Objects.requireNonNull(encoding, "encoding");
            Objects.requireNonNull(format, "format");

            if (!format.isValid()) {
                throw new IllegalArgumentException("format");
            }

            CharsetEncoder encoder = encoding.newEncoder();
            BufferSizes sizes = BufferSizes.of(stream, encoder);
            return make(format, sizes.chars, new OutputStreamWriter(stream, encoder));
        }

        /**
         * Creates a new instance from a char writer.
         *
         * @param charWriter a non-null char writer
         * @param format a non-null format
         * @return a new CSV writer
         * @throws IllegalArgumentException if the format contains an invalid
         * combination of options
         * @throws IOException if an I/O error occurs
         */
        public static Writer of(java.io.Writer charWriter, Format format) throws IOException {
            Objects.requireNonNull(charWriter, "charWriter");
            Objects.requireNonNull(format, "format");

            if (!format.isValid()) {
                throw new IllegalArgumentException("format");
            }

            BufferSizes sizes = BufferSizes.EMPTY;
            return make(format, sizes.chars, charWriter);
        }

        private static Writer make(Format format, OptionalInt charBufferSize, java.io.Writer charWriter) {
            int size = BufferSizes.getSize(charBufferSize, BufferSizes.DEFAULT_CHAR_BUFFER_SIZE);
            return new Writer(
                    new Output(charWriter, size),
                    format.getQuote(), format.getDelimiter(),
                    EndOfLineWriter.of(format.getSeparator())
            );
        }

        private final Output output;
        private final char quote;
        private final char delimiter;
        private final EndOfLineWriter endOfLine;
        private State state;

        private Writer(Output output, char quote, char delimiter, EndOfLineWriter endOfLine) {
            this.output = output;
            this.quote = quote;
            this.delimiter = delimiter;
            this.endOfLine = endOfLine;
            this.state = State.NO_FIELD;
        }

        /**
         * Writes a new field. Null field is handled as empty.
         *
         * @param field a nullable field
         * @throws IOException if an I/O error occurs
         */
        public void writeField(CharSequence field) throws IOException {
            switch (state) {
                case NO_FIELD:
                    if (!isNullOrEmpty(field)) {
                        state = State.MULTI_FIELD;
                        writeNonEmptyField(field);
                    } else {
                        state = State.SINGLE_EMPTY_FIELD;
                    }
                    break;
                case SINGLE_EMPTY_FIELD:
                    state = State.MULTI_FIELD;
                    output.write(delimiter);
                    if (!isNullOrEmpty(field)) {
                        writeNonEmptyField(field);
                    }
                    break;
                case MULTI_FIELD:
                    output.write(delimiter);
                    if (!isNullOrEmpty(field)) {
                        writeNonEmptyField(field);
                    }
                    break;
            }
        }

        /**
         * Writes an end of line.
         *
         * @throws IOException if an I/O error occurs
         */
        public void writeEndOfLine() throws IOException {
            flushField();
            endOfLine.write(output);
        }

        @Override
        public void close() throws IOException {
            flushField();
            output.close();
        }

        private boolean isNullOrEmpty(CharSequence field) {
            return field == null || field.length() == 0;
        }

        private void writeNonEmptyField(CharSequence field) throws IOException {
            switch (getQuoting(field)) {
                case NONE:
                    output.write(field);
                    break;
                case PARTIAL:
                    output.write(quote);
                    output.write(field);
                    output.write(quote);
                    break;
                case FULL:
                    output.write(quote);
                    for (int i = 0; i < field.length(); i++) {
                        char c = field.charAt(i);
                        if (c == quote) {
                            output.write(c);
                        }
                        output.write(c);
                    }
                    output.write(quote);
                    break;
            }
        }

        private void flushField() throws IOException {
            if (state == State.SINGLE_EMPTY_FIELD) {
                output.write(quote);
                output.write(quote);
            }
            state = State.NO_FIELD;
        }

        private Quoting getQuoting(CharSequence field) {
            Quoting result = Quoting.NONE;
            for (int i = 0; i < field.length(); i++) {
                char c = field.charAt(i);
                if (c == quote) {
                    return Quoting.FULL;
                }
                if (c == delimiter || NewLine.isNewLine(c)) {
                    result = Quoting.PARTIAL;
                }
            }
            return result;
        }

        private enum Quoting {
            NONE, PARTIAL, FULL
        }

        private enum State {
            NO_FIELD, SINGLE_EMPTY_FIELD, MULTI_FIELD
        }

        private static final class Output implements Closeable {

            private final java.io.Writer charWriter;
            private final char[] buffer;
            private int length;

            private Output(java.io.Writer charWriter, int bufferSize) {
                this.charWriter = charWriter;
                this.buffer = new char[bufferSize];
                this.length = 0;
            }

            public void write(char c) throws IOException {
                if (length == buffer.length) {
                    flush();
                }
                buffer[length++] = c;
            }

            public void write(CharSequence chars) throws IOException {
                int charsLength = chars.length();
                if (length + charsLength >= buffer.length) {
                    flush();
                    if (charsLength >= buffer.length) {
                        charWriter.append(chars);
                        return;
                    }
                }
                if (chars instanceof String) {
                    ((String) chars).getChars(0, charsLength, buffer, length);
                    length += charsLength;
                } else {
                    for (int i = 0; i < charsLength; i++) {
                        buffer[length++] = chars.charAt(i);
                    }
                }
            }

            @Override
            public void close() throws IOException {
                flush();
                charWriter.close();
            }

            private void flush() throws IOException {
                charWriter.write(buffer, 0, length);
                length = 0;
            }
        }

        @FunctionalInterface
        private interface EndOfLineWriter {

            void write(Output stream) throws IOException;

            static EndOfLineWriter of(NewLine newLine) {
                switch (newLine) {
                    case MACINTOSH:
                        return stream -> stream.write(NewLine.CR);
                    case UNIX:
                        return stream -> stream.write(NewLine.LF);
                    case WINDOWS:
                        return stream -> stream.write(NewLine.CRLF);
                    default:
                        throw new RuntimeException();
                }
            }
        }

        private static java.io.Writer newCharWriter(Path file, CharsetEncoder encoder, OptionalInt byteBufferSize) throws IOException {
            return Channels.newWriter(Files.newByteChannel(file, StandardOpenOption.WRITE), encoder, byteBufferSize.orElse(-1));
        }
    }

    static final class BufferSizes {

        static final int DEFAULT_CHAR_BUFFER_SIZE = 8192;
        static final int DEFAULT_BLOCK_BUFFER_SIZE = 512;
        static final int DEFAULT_BUFFER_OUTPUT_STREAM_SIZE = 8192;

        static final BufferSizes EMPTY = new BufferSizes(OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty());

        static BufferSizes of(Path file, CharsetDecoder decoder) throws IOException {
            return make(getBlockSize(file), decoder.averageCharsPerByte());
        }

        static BufferSizes of(Path file, CharsetEncoder encoder) throws IOException {
            return make(getBlockSize(file), 1f / encoder.averageBytesPerChar());
        }

        static BufferSizes of(InputStream stream, CharsetDecoder decoder) throws IOException {
            return make(getBlockSize(stream), decoder.averageCharsPerByte());
        }

        static BufferSizes of(OutputStream stream, CharsetEncoder encoder) throws IOException {
            Objects.requireNonNull(stream);
            return make(getBlockSize(stream), 1f / encoder.averageBytesPerChar());
        }

        private static BufferSizes make(OptionalInt byteBlockSize, float averageCharsPerByte) {
            if (!byteBlockSize.isPresent()) {
                return EMPTY;
            }
            int bytes = getByteBufferSize(byteBlockSize.getAsInt());
            return new BufferSizes(byteBlockSize, OptionalInt.of(bytes), OptionalInt.of((int) (bytes * averageCharsPerByte)));
        }

        final OptionalInt block;
        final OptionalInt bytes;
        final OptionalInt chars;

        BufferSizes(OptionalInt block, OptionalInt bytes, OptionalInt chars) {
            this.block = block;
            this.bytes = bytes;
            this.chars = chars;
        }

        private static int getByteBufferSize(int byteBlockSize) {
            int tmp = getNextHighestPowerOfTwo(byteBlockSize);
            return tmp == byteBlockSize ? byteBlockSize * 64 : byteBlockSize;
        }

        private static int getNextHighestPowerOfTwo(int val) {
            val = val - 1;
            val |= val >> 1;
            val |= val >> 2;
            val |= val >> 4;
            val |= val >> 8;
            val |= val >> 16;
            return val + 1;
        }

        private static OptionalInt getBlockSize(Path file) throws IOException {
            Objects.requireNonNull(file);
            // FIXME: JDK10 -> https://docs.oracle.com/javase/10/docs/api/java/nio/file/FileStore.html#getBlockSize()
            return OptionalInt.of(DEFAULT_BLOCK_BUFFER_SIZE);
        }

        private static OptionalInt getBlockSize(InputStream stream) throws IOException {
            int result = stream.available();
            return result > 0 ? OptionalInt.of(result) : OptionalInt.empty();
        }

        private static OptionalInt getBlockSize(OutputStream stream) throws IOException {
            if (stream instanceof BufferedOutputStream) {
                return OptionalInt.of(DEFAULT_BUFFER_OUTPUT_STREAM_SIZE);
            }
            return OptionalInt.empty();
        }

        static int getSize(OptionalInt value, int defaultValue) {
            if (value.isPresent()) {
                int result = value.getAsInt();
                return result > 0 ? result : defaultValue;
            }
            return defaultValue;
        }
    }
}
