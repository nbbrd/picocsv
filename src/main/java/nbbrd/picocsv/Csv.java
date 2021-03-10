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
     * Specifies the format of a CSV file.
     */
    public static final class Format {

        private static final char CR = '\r';
        private static final char LF = '\n';

        public static final String WINDOWS_SEPARATOR = String.valueOf(new char[]{CR, LF});
        public static final String UNIX_SEPARATOR = String.valueOf(LF);
        public static final String MACINTOSH_SEPARATOR = String.valueOf(CR);

        /**
         * @param c
         * @return
         * @see <a href="https://en.wikipedia.org/wiki/Newline">https://en.wikipedia.org/wiki/Newline</a>
         */
        private static boolean isNewLine(char c) {
            return c == CR || c == LF;
        }

        /**
         * Predefined format as defined by RFC 4180.
         */
        public static final Format RFC4180 = new Format(WINDOWS_SEPARATOR, ',', '"');

        /**
         * Predefined format as alias to {@link Format#RFC4180}.
         */
        public static final Format DEFAULT = RFC4180;

        private final String separator;
        private final char delimiter;
        private final char quote;

        private Format(String separator, char delimiter, char quote) {
            this.separator = Objects.requireNonNull(separator, "separator");
            this.delimiter = delimiter;
            this.quote = quote;
        }

        /**
         * Characters used to separate lines.
         *
         * @return a non-null line separator
         */
        public String getSeparator() {
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
         * <li>Min separator length is 1
         * <li>Max separator length is 2
         * <li>delimiter != quote != separator[0]
         * <li>delimiter != NewLine chars
         * <li>quote != NewLine chars
         * </ul>
         *
         * @return true if valid, false otherwise
         */
        public boolean isValid() {
            return !separator.isEmpty()
                    && separator.length() <= 2
                    && delimiter != quote
                    && delimiter != separator.charAt(0)
                    && quote != separator.charAt(0)
                    && !isNewLine(delimiter)
                    && !isNewLine(quote);
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
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            final Format other = (Format) obj;
            if (this.separator != other.separator) return false;
            if (this.delimiter != other.delimiter) return false;
            if (this.quote != other.quote) return false;
            return true;
        }

        @Override
        public String toString() {
            return "Format{" + "separator=" + separator + ", delimiter=" + delimiter + ", quote=" + quote + '}';
        }

        public Builder toBuilder() {
            return new Builder()
                    .separator(separator)
                    .delimiter(delimiter)
                    .quote(quote);
        }

        public static Builder builder() {
            return DEFAULT.toBuilder();
        }

        public static final class Builder {

            private String separator;
            private char delimiter;
            private char quote;

            private Builder() {
            }

            public Builder separator(String separator) {
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

        private static final boolean DEFAULT_LENIENT_SEPARATOR = false;
        private static final int DEFAULT_MAX_CHARS_PER_FIELD = 4096;

        /**
         * Default parsing options.
         */
        public static final Parsing DEFAULT = new Parsing(DEFAULT_LENIENT_SEPARATOR, DEFAULT_MAX_CHARS_PER_FIELD);

        private final boolean lenientSeparator;
        private final int maxCharsPerField;

        private Parsing(boolean lenientSeparator, int maxCharsPerField) {
            this.lenientSeparator = lenientSeparator;
            this.maxCharsPerField = maxCharsPerField;
        }

        /**
         * Determines if the separator is parsed leniently or not. If set to
         * true, the reader follows the same behavior as BufferedReader: <i>a
         * line is considered to be terminated by any one of a line feed ('\n'),
         * a carriage return ('\r'), a carriage return followed immediately by a
         * line feed, or by reaching the end-of-file (EOF)</i>.
         * The default value is false.
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

        /**
         * Checks if the current options follows theses rules:
         * <ul>
         * <li>maximum number of characters for a field must be positive
         * </ul>
         *
         * @return true if valid, false otherwise
         */
        public boolean isValid() {
            return maxCharsPerField > 0;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 37 * hash + hashCodeOf(lenientSeparator);
            hash = 37 * hash + this.maxCharsPerField;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            final Parsing other = (Parsing) obj;
            if (this.lenientSeparator != other.lenientSeparator) return false;
            if (this.maxCharsPerField != other.maxCharsPerField) return false;
            return true;
        }

        @Override
        public String toString() {
            return "Parsing{" + "lenientSeparator=" + lenientSeparator + ", maxCharsPerField=" + maxCharsPerField + '}';
        }

        public Builder toBuilder() {
            return new Builder()
                    .lenientSeparator(lenientSeparator)
                    .maxCharsPerField(maxCharsPerField);
        }

        public static Builder builder() {
            return DEFAULT.toBuilder();
        }

        public static final class Builder {

            private boolean lenientSeparator;
            private int maxCharsPerField;

            private Builder() {
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
                return new Parsing(lenientSeparator, maxCharsPerField);
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
         * @param format     a non-null format
         * @param options    a non-null options
         * @param charReader a non-null char reader
         * @return a new CSV reader
         * @throws IllegalArgumentException if the format contains an invalid
         *                                  combination of options
         * @throws IOException              if an I/O error occurs
         */
        public static Reader of(Format format, Parsing options, java.io.Reader charReader) throws IllegalArgumentException, IOException {
            return of(format, options, charReader, DEFAULT_CHAR_BUFFER_SIZE);
        }

        /**
         * Creates a new instance from a char reader.
         *
         * @param format     a non-null format
         * @param options    a non-null options
         * @param charReader a non-null char reader
         * @return a new CSV reader
         * @throws IllegalArgumentException if the format contains an invalid
         *                                  combination of options
         * @throws IOException              if an I/O error occurs
         */
        public static Reader of(Format format, Parsing options, java.io.Reader charReader, int charBufferSize) throws IllegalArgumentException, IOException {
            Objects.requireNonNull(format, "format");
            Objects.requireNonNull(options, "options");
            Objects.requireNonNull(charReader, "charReader");
            requireArgument(charBufferSize > 0, "charBufferSize");
            requireArgument(format.isValid(), "format must be valid");
            requireArgument(options.isValid(), "options must be valid");

            return new Reader(
                    ReadAheadInput.isNeeded(format, options) ? new ReadAheadInput(charReader, charBufferSize) : new Input(charReader, charBufferSize),
                    format.getQuote(), format.getDelimiter(),
                    EndOfLineDecoder.of(format, options),
                    options.getMaxCharsPerField());
        }

        private final Input input;
        private final int quoteCode;
        private final int delimiterCode;
        private final EndOfLineDecoder endOfLine;
        private final char[] fieldChars;
        private int fieldLength;
        private boolean fieldQuoted;
        private State state;
        private boolean parsedByLine;

        private Reader(Input input, int quoteCode, int delimiterCode, EndOfLineDecoder endOfLine, int maxCharsPerField) {
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
                    parseNextField();
                    parsedByLine = true;
                    return state != State.DONE;
                case NOT_LAST:
                    skipRemainingFields();
                    parseNextField();
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
                    parseNextField();
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
            do {
                parseNextField();
            } while (state == State.NOT_LAST);
        }

        // WARNING: main loop; lots of duplication to maximize perfs
        // WARNING: comparing ints more performant than comparing chars
        private void parseNextField() throws IOException {
            int code;

            try {
                fieldLength = 0;

                // [Step 1]: first char
                fieldQuoted = false;
                if (/*-next-*/ (code = input.read()) != Input.EOF_CODE) {
                    if (code == quoteCode) {
                        fieldQuoted = true;
                    } else {
                        /*-end-of-field-*/
                        if (code == delimiterCode) {
                            state = State.NOT_LAST;
                            return;
                        } else if (endOfLine.isEndOfLine(code, input)) {
                            state = State.LAST;
                            return;
                        }
                        /*-append-*/
                        fieldChars[fieldLength++] = (char) code;
                    }
                } else {
                    // EOF
                    {
                        state = State.DONE;
                        return;
                    }
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
                                if (code == delimiterCode) {
                                    state = State.NOT_LAST;
                                    return;
                                } else if (endOfLine.isEndOfLine(code, input)) {
                                    state = State.LAST;
                                    return;
                                }
                            }
                            /*-append-*/
                            fieldChars[fieldLength++] = (char) code;
                        }
                    }
                    // EOF
                    {
                        state = State.LAST;
                        return;
                    }
                } else {
                    // [Step 2B]: subsequent chars without escape
                    while (/*-next-*/ (code = input.read()) != Input.EOF_CODE) {
                        /*-end-of-field-*/
                        if (code == delimiterCode) {
                            state = State.NOT_LAST;
                            return;
                        } else if (endOfLine.isEndOfLine(code, input)) {
                            state = State.LAST;
                            return;
                        }
                        /*-append-*/
                        fieldChars[fieldLength++] = (char) code;
                    }
                    // EOF
                    {
                        state = fieldLength > 0 ? State.LAST : State.DONE;
                        return;
                    }
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

            static boolean isNeeded(Format format, Parsing options) {
                return options.isLenientSeparator() || format.getSeparator().length() > 1;
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

        private static abstract class EndOfLineDecoder {

            abstract public boolean isEndOfLine(int code, Input input) throws IOException;

            public static EndOfLineDecoder of(Format format, Parsing options) {
                String eol = format.getSeparator();
                switch (eol.length()) {
                    case 1:
                        return new SingleDecoder(eol.charAt(0));
                    case 2:
                        return options.isLenientSeparator()
                                ? new LenientDecoder(eol.charAt(0), eol.charAt(1))
                                : new DualDecoder(eol.charAt(0), eol.charAt(1));
                    default:
                        throw new RuntimeException("Unreachable");
                }
            }

            private static final class SingleDecoder extends EndOfLineDecoder {

                private final int single;

                private SingleDecoder(int single) {
                    this.single = single;
                }

                @Override
                public boolean isEndOfLine(int code, Input input) {
                    return code == single;
                }
            }

            private static final class DualDecoder extends EndOfLineDecoder {

                private final int first;
                private final int second;

                private DualDecoder(int first, int second) {
                    this.first = first;
                    this.second = second;
                }

                @Override
                public boolean isEndOfLine(int code, Input input) throws IOException {
                    if (code == first && ((ReadAheadInput) input).peek(second)) {
                        ((ReadAheadInput) input).discardAheadOfTimeCode();
                        return true;
                    }
                    return false;
                }
            }

            private static final class LenientDecoder extends EndOfLineDecoder {

                private final int first;
                private final int second;

                private LenientDecoder(int first, int second) {
                    this.first = first;
                    this.second = second;
                }

                @Override
                public boolean isEndOfLine(int code, Input input) throws IOException {
                    if (code == first) {
                        if (((ReadAheadInput) input).peek(second)) {
                            ((ReadAheadInput) input).discardAheadOfTimeCode();
                        }
                        return true;
                    }
                    return code == second;
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
        public static final Formatting DEFAULT = new Formatting();

        private Formatting() {
        }

        @Override
        public int hashCode() {
            int hash = 7;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            final Formatting other = (Formatting) obj;
            return true;
        }

        @Override
        public String toString() {
            return "Formatting{" + '}';
        }

        public Builder toBuilder() {
            return new Builder();
        }

        public static Builder builder() {
            return DEFAULT.toBuilder();
        }

        public static final class Builder {

            private Builder() {
            }

            public Formatting build() {
                return new Formatting();
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
         * @param format     a non-null format
         * @param options    a non-null options
         * @param charWriter a non-null char writer
         * @return a new CSV writer
         * @throws IllegalArgumentException if the format contains an invalid
         *                                  combination of options
         * @throws IOException              if an I/O error occurs
         */
        public static Writer of(Format format, Formatting options, java.io.Writer charWriter) throws IllegalArgumentException, IOException {
            return of(format, options, charWriter, DEFAULT_CHAR_BUFFER_SIZE);
        }

        /**
         * Creates a new instance from a char writer.
         *
         * @param format     a non-null format
         * @param options    a non-null options
         * @param charWriter a non-null char writer
         * @return a new CSV writer
         * @throws IllegalArgumentException if the format contains an invalid
         *                                  combination of options
         * @throws IOException              if an I/O error occurs
         */
        public static Writer of(Format format, Formatting options, java.io.Writer charWriter, int charBufferSize) throws IllegalArgumentException, IOException {
            Objects.requireNonNull(format, "format");
            Objects.requireNonNull(options, "options");
            Objects.requireNonNull(charWriter, "charWriter");
            requireArgument(charBufferSize > 0, "charBufferSize");
            requireArgument(format.isValid(), "format must be valid");

            return new Writer(
                    new Output(charWriter, charBufferSize),
                    format.getQuote(), format.getDelimiter(),
                    EndOfLineEncoder.of(format)
            );
        }

        private final Output output;
        private final char quote;
        private final char delimiter;
        private final EndOfLineEncoder endOfLine;
        private State state;

        private Writer(Output output, char quote, char delimiter, EndOfLineEncoder endOfLine) {
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
                if (c == delimiter || Format.isNewLine(c)) {
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

        private static abstract class EndOfLineEncoder {

            abstract public void write(Output output) throws IOException;

            public static EndOfLineEncoder of(Format format) {
                String eol = format.getSeparator();
                switch (eol.length()) {
                    case 1:
                        return new SingleEncoder(eol.charAt(0));
                    case 2:
                        return new DualEncoder(eol.charAt(0), eol.charAt(1));
                    default:
                        throw new RuntimeException("Unreachable");
                }
            }

            private static final class SingleEncoder extends EndOfLineEncoder {

                private final char single;

                private SingleEncoder(char single) {
                    this.single = single;
                }

                @Override
                public void write(Output output) throws IOException {
                    output.write(single);
                }
            }

            private static final class DualEncoder extends EndOfLineEncoder {

                private final char first;
                private final char second;

                private DualEncoder(char first, char second) {
                    this.first = first;
                    this.second = second;
                }

                @Override
                public void write(Output output) throws IOException {
                    output.write(first);
                    output.write(second);
                }
            }
        }
    }

    private static void requireArgument(boolean condition, String message) throws IllegalArgumentException {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    // JDK8
    private static int hashCodeOf(boolean value) {
        return value ? 1231 : 1237;
    }

    public static final int DEFAULT_CHAR_BUFFER_SIZE = 8192;
}
