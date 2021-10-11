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
 * Lightweight CSV library for Java.
 *
 * @author Philippe Charles
 */
public final class Csv {

    private Csv() {
        // static class
    }

    public static final int DEFAULT_CHAR_BUFFER_SIZE = 8192;

    /**
     * CSV format.
     * <p>
     * This format is used both by reader and writer
     * but is independent of the source of data (stream or files).
     * Therefore, it doesn't deal with encoding.
     */
    public static final class Format {

        public static final String WINDOWS_SEPARATOR = "\r\n";
        public static final String UNIX_SEPARATOR = "\n";
        public static final String MACINTOSH_SEPARATOR = "\r";

        private static final String DEFAULT_SEPARATOR = WINDOWS_SEPARATOR;
        private static final char DEFAULT_DELIMITER = ',';
        private static final char DEFAULT_QUOTE = '"';
        private static final char DEFAULT_COMMENT = '#';

        /**
         * Predefined format as defined by <a href="https://tools.ietf.org/html/rfc4180">RFC 4180</a>.
         */
        public static final Format RFC4180 = new Format(DEFAULT_SEPARATOR, DEFAULT_DELIMITER, DEFAULT_QUOTE, DEFAULT_COMMENT);

        /**
         * Predefined format as alias to {@link Format#RFC4180}.
         */
        public static final Format DEFAULT = RFC4180;

        private final String separator;
        private final char delimiter;
        private final char quote;
        private final char comment;

        private Format(String separator, char delimiter, char quote, char comment) {
            this.separator = Objects.requireNonNull(separator, "separator");
            this.delimiter = delimiter;
            this.quote = quote;
            this.comment = comment;
        }

        /**
         * Characters used to separate lines.
         * <p>
         * The default value is "\r\n".
         *
         * @return a non-null line separator
         * @see <a href="https://en.wikipedia.org/wiki/Newline">Newline</a>
         */
        public String getSeparator() {
            return separator;
        }

        /**
         * Character used to delimit the values.
         * <p>
         * The default value is ','.
         *
         * @return the delimiting character
         */
        public char getDelimiter() {
            return delimiter;
        }

        /**
         * Character used to encapsulate values containing special characters.
         * <p>
         * The default value is <code>"</code>.
         *
         * @return the quoting character
         */
        public char getQuote() {
            return quote;
        }

        /**
         * Character used to comment lines.
         * <p>
         * The default value is <code>#</code>.
         *
         * @return the comment character
         */
        public char getComment() {
            return comment;
        }

        /**
         * Checks if the current format is valid.
         * <p>
         * Validation rules:
         * <ul>
         * <li>Separator has one or two chars
         * <li>delimiter != quote != separator chars
         * </ul>
         *
         * @return true if valid, false otherwise
         */
        public boolean isValid() {
            return hasValidSize(separator)
                    && delimiter != quote
                    && comment != delimiter
                    && comment != quote
                    && doesNotContain(separator, delimiter)
                    && doesNotContain(separator, quote)
                    && doesNotContain(separator, comment);
        }

        private static boolean hasValidSize(String text) {
            int length = text.length();
            return 1 <= length && length < 3;
        }

        private static boolean doesNotContain(String text, char c) {
            return text.indexOf(c) == -1;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 37 * hash + Objects.hashCode(this.separator);
            hash = 37 * hash + this.delimiter;
            hash = 37 * hash + this.quote;
            hash = 37 * hash + this.comment;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            final Format other = (Format) obj;
            if (!this.separator.equals(other.separator)) return false;
            if (this.delimiter != other.delimiter) return false;
            if (this.quote != other.quote) return false;
            if (this.comment != other.comment) return false;
            return true;
        }

        @Override
        public String toString() {
            return "Format("
                    + "separator=" + prettyPrint(separator)
                    + ", delimiter=" + prettyPrint(delimiter)
                    + ", quote=" + prettyPrint(quote)
                    + ", comment=" + prettyPrint(comment)
                    + ')';
        }

        public Builder toBuilder() {
            return new Builder()
                    .separator(separator)
                    .delimiter(delimiter)
                    .quote(quote)
                    .comment(comment);
        }

        public static Builder builder() {
            return DEFAULT.toBuilder();
        }

        /**
         * CSV format builder.
         */
        public static final class Builder {

            private String separator;
            private char delimiter;
            private char quote;
            private char comment;

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

            public Builder comment(char comment) {
                this.comment = comment;
                return this;
            }

            public Format build() {
                return new Format(separator, delimiter, quote, comment);
            }
        }
    }

    /**
     * CSV reader options.
     */
    public static final class ReaderOptions {

        private static final boolean DEFAULT_LENIENT_SEPARATOR = false;
        private static final int DEFAULT_MAX_CHARS_PER_FIELD = 4096;

        /**
         * Default reader options.
         */
        public static final ReaderOptions DEFAULT = new ReaderOptions(DEFAULT_LENIENT_SEPARATOR, DEFAULT_MAX_CHARS_PER_FIELD);

        private final boolean lenientSeparator;
        private final int maxCharsPerField;

        private ReaderOptions(boolean lenientSeparator, int maxCharsPerField) {
            this.lenientSeparator = lenientSeparator;
            this.maxCharsPerField = maxCharsPerField;
        }

        /**
         * Determines if the {@link Format#getSeparator() format separator} is parsed leniently or not
         * when dealing with dual characters (first-char + second-char) such as {@link Format#WINDOWS_SEPARATOR}.
         * <p>
         * A lenient parsing considers a line to be terminated either:
         * <ul>
         *     <li>if the read char is the first-char possibly followed immediately by the second-char</li>
         *     <li>if the read char is the second-char</li>
         *     <li>if the end-of-file is reached</li>
         * </ul>
         * <p>
         * For example, if the format separator is {@link Format#WINDOWS_SEPARATOR},
         * the reader follows the same behavior as BufferedReader: <i>"a
         * line is considered to be terminated by any one of a line feed ('\n'),
         * a carriage return ('\r'), a carriage return followed immediately by a
         * line feed, or by reaching the end-of-file (EOF)"</i>.
         * <p>
         * The default value is {@value ReaderOptions#DEFAULT_LENIENT_SEPARATOR}.
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
         * <p>
         * The default value is {@value ReaderOptions#DEFAULT_MAX_CHARS_PER_FIELD}.
         *
         * @return the maximum number of characters for a field
         */
        public int getMaxCharsPerField() {
            return maxCharsPerField;
        }

        /**
         * Checks if the current options are valid.
         * <p>
         * Validation rules:
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
            final ReaderOptions other = (ReaderOptions) obj;
            if (this.lenientSeparator != other.lenientSeparator) return false;
            if (this.maxCharsPerField != other.maxCharsPerField) return false;
            return true;
        }

        @Override
        public String toString() {
            return "ReaderOptions("
                    + "lenientSeparator=" + lenientSeparator
                    + ", maxCharsPerField=" + maxCharsPerField
                    + ')';
        }

        public Builder toBuilder() {
            return new Builder()
                    .lenientSeparator(lenientSeparator)
                    .maxCharsPerField(maxCharsPerField);
        }

        public static Builder builder() {
            return DEFAULT.toBuilder();
        }

        /**
         * CSV reader options builder.
         */
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

            public ReaderOptions build() {
                return new ReaderOptions(lenientSeparator, maxCharsPerField);
            }
        }
    }

    /**
     * CSV reader.
     */
    public static final class Reader implements Closeable, CharSequence {

        /**
         * Creates a new instance from a char reader.
         *
         * @param format         a non-null format
         * @param options        a non-null options
         * @param charReader     a non-null char reader
         * @param charBufferSize the size of the internal char buffer
         * @return a new CSV reader
         * @throws IllegalArgumentException if the format contains an invalid
         *                                  combination of options
         * @throws IOException              if an I/O error occurs
         */
        public static Reader of(Format format, ReaderOptions options, java.io.Reader charReader, int charBufferSize) throws IllegalArgumentException, IOException {
            Objects.requireNonNull(format, "format");
            Objects.requireNonNull(options, "options");
            Objects.requireNonNull(charReader, "charReader");
            requireArgument(charBufferSize > 0, "Invalid charBufferSize: %s", charBufferSize);
            requireArgument(format.isValid(), "Invalid format: %s", format);
            requireArgument(options.isValid(), "Invalid options: %s", options);

            char[] charBuffer = new char[charBufferSize];

            return new Reader(
                    ReadAheadInput.isNeeded(format, options) ? new ReadAheadInput(charReader, charBuffer) : new Input(charReader, charBuffer),
                    format.getQuote(), format.getDelimiter(), format.getComment(),
                    EndOfLineDecoder.of(format, options),
                    new char[options.getMaxCharsPerField()]);
        }

        private final Input input;
        private final int quoteCode;
        private final int delimiterCode;
        private final int commentCode;
        private final EndOfLineDecoder eolDecoder;
        private final char[] fieldChars;

        private int fieldLength = 0;
        private int fieldType = FIELD_TYPE_NORMAL;
        private int state = STATE_READY;
        private boolean firstField = false;

        private Reader(Input input, int quoteCode, int delimiterCode, int commentCode, EndOfLineDecoder eolDecoder, char[] fieldChars) {
            this.input = input;
            this.quoteCode = quoteCode;
            this.delimiterCode = delimiterCode;
            this.commentCode = commentCode;
            this.eolDecoder = eolDecoder;
            this.fieldChars = fieldChars;
        }

        private static final int STATE_READY = 0;
        private static final int STATE_NOT_LAST = 1;
        private static final int STATE_LAST = 2;
        private static final int STATE_DONE = 3;

        private static final int FIELD_TYPE_NORMAL = 0;
        private static final int FIELD_TYPE_QUOTED = 1;
        private static final int FIELD_TYPE_COMMENTED = 2;

        /**
         * Reads the next line.
         *
         * @return true if not at the end of file
         * @throws IOException if an I/O error occurs
         */
        public boolean readLine() throws IOException {
            switch (state) {
                case STATE_DONE:
                    return false;
                case STATE_READY:
                case STATE_LAST: {
                    parseFirstField();
                    return state != STATE_DONE;
                }
                case STATE_NOT_LAST: {
                    skipRemainingFields();
                    parseFirstField();
                    return state != STATE_DONE;
                }
                default:
                    throw newUnreachable();
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
                case STATE_LAST:
                    if (firstField) {
                        firstField = false;
                        return isFieldNotNull();
                    }
                    return false;
                case STATE_NOT_LAST:
                    if (firstField) {
                        firstField = false;
                        return true;
                    }
                    parseNextField();
                    return true;
                case STATE_DONE:
                    return false;
                case STATE_READY:
                    throw new IllegalStateException();
                default:
                    throw newUnreachable();
            }
        }

        /**
         * Check if the current field is a comment or not.
         *
         * @return true if the current field is a comment
         */
        public boolean isComment() {
            return fieldType == FIELD_TYPE_COMMENTED;
        }

        @Override
        public void close() throws IOException {
            input.close();
        }

        private void skipRemainingFields() throws IOException {
            firstField = false;
            do {
                parseNextField();
            } while (state == STATE_NOT_LAST);
        }

        private void parseFirstField() throws IOException {
            firstField = true;
            parseNextField();
        }

        // WARNING: main loop; lots of duplication to maximize performances
        // WARNING: comparing ints more performant than comparing chars
        // WARNING: local var access slightly quicker that field access
        private void parseNextField() throws IOException {
            int fieldLength = this.fieldLength;
            int fieldType = this.fieldType;
            int state = this.state;

            try {
                final int quoteCode = this.quoteCode;
                final int delimiterCode = this.delimiterCode;
                final int commentCode = this.commentCode;
                final char[] fieldChars = this.fieldChars;

                int code;

                fieldLength = 0;

                // [Step 1]: first char
                fieldType = FIELD_TYPE_NORMAL;
                if (/*-next-*/ (code = input.read()) != Input.EOF_CODE) {
                    if (code == quoteCode) {
                        fieldType = FIELD_TYPE_QUOTED;
                    } else if (firstField && code == commentCode) {
                        fieldType = FIELD_TYPE_COMMENTED;
                    } else {
                        /*-end-of-field-*/
                        if (code == delimiterCode) {
                            state = STATE_NOT_LAST;
                            return;
                        } else if (eolDecoder.isEndOfLine(code, input)) {
                            state = STATE_LAST;
                            return;
                        }
                        /*-append-*/
                        fieldChars[fieldLength++] = (char) code;
                    }
                } else {
                    // EOF
                    {
                        state = STATE_DONE;
                        return;
                    }
                }

                switch (fieldType) {
                    case FIELD_TYPE_NORMAL: {
                        // [Step 2A]: subsequent chars without escape
                        while (/*-next-*/ (code = input.read()) != Input.EOF_CODE) {
                            /*-end-of-field-*/
                            if (code == delimiterCode) {
                                state = STATE_NOT_LAST;
                                return;
                            } else if (eolDecoder.isEndOfLine(code, input)) {
                                state = STATE_LAST;
                                return;
                            }
                            /*-append-*/
                            fieldChars[fieldLength++] = (char) code;
                        }
                        // EOF
                        {
                            state = fieldLength > 0 ? STATE_LAST : STATE_DONE;
                            return;
                        }
                    }
                    case FIELD_TYPE_QUOTED: {
                        // [Step 2B]: subsequent chars with escape
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
                                        state = STATE_NOT_LAST;
                                        return;
                                    } else if (eolDecoder.isEndOfLine(code, input)) {
                                        state = STATE_LAST;
                                        return;
                                    }
                                }
                                /*-append-*/
                                fieldChars[fieldLength++] = (char) code;
                            }
                        }
                        // EOF
                        {
                            state = STATE_LAST;
                            return;
                        }
                    }
                    case FIELD_TYPE_COMMENTED: {
                        // [Step 2C]: subsequent comment chars
                        while (/*-next-*/ (code = input.read()) != Input.EOF_CODE) {
                            if (eolDecoder.isEndOfLine(code, input)) {
                                state = STATE_LAST;
                                return;
                            }
                            /*-append-*/
                            fieldChars[fieldLength++] = (char) code;
                        }
                        // EOF
                        {
                            state = STATE_LAST;
                            return;
                        }
                    }
                    default:
                        throw newUnreachable();
                }

            } catch (IndexOutOfBoundsException ex) {
                throw new IOException("Field overflow", ex);
            } finally {
                this.fieldLength = fieldLength;
                this.fieldType = fieldType;
                this.state = state;
            }
        }

        private boolean isFieldNotNull() {
            return fieldLength > 0 || fieldType != FIELD_TYPE_NORMAL;
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

            private int length = 0;
            private int index = 0;

            private Input(java.io.Reader charReader, char[] charBuffer) {
                this.charReader = charReader;
                this.buffer = charBuffer;
            }

            @Override
            public void close() throws IOException {
                charReader.close();
            }

            public int read() throws IOException {
                return (index < length) ? buffer[index++] : ((length = charReader.read(buffer)) == EOF_CODE) ? EOF_CODE : buffer[(index = 1) - 1];
            }
        }

        private static final class ReadAheadInput extends Input {

            static boolean isNeeded(Format format, ReaderOptions options) {
                return options.isLenientSeparator() || format.getSeparator().length() > 1;
            }

            private static final int NULL_CODE = -2;
            private int readAheadCode = NULL_CODE;

            private ReadAheadInput(java.io.Reader charReader, char[] charBuffer) {
                super(charReader, charBuffer);
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

            public static EndOfLineDecoder of(Format format, ReaderOptions options) {
                String eol = format.getSeparator();
                switch (eol.length()) {
                    case 1:
                        return new SingleDecoder(eol.charAt(0));
                    case 2:
                        return options.isLenientSeparator()
                                ? new LenientDecoder(eol.charAt(0), eol.charAt(1))
                                : new DualDecoder(eol.charAt(0), eol.charAt(1));
                    default:
                        throw newUnreachable();
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
     * CSV writer options.
     */
    public static final class WriterOptions {

        /**
         * Default writer options.
         */
        public static final WriterOptions DEFAULT = new WriterOptions();

        private WriterOptions() {
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
            final WriterOptions other = (WriterOptions) obj;
            return true;
        }

        @Override
        public String toString() {
            return "WriterOptions(" + ')';
        }

        public Builder toBuilder() {
            return new Builder();
        }

        public static Builder builder() {
            return DEFAULT.toBuilder();
        }

        /**
         * CSV writer options builder.
         */
        public static final class Builder {

            private Builder() {
            }

            public WriterOptions build() {
                return new WriterOptions();
            }
        }
    }

    /**
     * CSV writer.
     */
    public static final class Writer implements Closeable {

        /**
         * Creates a new instance from a char writer.
         *
         * @param format         a non-null format
         * @param options        a non-null options
         * @param charWriter     a non-null char writer
         * @param charBufferSize the size of the internal buffer
         * @return a new CSV writer
         * @throws IllegalArgumentException if the format contains an invalid
         *                                  combination of options
         * @throws IOException              if an I/O error occurs
         */
        public static Writer of(Format format, WriterOptions options, java.io.Writer charWriter, int charBufferSize) throws IllegalArgumentException, IOException {
            Objects.requireNonNull(format, "format");
            Objects.requireNonNull(options, "options");
            Objects.requireNonNull(charWriter, "charWriter");
            requireArgument(charBufferSize > 0, "Invalid charBufferSize: %s", charBufferSize);
            requireArgument(format.isValid(), "Invalid format: %s", format);

            return new Writer(
                    new Output(charWriter, new char[charBufferSize]),
                    format.getQuote(), format.getDelimiter(), format.getComment(),
                    EndOfLineEncoder.of(format)
            );
        }

        private final Output output;
        private final char quote;
        private final char delimiter;
        private final char comment;
        private final EndOfLineEncoder eolEncoder;

        private int state = STATE_NO_FIELD;

        private Writer(Output output, char quote, char delimiter, char comment, EndOfLineEncoder eolEncoder) {
            this.output = output;
            this.quote = quote;
            this.delimiter = delimiter;
            this.comment = comment;
            this.eolEncoder = eolEncoder;
        }

        /**
         * Writes a new comment. Null comment is handled as empty.
         *
         * @param comment a nullable field
         * @throws IOException if an I/O error occurs
         */
        public void writeComment(CharSequence comment) throws IOException {
            if (state != STATE_NO_FIELD) {
                writeEndOfLine();
            }
            output.write(this.comment);
            if (comment != null && comment.length() > 0) {
                for (int i = 0; i < comment.length(); i++) {
                    char c = comment.charAt(i);
                    if (eolEncoder.isNewLine(c)) {
                        writeEndOfLine();
                        output.write(this.comment);
                        if (isSkipSecondEOL(comment, i)) {
                            i++;
                        }
                    } else {
                        output.write(c);
                    }
                }
            }
            writeEndOfLine();
        }

        private boolean isSkipSecondEOL(CharSequence text, int i) {
            return eolEncoder instanceof EndOfLineEncoder.DualEncoder
                    && i + 1 < text.length()
                    && eolEncoder.isNewLine(text.charAt(i + 1));
        }

        /**
         * Writes a new field. Null field is handled as empty.
         *
         * @param field a nullable field
         * @throws IOException if an I/O error occurs
         */
        public void writeField(CharSequence field) throws IOException {
            switch (state) {
                case STATE_NO_FIELD: {
                    if (isNotEmpty(field)) {
                        state = STATE_MULTI_FIELD;
                        if (field.charAt(0) == comment) {
                            writeNonEmptyField(field, QUOTING_FULL);
                        } else {
                            writeNonEmptyField(field);
                        }
                    } else {
                        state = STATE_SINGLE_EMPTY_FIELD;
                    }
                    break;
                }
                case STATE_SINGLE_EMPTY_FIELD: {
                    state = STATE_MULTI_FIELD;
                    output.write(delimiter);
                    if (isNotEmpty(field)) {
                        writeNonEmptyField(field);
                    }
                    break;
                }
                case STATE_MULTI_FIELD: {
                    output.write(delimiter);
                    if (isNotEmpty(field)) {
                        writeNonEmptyField(field);
                    }
                    break;
                }
            }
        }

        /**
         * Writes an end of line.
         *
         * @throws IOException if an I/O error occurs
         */
        public void writeEndOfLine() throws IOException {
            flushField();
            eolEncoder.write(output);
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
            writeNonEmptyField(field, getQuoting(field));
        }

        private void writeNonEmptyField(CharSequence field, int quoting) throws IOException {
            switch (quoting) {
                case QUOTING_NONE:
                    output.write(field);
                    break;
                case QUOTING_PARTIAL:
                    output.write(quote);
                    output.write(field);
                    output.write(quote);
                    break;
                case QUOTING_FULL:
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
            if (state == STATE_SINGLE_EMPTY_FIELD) {
                output.write(quote);
                output.write(quote);
            }
            state = STATE_NO_FIELD;
        }

        private int getQuoting(CharSequence field) {
            int result = QUOTING_NONE;
            for (int i = 0; i < field.length(); i++) {
                char c = field.charAt(i);
                if (c == quote) {
                    return QUOTING_FULL;
                }
                if (c == delimiter || eolEncoder.isNewLine(c)) {
                    result = QUOTING_PARTIAL;
                }
            }
            return result;
        }

        private static final int QUOTING_NONE = 0;
        private static final int QUOTING_PARTIAL = 1;
        private static final int QUOTING_FULL = 2;

        private static final int STATE_NO_FIELD = 0;
        private static final int STATE_SINGLE_EMPTY_FIELD = 1;
        private static final int STATE_MULTI_FIELD = 2;

        private static final class Output implements Closeable {

            private final java.io.Writer charWriter;
            private final char[] buffer;

            private int length = 0;

            private Output(java.io.Writer charWriter, char[] charBuffer) {
                this.charWriter = charWriter;
                this.buffer = charBuffer;
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

            abstract public boolean isNewLine(char c);

            public static EndOfLineEncoder of(Format format) {
                String eol = format.getSeparator();
                switch (eol.length()) {
                    case 1:
                        return new SingleEncoder(eol.charAt(0));
                    case 2:
                        return new DualEncoder(eol.charAt(0), eol.charAt(1));
                    default:
                        throw newUnreachable();
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

                @Override
                public boolean isNewLine(char c) {
                    return c == single;
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

                @Override
                public boolean isNewLine(char c) {
                    return c == first || c == second;
                }
            }
        }
    }

    private static RuntimeException newUnreachable() {
        return new RuntimeException("Unreachable");
    }

    private static void requireArgument(boolean condition, String format, Object arg) throws IllegalArgumentException {
        if (!condition) {
            throw new IllegalArgumentException(String.format(format, arg));
        }
    }

    // JDK8
    private static int hashCodeOf(boolean value) {
        return value ? 1231 : 1237;
    }

    private static String prettyPrint(String text) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            result.append(prettyPrint(text.charAt(i)));
        }
        return result.toString();
    }

    private static String prettyPrint(char c) {
        switch (c) {
            case '\t':
                return "\\t";
            case '\b':
                return "\\b";
            case '\n':
                return "\\n";
            case '\r':
                return "\\r";
            case '\f':
                return "\\f";
//            case '\'':
//                return "\\'";
            case '\"':
                return "\\\"";
            case '\\':
                return "\\\\";
            default:
                return String.valueOf(c);
        }
    }
}
