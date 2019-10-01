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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.channels.Channels;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.IntConsumer;

/**
 * Reads CSV files.
 *
 * @author Philippe Charles
 */
public final class CsvReader implements Closeable, CharSequence {

    /**
     * Creates a new instance from a file.
     *
     * @param file a non-null file
     * @param encoding a non-null encoding
     * @param format a non-null format
     * @return a new reader
     * @throws IllegalArgumentException if the format contains an invalid
     * combination of options
     * @throws IOException if an I/O error occurs
     */
    public static CsvReader of(Path file, Charset encoding, CsvFormat format) throws IllegalArgumentException, IOException {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(encoding, "encoding");
        Objects.requireNonNull(format, "format");

        if (!format.isValid()) {
            throw new IllegalArgumentException("format");
        }

        CharsetDecoder decoder = encoding.newDecoder();
        BufferSizes sizes = BufferSizes.of(file, decoder);
        return make(format, sizes.chars, newReader(file, decoder, sizes.bytes));
    }

    /**
     * Creates a new instance from a stream.
     *
     * @param stream a non-null stream
     * @param encoding a non-null encoding
     * @param format a non-null format
     * @return a new reader
     * @throws IllegalArgumentException if the format contains an invalid
     * combination of options
     * @throws IOException if an I/O error occurs
     */
    public static CsvReader of(InputStream stream, Charset encoding, CsvFormat format) throws IllegalArgumentException, IOException {
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
     * Creates a new instance from a reader.
     *
     * @param reader a non-null reader
     * @param format a non-null format
     * @return a new reader
     * @throws IllegalArgumentException if the format contains an invalid
     * combination of options
     * @throws IOException if an I/O error occurs
     */
    public static CsvReader of(Reader reader, CsvFormat format) throws IllegalArgumentException, IOException {
        Objects.requireNonNull(reader, "reader");
        Objects.requireNonNull(format, "format");

        if (!format.isValid()) {
            throw new IllegalArgumentException("format");
        }

        BufferSizes sizes = BufferSizes.EMPTY;
        return make(format, sizes.chars, reader);
    }

    private static CsvReader make(CsvFormat format, OptionalInt charBufferSize, Reader reader) {
        int size = BufferSizes.getSize(charBufferSize, BufferSizes.DEFAULT_CHAR_BUFFER_SIZE);
        return new CsvReader(
                format.getSeparator() == NewLine.WINDOWS ? new ReadAheadInput(reader, size) : new Input(reader, size),
                format.getQuote(), format.getDelimiter(),
                EndOfLineReader.of(format.getSeparator()));
    }

    private final Input input;
    private final int quote;
    private final int delimiter;
    private final EndOfLineReader endOfLine;
    private char[] fieldChars;
    private int fieldLength;
    private State state;
    private boolean parsedByLine;

    private CsvReader(Input input, int quote, int delimiter, EndOfLineReader endOfLine) {
        this.input = input;
        this.quote = quote;
        this.delimiter = delimiter;
        this.endOfLine = endOfLine;
        this.fieldChars = new char[64];
        this.fieldLength = 0;
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
                    return true;
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
        boolean quoted = false;
        if ((val = input.read()) != Input.EOF) {
            if (val == quote) {
                quoted = true;
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

        if (quoted) {
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

        private final Reader reader;
        private final char[] buffer;
        private int length;
        private int index;

        private Input(Reader reader, int bufferSize) {
            this.reader = reader;
            this.buffer = new char[bufferSize];
            this.length = 0;
            this.index = 0;
        }

        @Override
        public void close() throws IOException {
            reader.close();
        }

        public int read() throws IOException {
            if (index < length) {
                return buffer[index++];
            }
            if ((length = reader.read(buffer)) == EOF) {
                return EOF;
            }
            index = 1;
            return buffer[0];
        }
    }

    private static final class ReadAheadInput extends Input {

        private static final int NULL = -2;
        private int readAhead;

        private ReadAheadInput(Reader reader, int bufferSize) {
            super(reader, bufferSize);
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

    private static Reader newReader(Path file, CharsetDecoder decoder, OptionalInt byteBufferSize) throws IOException {
        return Channels.newReader(Files.newByteChannel(file, StandardOpenOption.READ), decoder, byteBufferSize.orElse(-1));
    }
}
