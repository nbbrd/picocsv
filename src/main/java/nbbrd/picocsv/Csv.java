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
import java.io.Flushable;
import java.io.IOException;
import java.util.Locale;
import java.util.Objects;

/**
 * Unusual CSV library for Java.
 * <p>
 * Picocsv is an unusual CSV library designed to be embedded in other libraries. <br>
 * While it can be used directly, it's main purpose is to be the core foundation of those other libraries.
 * <p>
 * Key points:
 * <ul>
 * <li> lightweight library with no dependency
 * <li> fast and efficient (no heap memory allocation)
 * <li> designed to be embedded into other libraries as <a href="https://search.maven.org/artifact/com.github.nbbrd.picocsv/picocsv">an external dependency</a> or <a href="https://github.com/nbbrd/picocsv/blob/develop/src/main/java/nbbrd/picocsv/Csv.java">as a single-file source</a>
 * <li> has a module-info that makes it compatible with <a href="https://www.baeldung.com/java-9-modularity">JPMS</a>
 * <li> Java 8 minimum requirement
 * </ul>
 * <p>
 * Features:
 * <ul>
 * <li> reads/writes CSV from/to character streams
 * <li> provides a minimalist low-level API
 * <li> does not interpret content
 * <li> does not correct invalid files
 * <li> follows the <a href="https://tools.ietf.org/html/rfc4180">RFC4180</a> specification
 * <li> supports custom line separator
 * <li> supports custom comment character
 * </ul>
 * <p>
 * ⚠️ <i>Note that the <code>Format#acceptMissingField</code> option must be set to <code>false</code> to closely follow the RFC4180 specification.
 * The default value is currently <code>true</code> but will be reversed in the next major release.</i>
 *
 * @author Philippe Charles
 */
public final class Csv {

    private Csv() {
        // static class
    }

    /**
     * Default character buffer size used to read and write content.
     */
    public static final int DEFAULT_CHAR_BUFFER_SIZE = 8192;

    /**
     * CSV format.
     * This format is used both by reader and writer but is independent of the source of data (stream or files).
     * Therefore, it doesn't deal with encoding.
     *
     * <p> This class is immutable and is created by a builder.
     * <pre>
     * Csv.Format tsv = Csv.Format.builder().delimiter('\t').build();
     * </pre>
     */
    public static final class Format {

        /**
         * Default line separator for Windows OS.
         */
        public static final String WINDOWS_SEPARATOR = "\r\n";

        /**
         * Default line separator for Unix and Unix-like OS.
         */
        public static final String UNIX_SEPARATOR = "\n";

        /**
         * Default line separator for classic Mac OS.
         */
        public static final String MACINTOSH_SEPARATOR = "\r";

        private static final String DEFAULT_SEPARATOR = WINDOWS_SEPARATOR;
        private static final char DEFAULT_DELIMITER = ',';
        private static final char DEFAULT_QUOTE = '"';
        private static final char DEFAULT_COMMENT = '#';
        private static final boolean DEFAULT_ACCEPT_MISSING_FIELD = true;

        /**
         * Predefined format as defined by <a href="https://tools.ietf.org/html/rfc4180">RFC 4180</a>.
         */
        public static final Format RFC4180 = new Format(DEFAULT_SEPARATOR, DEFAULT_DELIMITER, DEFAULT_QUOTE, DEFAULT_COMMENT, DEFAULT_ACCEPT_MISSING_FIELD);

        /**
         * Predefined format as alias to {@link Format#RFC4180}.
         */
        public static final Format DEFAULT = RFC4180;

        private final String separator;
        private final char delimiter;
        private final char quote;
        private final char comment;
        private final boolean acceptMissingField;

        private Format(String separator, char delimiter, char quote, char comment, boolean acceptMissingField) {
            this.separator = Objects.requireNonNull(separator, "separator");
            this.delimiter = delimiter;
            this.quote = quote;
            this.comment = comment;
            this.acceptMissingField = acceptMissingField;
        }

        /**
         * Characters used to separate lines.
         * <p>
         * The default value is <code>"\r\n"</code>.
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
         * The default value is <code>','</code>.
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
         * Determines if missing field is accepted in a record.
         *
         * @return <code>true</code> if missing field is accepted, <code>false</code> otherwise
         */
        public boolean isAcceptMissingField() {
            return acceptMissingField;
        }

        /**
         * Checks if the current format is valid.
         *
         * <p> Validation rules:
         * <ul>
         * <li>Separator has one or two chars
         * <li>delimiter != quote != separator chars
         * </ul>
         *
         * @return <code>true</code> if valid, <code>false</code> otherwise
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
            hash = 37 * hash + (this.acceptMissingField ? 1 : 0);
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
            if (this.acceptMissingField != other.acceptMissingField) return false;
            return true;
        }

        @Override
        public String toString() {
            return "Format("
                    + "separator=" + prettyPrint(separator)
                    + ", delimiter=" + prettyPrint(delimiter)
                    + ", quote=" + prettyPrint(quote)
                    + ", comment=" + prettyPrint(comment)
                    + ", acceptMissingField=" + acceptMissingField
                    + ')';
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

        /**
         * Creates a new builder using this instance values.
         *
         * @return a non-null builder
         */
        public Builder toBuilder() {
            return new Builder()
                    .separator(separator)
                    .delimiter(delimiter)
                    .quote(quote)
                    .comment(comment)
                    .acceptMissingField(acceptMissingField);
        }

        /**
         * Creates a new builder using the default values.
         *
         * @return a non-null builder
         */
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
            private boolean acceptMissingField;

            private Builder() {
            }

            /**
             * Sets the {@link Format#getSeparator() separator} parameter of {@link Format}.
             *
             * @param separator a non-null string
             * @return this builder
             */
            public Builder separator(String separator) {
                this.separator = separator;
                return this;
            }

            /**
             * Sets the {@link Format#getDelimiter() delimiter} parameter of {@link Format}.
             *
             * @param delimiter a character
             * @return this builder
             */
            public Builder delimiter(char delimiter) {
                this.delimiter = delimiter;
                return this;
            }

            /**
             * Sets the {@link Format#getQuote() quote} parameter of {@link Format}.
             *
             * @param quote a character
             * @return this builder
             */
            public Builder quote(char quote) {
                this.quote = quote;
                return this;
            }

            /**
             * Sets the {@link Format#getComment() comment} parameter of {@link Format}.
             *
             * @param comment a character
             * @return this builder
             */
            public Builder comment(char comment) {
                this.comment = comment;
                return this;
            }

            /**
             * Sets the {@link Format#isAcceptMissingField()} () comment} parameter of {@link Format}.
             *
             * @param acceptMissingField a boolean
             * @return this builder
             */
            public Builder acceptMissingField(boolean acceptMissingField) {
                this.acceptMissingField = acceptMissingField;
                return this;
            }

            /**
             * Creates a new instance of {@link Format}.
             *
             * @return a non-null new instance
             */
            public Format build() {
                return new Format(separator, delimiter, quote, comment, acceptMissingField);
            }
        }
    }

    /**
     * CSV reader options.
     * Defines how the reader behaves.
     *
     * <p> This class is immutable and is created by a builder.
     * <pre>
     * Csv.ReaderOptions options = Csv.ReaderOptions.builder().lenientSeparator(false).build();
     * </pre>
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
         * The default value is <code>{@value ReaderOptions#DEFAULT_LENIENT_SEPARATOR}</code>.
         *
         * @return <code>true</code> if lenient parsing of separator, <code>false</code> otherwise
         */
        public boolean isLenientSeparator() {
            return lenientSeparator;
        }

        /**
         * Determines the maximum number of characters to read in each field
         * to avoid {@link java.lang.OutOfMemoryError} in case a file does not
         * have a valid format. This sets a limit which avoids unwanted JVM crashes.
         * <p>
         * The default value is <code>{@value ReaderOptions#DEFAULT_MAX_CHARS_PER_FIELD}</code>.
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
         * @return <code>true</code> if valid, <code>false</code> otherwise
         */
        public boolean isValid() {
            return maxCharsPerField > 0;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 37 * hash + Boolean.hashCode(lenientSeparator);
            hash = 37 * hash + this.maxCharsPerField;
            return hash;
        }

        @SuppressWarnings("RedundantIfStatement")
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

        /**
         * Creates a new builder using this instance values.
         *
         * @return a non-null builder
         */
        public Builder toBuilder() {
            return new Builder()
                    .lenientSeparator(lenientSeparator)
                    .maxCharsPerField(maxCharsPerField);
        }

        /**
         * Creates a new builder using the default values.
         *
         * @return a non-null builder
         */
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

            /**
             * Sets the {@link ReaderOptions#isLenientSeparator() lenientSeparator} parameter of {@link ReaderOptions}.
             *
             * @param lenientSeparator <code>true</code> if lenient parsing of separator, <code>false</code> otherwise
             * @return this builder
             */
            public Builder lenientSeparator(boolean lenientSeparator) {
                this.lenientSeparator = lenientSeparator;
                return this;
            }

            /**
             * Sets the {@link ReaderOptions#getMaxCharsPerField() maxCharsPerField} parameter of {@link ReaderOptions}.
             *
             * @param maxCharsPerField the maximum number of characters for a field
             * @return this builder
             */
            public Builder maxCharsPerField(int maxCharsPerField) {
                this.maxCharsPerField = maxCharsPerField;
                return this;
            }

            /**
             * Creates a new instance of {@link ReaderOptions}.
             *
             * @return a non-null new instance
             */
            public ReaderOptions build() {
                return new ReaderOptions(lenientSeparator, maxCharsPerField);
            }
        }
    }

    /**
     * CSV line reader.
     * This interface describes all the read operations available on a CSV line.
     */
    public interface LineReader extends CharSequence {

        /**
         * Reads the next field.
         *
         * @return <code>true</code> if not at the end of line, <code>false</code> otherwise
         * @throws IOException if an I/O error occurs
         */
        boolean readField() throws IOException;

        /**
         * Check if the current line is a comment or not.
         *
         * @return <code>true</code> if the current line is a comment, <code>false</code> otherwise
         */
        boolean isComment();

        /**
         * Check if the current field is a quoted or not.
         *
         * @return <code>true</code> if the current field is quoted, <code>false</code> otherwise
         */
        boolean isQuoted();
    }

    /**
     * CSV reader.
     * Reads CSV from a character-input stream, buffering characters in order to provide efficient reading.
     *
     * <p> This class is created by a static factory method.
     * <pre>
     * try (java.io.Reader chars = ...; Csv.Reader csv = Csv.Reader.of(Csv.Format.DEFAULT, Csv.ReaderOptions.DEFAULT, chars)) {
     *   ...
     * }
     * </pre>
     * While chaining streams on creation is possible, it is recommended to use the "try-with-resources with multiple resources" pattern instead.
     * This ensures that the resources are properly closed on a class initialization failure.
     *
     * <p> The buffer size can be specified. Ideally, it should align with the underlying input
     * but the {@link Csv#DEFAULT_CHAR_BUFFER_SIZE} should be ok for most usage.<br>
     * Note that the CSV reader maintains its own buffer so there is no need to create a {@link java.io.BufferedReader}.
     *
     * @see java.io.Reader
     */
    public static final class Reader implements LineReader, Closeable {

        /**
         * Creates a new instance from a char reader using the {@link #DEFAULT_CHAR_BUFFER_SIZE default char buffer size}.
         *
         * @param format     a non-null format
         * @param options    a non-null options
         * @param charReader a non-null char reader
         * @return a new CSV reader
         * @throws IllegalArgumentException if the format contains an invalid
         *                                  combination of options
         * @throws IOException              if an I/O error occurs
         */
        public static Reader of(Format format, ReaderOptions options, java.io.Reader charReader) throws IllegalArgumentException, IOException {
            return of(format, options, charReader, DEFAULT_CHAR_BUFFER_SIZE);
        }

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

            byte eolType;
            int eolCode0;
            int eolCode1;

            if (format.getSeparator().length() == 1) {
                eolType = EOL_TYPE_SINGLE;
                eolCode0 = format.getSeparator().charAt(0);
                eolCode1 = EOF_CODE;
            } else {
                eolType = options.isLenientSeparator() ? EOL_TYPE_DUAL_LENIENT : EOL_TYPE_DUAL_STRICT;
                eolCode0 = format.getSeparator().charAt(0);
                eolCode1 = format.getSeparator().charAt(1);
            }

            return new Reader(
                    charReader, charBuffer,
                    format.getQuote(), format.getDelimiter(), format.getComment(), format.isAcceptMissingField() ? STATE_5_MISSING : STATE_4_SINGLE,
                    new char[options.getMaxCharsPerField()],
                    eolType, eolCode0, eolCode1
            );
        }

        private final java.io.Reader charReader;
        private final char[] buffer;
        private final int quoteCode;
        private final int delimiterCode;
        private final int commentCode;
        private final byte emptyLineState;
        private final char[] fieldChars;
        private final byte eolType;
        private final int eolCode0;
        private final int eolCode1;

        private int bufferLength = 0;
        private int bufferIndex = 0;
        private int fieldLength = 0;
        private byte fieldType = FIELD_TYPE_NORMAL;
        private byte state = STATE_0_READY;

        private Reader(java.io.Reader charReader, char[] buffer, int quoteCode, int delimiterCode, int commentCode, byte emptyLineState, char[] fieldChars, byte eolType, int eolCode0, int eolCode1) {
            this.charReader = charReader;
            this.buffer = buffer;
            this.quoteCode = quoteCode;
            this.delimiterCode = delimiterCode;
            this.commentCode = commentCode;
            this.emptyLineState = emptyLineState;
            this.fieldChars = fieldChars;
            this.eolType = eolType;
            this.eolCode0 = eolCode0;
            this.eolCode1 = eolCode1;
        }

        private static final byte STATE_0_READY = 0;
        private static final byte STATE_1_FIRST = 1;
        private static final byte STATE_2_NOT_LAST = 2;
        private static final byte STATE_3_LAST = 3;
        private static final byte STATE_4_SINGLE = 4;
        private static final byte STATE_5_MISSING = 5;
        private static final byte STATE_6_DONE = 6;

        private static final byte FIELD_TYPE_NORMAL = 10;
        private static final byte FIELD_TYPE_QUOTED = 11;
        private static final byte FIELD_TYPE_COMMENTED = 12;

        private static final byte EOL_TYPE_SINGLE = 20;
        private static final byte EOL_TYPE_DUAL_STRICT = 21;
        private static final byte EOL_TYPE_DUAL_LENIENT = 22;

        private static final int EOF_CODE = -1;

        /**
         * Reads the next line.
         *
         * @return <code>true</code> if not at the end of file, <code>false</code> otherwise
         * @throws IOException if an I/O error occurs
         */
        @SuppressWarnings("DuplicateBranchesInSwitch")
        public boolean readLine() throws IOException {
            // WARNING: try to force JVM "tableswitch"
            // WARNING: see https://docs.oracle.com/javase/specs/jvms/se18/html/jvms-6.html#jvms-6.5.tableswitch
            // WARNING: default value in JDK21 -XX:MinJumpTableSize=10
            switch (state) {
                case STATE_0_READY:
                    relocateBuffer();
                    parseNextField(true);
                    return state != STATE_6_DONE;
                case STATE_1_FIRST:
                    skipRemainingFields();
                    relocateBuffer();
                    parseNextField(true);
                    return state != STATE_6_DONE;
                case STATE_2_NOT_LAST:
                    skipRemainingFields();
                    relocateBuffer();
                    parseNextField(true);
                    return state != STATE_6_DONE;
                case STATE_3_LAST:
                    relocateBuffer();
                    parseNextField(true);
                    return state != STATE_6_DONE;
                case STATE_4_SINGLE:
                    relocateBuffer();
                    parseNextField(true);
                    return state != STATE_6_DONE;
                case STATE_5_MISSING:
                    relocateBuffer();
                    parseNextField(true);
                    return state != STATE_6_DONE;
                case STATE_6_DONE:
                    return false;
                case 7:
                case 8:
                case 9:
                case 10:
                case 11:
                case 12:
                case 13:
                case 14:
                case 15:
                case 16:
                case 17:
                case 18:
                    return false;
                default:
                    throw new RuntimeException("Unreachable");
            }
        }

        @SuppressWarnings("DuplicateBranchesInSwitch")
        @Override
        public boolean readField() throws IOException {
            // WARNING: try to force JVM "tableswitch"
            // WARNING: see https://docs.oracle.com/javase/specs/jvms/se18/html/jvms-6.html#jvms-6.5.tableswitch
            // WARNING: default value in JDK21 -XX:MinJumpTableSize=10
            switch (state) {
                case STATE_0_READY:
                    throw new IllegalStateException();
                case STATE_1_FIRST:
                    state = STATE_2_NOT_LAST;
                    return true;
                case STATE_2_NOT_LAST:
                    parseNextField(false);
                    return true;
                case STATE_3_LAST:
                    return false;
                case STATE_4_SINGLE:
                    state = STATE_3_LAST;
                    return true;
                case STATE_5_MISSING:
                    return false;
                case STATE_6_DONE:
                    return false;
                case 7:
                case 8:
                case 9:
                case 10:
                case 11:
                case 12:
                case 13:
                case 14:
                case 15:
                case 16:
                case 17:
                case 18:
                    return false;
                default:
                    throw new RuntimeException("Unreachable");
            }
        }

        @Override
        public boolean isComment() {
            return fieldType == FIELD_TYPE_COMMENTED;
        }

        @Override
        public boolean isQuoted() {
            return fieldType == FIELD_TYPE_QUOTED;
        }

        /**
         * Closes the {@link java.io.Reader chararcter stream} used by this reader.
         *
         * @throws IOException if an I/O error occurs
         */
        @Override
        public void close() throws IOException {
            charReader.close();
        }

        private void skipRemainingFields() throws IOException {
            do {
                parseNextField(false);
            } while (state == STATE_2_NOT_LAST);
        }

        private void relocateBuffer() throws IOException {
            if (bufferLength != EOF_CODE && bufferIndex > buffer.length / 2) {
                int remainingChars = bufferLength - bufferIndex;
                System.arraycopy(buffer, bufferIndex, buffer, 0, remainingChars);
                int newChars = charReader.read(buffer, remainingChars, bufferIndex);
                bufferIndex = 0;
                bufferLength = remainingChars + newChars;
            }
        }

        // WARNING: main loop; lots of duplication to maximize performances
        // WARNING: comparing ints more performant than comparing chars
        // WARNING: local var access slightly quicker that field access
        // WARNING: the main trick is to never get out of this method when reading a field
        // WARNING: and therefore never have to fill the buffer
        private void parseNextField(final boolean firstField) throws IOException {
            int fieldLength = this.fieldLength;
            int i = this.bufferIndex;
            int l = this.bufferLength;

            try {
                final int quoteCode = this.quoteCode;
                final int delimiterCode = this.delimiterCode;
//                final int commentCode = this.commentCode;
                final char[] fieldChars = this.fieldChars;

                final java.io.Reader s = this.charReader;
                final char[] b = this.buffer;

                final byte eolType = this.eolType;
                final int eolCode0 = this.eolCode0;
                final int eolCode1 = this.eolCode1;

                int firstCode;

                fieldLength = 0;

                // [STEP 1]: first char
                if ((firstCode = (/*next>*/ i < l ? b[i++] : (l = s.read(b)) == EOF_CODE ? EOF_CODE : b[(i = 1) - 1] /*<next*/)) != EOF_CODE) {

                    // FIELD_TYPE_QUOTED
                    if (firstCode == quoteCode) {
                        fieldType = FIELD_TYPE_QUOTED;

                        // [STEP 2B]: subsequent chars with escape
                        boolean escaped = false;
                        for (int subCode; (subCode = (/*next>*/ i < l ? b[i++] : (l = s.read(b)) == EOF_CODE ? EOF_CODE : b[(i = 1) - 1] /*<next*/)) != EOF_CODE; ) {
                            if (subCode == quoteCode) {
                                if (!escaped) {
                                    escaped = true;
                                } else {
                                    escaped = false;
                                    /*-append-*/
                                    fieldChars[fieldLength++] = (char) subCode;
                                }
                            } else {
                                if (escaped) {
                                    /*-end-of-field-*/
                                    if (subCode == delimiterCode) {
                                        state = firstField ? STATE_1_FIRST : STATE_2_NOT_LAST;
                                        return;
                                    }
                                    /*-end-of-line-*/
                                    {
                                        if (subCode == eolCode0) {
                                            switch (eolType) {
                                                case EOL_TYPE_SINGLE:
                                                    state = firstField ? STATE_4_SINGLE : STATE_3_LAST;
                                                    return;
                                                case EOL_TYPE_DUAL_STRICT:
                                                    if ((/*next>*/ i < l ? b[i++] : (l = s.read(b)) == EOF_CODE ? EOF_CODE : b[(i = 1) - 1] /*<next*/) != eolCode1) {
                                                        i--;
                                                        break;
                                                    }
                                                    state = firstField ? STATE_4_SINGLE : STATE_3_LAST;
                                                    return;
                                                case EOL_TYPE_DUAL_LENIENT:
                                                    if ((/*next>*/ i < l ? b[i++] : (l = s.read(b)) == EOF_CODE ? EOF_CODE : b[(i = 1) - 1] /*<next*/) != eolCode1) {
                                                        i--;
                                                    }
                                                    state = firstField ? STATE_4_SINGLE : STATE_3_LAST;
                                                    return;
                                            }
                                        } else if (eolType == EOL_TYPE_DUAL_LENIENT && subCode == eolCode1) {
                                            state = firstField ? STATE_4_SINGLE : STATE_3_LAST;
                                            return;
                                        }
                                    }
                                }
                                /*-append-*/
                                fieldChars[fieldLength++] = (char) subCode;
                            }
                        }
                        // EOF STEP 2B
                        state = firstField ? STATE_4_SINGLE : STATE_3_LAST;
                        return;
                    }

                    // FIELD_TYPE_COMMENTED
                    if (firstField && firstCode == commentCode) {
                        fieldType = FIELD_TYPE_COMMENTED;

                        // [STEP 2C]: subsequent comment chars
                        for (int subCode; (subCode = (/*next>*/ i < l ? b[i++] : (l = s.read(b)) == EOF_CODE ? EOF_CODE : b[(i = 1) - 1] /*<next*/)) != EOF_CODE; ) {
                            /*-end-of-line-*/
                            {
                                if (subCode == eolCode0) {
                                    switch (eolType) {
                                        case EOL_TYPE_SINGLE:
                                            state = STATE_4_SINGLE;
                                            return;
                                        case EOL_TYPE_DUAL_STRICT:
                                            if ((/*next>*/ i < l ? b[i++] : (l = s.read(b)) == EOF_CODE ? EOF_CODE : b[(i = 1) - 1] /*<next*/) != eolCode1) {
                                                i--;
                                                break;
                                            }
                                            state = STATE_4_SINGLE;
                                            return;
                                        case EOL_TYPE_DUAL_LENIENT:
                                            if ((/*next>*/ i < l ? b[i++] : (l = s.read(b)) == EOF_CODE ? EOF_CODE : b[(i = 1) - 1] /*<next*/) != eolCode1) {
                                                i--;
                                            }
                                            state = STATE_4_SINGLE;
                                            return;
                                    }
                                } else if (eolType == EOL_TYPE_DUAL_LENIENT && subCode == eolCode1) {
                                    state = STATE_4_SINGLE;
                                    return;
                                }
                            }
                            /*-append-*/
                            fieldChars[fieldLength++] = (char) subCode;
                        }
                        // EOF STEP 2C
                        state = STATE_4_SINGLE;
                        return;
                    }

                    // FIELD_TYPE_NORMAL
                    fieldType = FIELD_TYPE_NORMAL;
                    /*-end-of-field-*/
                    if (firstCode == delimiterCode) {
                        state = firstField ? STATE_1_FIRST : STATE_2_NOT_LAST;
                        return;
                    }
                    /*-end-of-line-*/
                    {
                        if (firstCode == eolCode0) {
                            switch (eolType) {
                                case EOL_TYPE_SINGLE:
                                    state = firstField ? emptyLineState : STATE_3_LAST;
                                    return;
                                case EOL_TYPE_DUAL_STRICT:
                                    if ((/*next>*/ i < l ? b[i++] : (l = s.read(b)) == EOF_CODE ? EOF_CODE : b[(i = 1) - 1] /*<next*/) != eolCode1) {
                                        i--;
                                        break;
                                    }
                                    state = firstField ? emptyLineState : STATE_3_LAST;
                                    return;
                                case EOL_TYPE_DUAL_LENIENT:
                                    if ((/*next>*/ i < l ? b[i++] : (l = s.read(b)) == EOF_CODE ? EOF_CODE : b[(i = 1) - 1] /*<next*/) != eolCode1) {
                                        i--;
                                    }
                                    state = firstField ? emptyLineState : STATE_3_LAST;
                                    return;
                            }
                        } else if (eolType == EOL_TYPE_DUAL_LENIENT && firstCode == eolCode1) {
                            state = firstField ? emptyLineState : STATE_3_LAST;
                            return;
                        }
                    }
                    /*-append-*/
                    fieldChars[fieldLength++] = (char) firstCode;

                    // [STEP 2A]: subsequent chars without escape
                    for (int subCode; (subCode = (/*next>*/ i < l ? b[i++] : (l = s.read(b)) == EOF_CODE ? EOF_CODE : b[(i = 1) - 1] /*<next*/)) != EOF_CODE; ) {
                        /*-end-of-field-*/
                        if (subCode == delimiterCode) {
                            state = firstField ? STATE_1_FIRST : STATE_2_NOT_LAST;
                            return;
                        }
                        /*-end-of-line-*/
                        {
                            if (subCode == eolCode0) {
                                switch (eolType) {
                                    case EOL_TYPE_SINGLE:
                                        state = firstField ? STATE_4_SINGLE : STATE_3_LAST;
                                        return;
                                    case EOL_TYPE_DUAL_STRICT:
                                        if ((/*next>*/ i < l ? b[i++] : (l = s.read(b)) == EOF_CODE ? EOF_CODE : b[(i = 1) - 1] /*<next*/) != eolCode1) {
                                            i--;
                                            break;
                                        }
                                        state = firstField ? STATE_4_SINGLE : STATE_3_LAST;
                                        return;
                                    case EOL_TYPE_DUAL_LENIENT:
                                        if ((/*next>*/ i < l ? b[i++] : (l = s.read(b)) == EOF_CODE ? EOF_CODE : b[(i = 1) - 1] /*<next*/) != eolCode1) {
                                            i--;
                                        }
                                        state = firstField ? STATE_4_SINGLE : STATE_3_LAST;
                                        return;
                                }
                            } else if (eolType == EOL_TYPE_DUAL_LENIENT && subCode == eolCode1) {
                                state = firstField ? STATE_4_SINGLE : STATE_3_LAST;
                                return;
                            }
                        }
                        /*-append-*/
                        fieldChars[fieldLength++] = (char) subCode;
                    }
                    // EOF STEP 2A
                    state = fieldLength > 0 ? (firstField ? STATE_4_SINGLE : STATE_3_LAST) : STATE_6_DONE;
                    return;

                }
                // EOF STEP 1
                state = STATE_6_DONE;
                return;

            } catch (IndexOutOfBoundsException ex) {
                throw new IOException("Field overflow", ex);
            } finally {
                this.fieldLength = fieldLength;
                this.bufferLength = l;
                this.bufferIndex = i;
            }
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
    }

    /**
     * CSV writer options.
     * Defines how the writer behaves.
     *
     * <p> This class is immutable and is created by a builder.
     * <pre>
     * Csv.WriterOptions options = Csv.WriterOptions.builder().build();
     * </pre>
     */
    public static final class WriterOptions {

        /**
         * Default writer options.
         */
        public static final WriterOptions DEFAULT = new WriterOptions();

        private WriterOptions() {
        }

        @SuppressWarnings("UnnecessaryLocalVariable")
        @Override
        public int hashCode() {
            int hash = 7;
            return hash;
        }

        @SuppressWarnings("unused")
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

        /**
         * Creates a new builder using this instance values.
         *
         * @return a non-null builder
         */
        public Builder toBuilder() {
            return new Builder();
        }

        /**
         * Creates a new builder using the default values.
         *
         * @return a non-null builder
         */
        public static Builder builder() {
            return DEFAULT.toBuilder();
        }

        /**
         * CSV writer options builder.
         */
        public static final class Builder {

            private Builder() {
            }

            /**
             * Creates a new instance of {@link WriterOptions}.
             *
             * @return a non-null new instance
             */
            public WriterOptions build() {
                return new WriterOptions();
            }
        }
    }

    /**
     * CSV line writer.
     * This interface describes all the write operations available on a CSV line.
     */
    public interface LineWriter {

        /**
         * Writes a new comment. Null is handled as empty.
         *
         * @param comment a nullable field
         * @throws IOException if an I/O error occurs
         */
        void writeComment(CharSequence comment) throws IOException;

        /**
         * Writes a new field. Null is handled as empty.
         *
         * @param field a nullable field
         * @throws IOException if an I/O error occurs
         */
        void writeField(CharSequence field) throws IOException;

        /**
         * Writes a new quoted field. Null is handled as empty.
         *
         * @param field a nullable field
         * @throws IOException if an I/O error occurs
         */
        void writeQuotedField(CharSequence field) throws IOException;
    }

    /**
     * CSV writer.
     * Writes text to a character-output stream, buffering characters in order to provide efficient writing.
     *
     * <p> This class is created by a static factory method.
     * <pre>
     * try (java.io.Writer chars = ...; Csv.Writer csv = Csv.Writer.of(Csv.Format.DEFAULT, Csv.WriterOptions.DEFAULT, chars)) {
     *   ...
     * }
     * </pre>
     * While chaining streams on creation is possible, it is recommended to use the "try-with-resources with multiple resources" pattern instead.
     * This ensures that the resources are properly closed on a class initialization failure.
     *
     * <p> The buffer size can be specified. Ideally, it should align with the underlying output
     * but the {@link Csv#DEFAULT_CHAR_BUFFER_SIZE} should be ok for most usage.<br>
     * Note that the CSV writer maintains its own buffer so there is no need to create a {@link java.io.BufferedWriter}.
     *
     * @see java.io.Writer
     */
    public static final class Writer implements LineWriter, Flushable, Closeable {

        /**
         * Creates a new instance from a char writer using the {@link #DEFAULT_CHAR_BUFFER_SIZE default char buffer size}.
         *
         * @param format     a non-null format
         * @param options    a non-null options
         * @param charWriter a non-null char writer
         * @return a new CSV writer
         * @throws IllegalArgumentException if the format contains an invalid
         *                                  combination of options
         * @throws IOException              if an I/O error occurs
         */
        public static Writer of(Format format, WriterOptions options, java.io.Writer charWriter) throws IllegalArgumentException, IOException {
            return of(format, options, charWriter, DEFAULT_CHAR_BUFFER_SIZE);
        }

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
                    charWriter, new char[charBufferSize],
                    format.getQuote(), format.getDelimiter(), format.getComment(),
                    format.getSeparator().charAt(0), format.getSeparator().length() == 1 ? NO_SECOND_EOL : format.getSeparator().charAt(1)
            );
        }

        private final java.io.Writer charWriter;
        private final char[] buffer;
        private final char quote;
        private final char delimiter;
        private final char comment;
        private final char eol0;
        private final char eol1;

        private int bufferLength = 0;
        private int state = STATE_0_NO_FIELD;

        private Writer(java.io.Writer charWriter, char[] buffer, char quote, char delimiter, char comment, char eol0, char eol1) {
            this.charWriter = charWriter;
            this.buffer = buffer;
            this.quote = quote;
            this.delimiter = delimiter;
            this.comment = comment;
            this.eol0 = eol0;
            this.eol1 = eol1;
        }

        @Override
        public void writeComment(CharSequence comment) throws IOException {
            switch (state) {
                case STATE_0_NO_FIELD:
                    break;
                case STATE_1_SINGLE_EMPTY_FIELD:
                    state = STATE_0_NO_FIELD;
                    formatSingleEmptyField();
                    formatEndOfLine();
                    break;
                case STATE_2_MULTI_FIELD:
                    state = STATE_0_NO_FIELD;
                    formatEndOfLine();
                    flushBuffer();
                    break;
            }

            if (comment != null && comment.length() > 0) {
                formatComment(comment);
            } else {
                formatEmptyComment();
            }
        }

        @Override
        public void writeField(CharSequence field) throws IOException {
            boolean notEmpty = field != null && field.length() != 0;
            switch (state) {
                case STATE_0_NO_FIELD: {
                    state = notEmpty ? STATE_2_MULTI_FIELD : STATE_1_SINGLE_EMPTY_FIELD;
                    if (notEmpty) {
                        if (field.charAt(0) == comment) {
                            formatField(field, QUOTING_FULL, true);
                        } else {
                            formatField(field, parseQuoting(field), true);
                        }
                    }
                    break;
                }
                case STATE_1_SINGLE_EMPTY_FIELD: {
                    state = STATE_2_MULTI_FIELD;
                    if (notEmpty) {
                        formatField(field, parseQuoting(field), false);
                    } else
                        formatEmptyField();
                    break;
                }
                case STATE_2_MULTI_FIELD: {
                    if (notEmpty) {
                        formatField(field, parseQuoting(field), false);
                    } else
                        formatEmptyField();
                    break;
                }
            }
        }

        @Override
        public void writeQuotedField(CharSequence field) throws IOException {
            boolean notEmpty = field != null && field.length() != 0;
            switch (state) {
                case STATE_0_NO_FIELD: {
                    state = STATE_2_MULTI_FIELD;
                    if (notEmpty) {
                        if (field.charAt(0) == comment) {
                            formatField(field, QUOTING_FULL, true);
                        } else {
                            formatField(field, parseForcedQuoting(field), true);
                        }
                    } else
                        formatSingleEmptyField();
                    break;
                }
                case STATE_1_SINGLE_EMPTY_FIELD: {
                    state = STATE_2_MULTI_FIELD;
                    if (notEmpty) {
                        formatField(field, parseForcedQuoting(field), false);
                    } else
                        formatEmptyQuotedField();
                    break;
                }
                case STATE_2_MULTI_FIELD: {
                    if (notEmpty) {
                        formatField(field, parseForcedQuoting(field), false);
                    } else
                        formatEmptyQuotedField();
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
            if (state == STATE_1_SINGLE_EMPTY_FIELD) {
                formatSingleEmptyField();
            }
            formatEndOfLine();
            state = STATE_0_NO_FIELD;
            //if (bufferLength > buffer.length / 2) {
            flushBuffer();
            //
        }

        @Override
        public void flush() throws IOException {
            if (bufferLength != 0) {
                flushBuffer();
            }
            charWriter.flush();
        }

        /**
         * Closes the {@link java.io.Writer character stream} used by this writer.
         * The content is also flushed if needed before closing.
         *
         * @throws IOException if an I/O error occurs
         */
        @Override
        public void close() throws IOException {
            if (state == STATE_1_SINGLE_EMPTY_FIELD) {
                formatSingleEmptyField();
            }
            state = STATE_0_NO_FIELD;
            flush();
            charWriter.close();
        }

        private void formatEmptyComment() throws IOException {
            appendChar(this.comment);
            appendChar(this.eol0);
            if (this.eol1 != NO_SECOND_EOL) {
                appendChar(this.eol1);
            }
        }

        private void formatComment(final CharSequence comment) throws IOException {
            final char cmnt = this.comment;
            final char eol0 = this.eol0;
            final char eol1 = this.eol1;
            final int length = comment.length();

            if (eol1 != NO_SECOND_EOL) {
                appendChar(cmnt);
                char c;
                for (int p = 0; p < length; p++) {
                    c = comment.charAt(p);
                    if (c == eol0 || c == eol1) {
                        appendChar(eol0);
                        appendChar(eol1);
                        appendChar(cmnt);
                        // skip second EOL
                        if (p + 1 < length && comment.charAt(p + 1) == eol1) {
                            p++;
                        }
                    } else {
                        appendChar(c);
                    }
                }
                appendChar(eol0);
                appendChar(eol1);
            } else {
                appendChar(cmnt);
                char c;
                for (int p = 0; p < length; p++) {
                    c = comment.charAt(p);
                    if (c == eol0) {
                        appendChar(eol0);
                        appendChar(cmnt);
                    } else {
                        appendChar(c);
                    }
                }
                appendChar(eol0);
            }
        }

        private void formatSingleEmptyField() throws IOException {
            appendChar(quote);
            appendChar(quote);
        }

        private void formatEmptyField() throws IOException {
            appendChar(delimiter);
        }

        private void formatEmptyQuotedField() throws IOException {
            appendChar(delimiter);
            appendChar(quote);
            appendChar(quote);
        }

        private void formatField(final CharSequence field, final int quoting, final boolean firstField) throws IOException {
            if (!firstField) {
                appendChar(delimiter);
            }
            final char quote = this.quote;
            switch (quoting) {
                case QUOTING_NONE:
                    appendChars(field);
                    break;
                case QUOTING_PARTIAL:
                    appendChar(quote);
                    appendChars(field);
                    appendChar(quote);
                    break;
                case QUOTING_FULL:
                    appendChar(quote);
                    final int length = field.length();
                    char c;
                    for (int p = 0; p < length; p++) {
                        c = field.charAt(p);
                        if (c == quote) {
                            appendChar(quote);
                        }
                        appendChar(c);
                    }
                    appendChar(quote);
                    break;
            }
        }

        private void formatEndOfLine() throws IOException {
            appendChar(eol0);
            if (eol1 != NO_SECOND_EOL) {
                appendChar(eol1);
            }
        }

        private int parseQuoting(final CharSequence field) {
            final char delimiter = this.delimiter;
            final char quote = this.quote;
            final char eol0 = this.eol0;
            final char eol1 = this.eol1;
            final int length = field.length();

            if (eol1 != NO_SECOND_EOL) {
                char c;
                for (int p = 0; p < length; p++) {
                    c = field.charAt(p);
                    if (c == quote) {
                        return QUOTING_FULL;
                    }
                    if (c == delimiter || c == eol0 || c == eol1) {
                        for (p++; p < length; p++) {
                            if (field.charAt(p) == quote) {
                                return QUOTING_FULL;
                            }
                        }
                        return QUOTING_PARTIAL;
                    }
                }
                return QUOTING_NONE;
            } else {
                char c;
                for (int p = 0; p < length; p++) {
                    c = field.charAt(p);
                    if (c == quote) {
                        return QUOTING_FULL;
                    }
                    if (c == delimiter || c == eol0) {
                        for (p++; p < length; p++) {
                            if (field.charAt(p) == quote) {
                                return QUOTING_FULL;
                            }
                        }
                        return QUOTING_PARTIAL;
                    }
                }
                return QUOTING_NONE;
            }
        }

        private int parseForcedQuoting(final CharSequence field) {
            final char quote = this.quote;
            final int length = field.length();

            for (int p = 0; p < length; p++) {
                if (field.charAt(p) == quote) {
                    return QUOTING_FULL;
                }
            }
            return QUOTING_PARTIAL;
        }

        private void flushBuffer() throws IOException {
            charWriter.write(buffer, 0, bufferLength);
            bufferLength = 0;
        }

        private int write(final int length) throws IOException {
            charWriter.write(buffer, 0, length);
            return 1;
        }

        private void appendChar(final char c) throws IOException {
            buffer[bufferLength == buffer.length ? (bufferLength = write(bufferLength)) - 1 : bufferLength++] = c;
        }

        private void appendChars(final CharSequence chars) throws IOException {
            final int charsLength = chars.length();
            if (bufferLength + charsLength >= buffer.length) {
                flushBuffer();
                if (charsLength >= buffer.length) {
                    charWriter.append(chars);
                    return;
                }
            }
            if (chars instanceof String) {
                ((String) chars).getChars(0, charsLength, buffer, bufferLength);
                bufferLength += charsLength;
            } else {
                for (int p = 0; p < charsLength; p++) {
                    buffer[bufferLength++] = chars.charAt(p);
                }
            }
        }

        private static final int QUOTING_NONE = 0;
        private static final int QUOTING_PARTIAL = 1;
        private static final int QUOTING_FULL = 2;

        private static final int STATE_0_NO_FIELD = 0;
        private static final int STATE_1_SINGLE_EMPTY_FIELD = 1;
        private static final int STATE_2_MULTI_FIELD = 2;

        private static final char NO_SECOND_EOL = '\0';
    }

    private static void requireArgument(boolean condition, String format, Object arg) throws IllegalArgumentException {
        if (!condition) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, format, arg));
        }
    }
}
