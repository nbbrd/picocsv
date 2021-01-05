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
import java.util.Objects;

/**
 * @author Philippe Charles
 */
public final class Csv {

    private Csv() {
        // static class
    }

    /**
     * Character used to signify the end of a line.
     *
     * @see <a href="https://en.wikipedia.org/wiki/Newline">https://en.wikipedia.org/wiki/Newline</a>
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
         * Predefined format as alias to {@link Format#RFC4180}.
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
         * Character used to separate lines.
         *
         * @return a non-null line separator
         */
        public NewLine getSeparator() {
            return separator;
        }

        /**
         * Character used to delimit the values.
         *
         * @return the delimiting character
         */
        public char getDelimiter() {
            return delimiter;
        }

        /**
         * Character used to encapsulate values containing special characters.
         *
         * @return the quoting character
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
            return "Format{" + "separator=" + separator + ", delimiter=" + delimiter + ", quote=" + quote + '}';
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
     * Specifies the reader options.
     */
    public static final class Parsing {

        /**
         * Default parsing options.
         */
        public static final Parsing DEFAULT = Parsing.builder().build();

        private final Format format;
        private final boolean lenientSeparator;
        private final int maxCharsPerField;

        private Parsing(Format format, boolean lenientSeparator, int maxCharsPerField) {
            this.format = format;
            this.lenientSeparator = lenientSeparator;
            this.maxCharsPerField = maxCharsPerField;
        }

        /**
         * Determines the CSV format used by this reader.
         *
         * @return a non-null format
         */
        public Format getFormat() {
            return format;
        }

        /**
         * Determines if the separator is parsed leniently or not. If set to
         * true, the reader follows the same behavior as BufferedReader: <i>a
         * line is considered to be terminated by any one of a line feed ('\n'),
         * a carriage return ('\r'), a carriage return followed immediately by a
         * line feed, or by reaching the end-of-file (EOF)</i>.
         *
         * @return true if lenient parsing of separator, false otherwise
         */
        public boolean isLenientSeparator() {
            return lenientSeparator;
        }

        /**
         * Determines the maximum number of characters to read in each field
         * to avoid {@link java.lang.OutOfMemoryError} in case a file does not
         * have a valid format. This sets a limit which avoids unwanted JVM crashes.
         * The default value is 4096.
         *
         * @return the maximum number of characters for a field
         */
        public int getMaxCharsPerField() {
            return maxCharsPerField;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 37 * hash + format.hashCode();
            hash = 37 * hash + hashCodeOf(lenientSeparator);
            hash = 37 * hash + this.maxCharsPerField;
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
            final Parsing other = (Parsing) obj;
            if (!this.format.equals(other.format)) {
                return false;
            }
            if (this.lenientSeparator != other.lenientSeparator) {
                return false;
            }
            if (this.maxCharsPerField != other.maxCharsPerField) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "Parsing{" + "format=" + format + ", lenientSeparator=" + lenientSeparator + ", maxCharsPerField=" + maxCharsPerField + '}';
        }

        public Builder toBuilder() {
            return builder()
                    .format(format)
                    .lenientSeparator(lenientSeparator)
                    .maxCharsPerField(maxCharsPerField);
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {

            private static final boolean DEFAULT_LENIENT_SEPARATOR = false;
            private static final int DEFAULT_MAX_CHARS_PER_FIELD = 4096;

            private Format format = Format.DEFAULT;
            private boolean lenientSeparator = DEFAULT_LENIENT_SEPARATOR;
            private int maxCharsPerField = DEFAULT_MAX_CHARS_PER_FIELD;

            private Builder() {
            }

            public Builder format(Format format) {
                this.format = Objects.requireNonNull(format);
                return this;
            }

            public Builder lenientSeparator(boolean lenientSeparator) {
                this.lenientSeparator = lenientSeparator;
                return this;
            }

            public Builder maxCharsPerField(int maxCharsPerField) {
                this.maxCharsPerField = maxCharsPerField;
                return this;
            }

            public Parsing build() {
                return new Parsing(format, lenientSeparator, maxCharsPerField);
            }
        }
    }

    /**
     * Reads CSV files.
     */
    public static final class Reader implements Closeable, CharSequence {

        /**
         * Creates a new instance from a char reader.
         *
         * @param charReader a non-null char reader
         * @param options    non-null options
         * @return a new CSV reader
         * @throws IllegalArgumentException if the format contains an invalid
         *                                  combination of options
         * @throws IOException              if an I/O error occurs
         */
        public static Reader of(java.io.Reader charReader, int charBufferSize, Parsing options) throws IllegalArgumentException, IOException {
            Objects.requireNonNull(charReader, "charReader");
            Objects.requireNonNull(options, "options");
            requireValidCharBufferSize(charBufferSize, "charBufferSize");
            requireValidFormat(options.getFormat(), "format");

            return new Reader(
                    ReadAheadInput.isNeeded(options) ? new ReadAheadInput(charReader, charBufferSize) : new Input(charReader, charBufferSize),
                    options.getFormat().getQuote(), options.getFormat().getDelimiter(),
                    EndOfLineReader.of(options),
                    options.getMaxCharsPerField());
        }

        private final Input input;
        private final int quoteCode;
        private final int delimiterCode;
        private final EndOfLineReader endOfLine;
        private final char[] fieldChars;
        private int fieldLength;
        private boolean fieldQuoted;
        private State state;
        private boolean parsedByLine;

        private Reader(Input input, int quoteCode, int delimiterCode, EndOfLineReader endOfLine, int maxCharsPerField) {
            this.input = input;
            this.quoteCode = quoteCode;
            this.delimiterCode = delimiterCode;
            this.endOfLine = endOfLine;
            this.fieldChars = new char[maxCharsPerField];
            this.fieldLength = 0;
            this.fieldQuoted = false;
            this.state = State.READY;
            this.parsedByLine = false;
        }

        private enum State {
            READY, NOT_LAST, LAST, DONE
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
                    state = parseNextField();
                    parsedByLine = true;
                    return state != State.DONE;
                case NOT_LAST:
                    skipRemainingFields();
                    state = parseNextField();
                    parsedByLine = true;
                    return state != State.DONE;
                default:
                    throw new RuntimeException("Unreachable");
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
                        return isFieldNotNull();
                    }
                    return false;
                case NOT_LAST:
                    if (parsedByLine) {
                        parsedByLine = false;
                        return true;
                    }
                    state = parseNextField();
                    return state != State.DONE;
                case DONE:
                case READY:
                    throw new IllegalStateException();
                default:
                    throw new RuntimeException("Unreachable");
            }
        }

        @Override
        public void close() throws IOException {
            input.close();
        }

        private void skipRemainingFields() throws IOException {
            while (true) {
                if ((state = parseNextField()) != State.NOT_LAST) break;
            }
        }

        // WARNING: main loop; lots of duplication to maximize perfs
        // WARNING: comparing ints more performant than comparing chars
        private State parseNextField() throws IOException {
            fieldLength = 0;
            int code;

            try {

                // [Step 1]: first char
                fieldQuoted = false;
                if (/*-next-*/ (code = input.read()) != Input.EOF_CODE) {
                    if (code == quoteCode) {
                        fieldQuoted = true;
                    } else {
                        /*-end-of-field-*/
                        if (code == delimiterCode) return State.NOT_LAST;
                        else if (endOfLine.isEndOfLine(code, input)) return State.LAST;
                        /*-append-*/
                        fieldChars[fieldLength++] = (char) code;
                    }
                } else {
                    // EOF
                    return State.DONE;
                }

                if (fieldQuoted) {
                    // [Step 2A]: subsequent chars with escape
                    boolean escaped = false;
                    while (/*-next-*/ (code = input.read()) != Input.EOF_CODE) {
                        if (code == quoteCode) {
                            if (!escaped) {
                                escaped = true;
                            } else {
                                escaped = false;
                                /*-append-*/
                                fieldChars[fieldLength++] = (char) code;
                            }
                        } else {
                            if (escaped) {
                                /*-end-of-field-*/
                                if (code == delimiterCode) return State.NOT_LAST;
                                else if (endOfLine.isEndOfLine(code, input)) return State.LAST;
                            }
                            /*-append-*/
                            fieldChars[fieldLength++] = (char) code;
                        }
                    }
                    // EOF
                    return State.LAST;
                } else {
                    // [Step 2B]: subsequent chars without escape
                    while (/*-next-*/ (code = input.read()) != Input.EOF_CODE) {
                        /*-end-of-field-*/
                        if (code == delimiterCode) return State.NOT_LAST;
                        else if (endOfLine.isEndOfLine(code, input)) return State.LAST;
                        /*-append-*/
                        fieldChars[fieldLength++] = (char) code;
                    }
                    // EOF
                    return fieldLength > 0 ? State.LAST : State.DONE;
                }

            } catch (IndexOutOfBoundsException ex) {
                throw new IOException("Field overflow", ex);
            }
        }

        private boolean isFieldNotNull() {
            return fieldLength > 0 || fieldQuoted;
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
            if (index >= fieldLength) {
                throw new IndexOutOfBoundsException(String.valueOf(index));
            }
            return fieldChars[index];
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            if (end > fieldLength) {
                throw new IndexOutOfBoundsException(String.valueOf(end));
            }
            return new String(fieldChars, start, end - start);
        }

        private static class Input implements Closeable {

            public static final int EOF_CODE = -1;

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
                if ((length = charReader.read(buffer)) == EOF_CODE) {
                    return EOF_CODE;
                }
                index = 1;
                return buffer[0];
            }
        }

        private static final class ReadAheadInput extends Input {

            static boolean isNeeded(Parsing options) {
                return options.isLenientSeparator() || options.getFormat().getSeparator() == NewLine.WINDOWS;
            }

            private static final int NULL_CODE = -2;
            private int readAheadCode;

            private ReadAheadInput(java.io.Reader charReader, int bufferSize) {
                super(charReader, bufferSize);
                this.readAheadCode = NULL_CODE;
            }

            @Override
            public int read() throws IOException {
                if (readAheadCode == NULL_CODE) {
                    return super.read();
                }
                int result = readAheadCode;
                readAheadCode = NULL_CODE;
                return result;
            }

            public boolean peek(int expected) throws IOException {
                return (readAheadCode = super.read()) == expected;
            }

            public void discardAheadOfTimeCode() {
                readAheadCode = NULL_CODE;
            }
        }

        private enum EndOfLineReader {

            LENIENT {
                @Override
                boolean isEndOfLine(int code, Input input) throws IOException {
                    switch (code) {
                        case LF_CODE:
                            return true;
                        case CR_CODE:
                            if (((ReadAheadInput) input).peek(LF_CODE)) {
                                ((ReadAheadInput) input).discardAheadOfTimeCode();
                            }
                            return true;
                        default:
                            return false;
                    }
                }
            },
            MACINTOSH {
                @Override
                boolean isEndOfLine(int code, Input input) {
                    return code == CR_CODE;
                }
            },
            UNIX {
                @Override
                boolean isEndOfLine(int code, Input input) {
                    return code == LF_CODE;
                }
            },
            WINDOWS {
                @Override
                boolean isEndOfLine(int code, Input input) throws IOException {
                    if (code == CR_CODE && ((ReadAheadInput) input).peek(LF_CODE)) {
                        ((ReadAheadInput) input).discardAheadOfTimeCode();
                        return true;
                    }
                    return false;
                }
            };

            abstract boolean isEndOfLine(int code, Input input) throws IOException;

            static final int CR_CODE = NewLine.CR;
            static final int LF_CODE = NewLine.LF;

            static EndOfLineReader of(Parsing options) {
                if (options.isLenientSeparator()) {
                    return LENIENT;
                }
                switch (options.getFormat().getSeparator()) {
                    case MACINTOSH:
                        return MACINTOSH;
                    case UNIX:
                        return UNIX;
                    case WINDOWS:
                        return WINDOWS;
                    default:
                        throw new RuntimeException("Unreachable");
                }
            }
        }
    }

    /**
     * Specifies the writer options.
     */
    public static final class Formatting {

        /**
         * Default formatting options.
         */
        public static final Formatting DEFAULT = Formatting.builder().build();

        private final Format format;

        private Formatting(Format format) {
            this.format = format;
        }

        /**
         * Determines the CSV format used by this writer.
         *
         * @return a non-null format
         */
        public Format getFormat() {
            return format;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 37 * hash + format.hashCode();
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
            final Parsing other = (Parsing) obj;
            if (!this.format.equals(other.format)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "Parsing{" + "format=" + format + '}';
        }

        public Builder toBuilder() {
            return builder()
                    .format(format);
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {

            private Format format = Format.DEFAULT;

            private Builder() {
            }

            public Builder format(Format format) {
                this.format = Objects.requireNonNull(format);
                return this;
            }

            public Formatting build() {
                return new Formatting(format);
            }
        }
    }

    /**
     * Writes CSV files.
     */
    public static final class Writer implements Closeable {

        /**
         * Creates a new instance from a char writer.
         *
         * @param charWriter a non-null char writer
         * @param options    a non-null options
         * @return a new CSV writer
         * @throws IllegalArgumentException if the format contains an invalid
         *                                  combination of options
         * @throws IOException              if an I/O error occurs
         */
        public static Writer of(java.io.Writer charWriter, int charBufferSize, Formatting options) throws IllegalArgumentException, IOException {
            Objects.requireNonNull(charWriter, "charWriter");
            Objects.requireNonNull(options, "options");
            requireValidCharBufferSize(charBufferSize, "charBufferSize");
            requireValidFormat(options.getFormat(), "format");

            return new Writer(
                    new Output(charWriter, charBufferSize),
                    options.getFormat().getQuote(), options.getFormat().getDelimiter(),
                    EndOfLineWriter.of(options)
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
                    if (isNotEmpty(field)) {
                        state = State.MULTI_FIELD;
                        writeNonEmptyField(field);
                    } else {
                        state = State.SINGLE_EMPTY_FIELD;
                    }
                    break;
                case SINGLE_EMPTY_FIELD:
                    state = State.MULTI_FIELD;
                    output.write(delimiter);
                    if (isNotEmpty(field)) {
                        writeNonEmptyField(field);
                    }
                    break;
                case MULTI_FIELD:
                    output.write(delimiter);
                    if (isNotEmpty(field)) {
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

        private boolean isNotEmpty(CharSequence field) {
            return field != null && field.length() != 0;
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

        private enum EndOfLineWriter {

            MACINTOSH {
                @Override
                void write(Output output) throws IOException {
                    output.write(NewLine.CR);
                }
            },
            UNIX {
                @Override
                void write(Output output) throws IOException {
                    output.write(NewLine.LF);
                }
            },
            WINDOWS {
                @Override
                void write(Output output) throws IOException {
                    output.write(NewLine.CRLF);
                }
            };

            abstract void write(Output output) throws IOException;

            static EndOfLineWriter of(Formatting options) {
                switch (options.getFormat().getSeparator()) {
                    case MACINTOSH:
                        return MACINTOSH;
                    case UNIX:
                        return UNIX;
                    case WINDOWS:
                        return WINDOWS;
                    default:
                        throw new RuntimeException("Unreachable");
                }
            }
        }
    }

    private static void requireValidCharBufferSize(int charBufferSize, String message) throws IllegalArgumentException {
        if (charBufferSize <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requireValidFormat(Format format, String message) throws IllegalArgumentException {
        if (!format.isValid()) {
            throw new IllegalArgumentException(message);
        }
    }

    // JDK8
    private static int hashCodeOf(boolean value) {
        return value ? 1231 : 1237;
    }

    public static final int DEFAULT_CHAR_BUFFER_SIZE = 8192;
}
