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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * Writes CSV files.
 *
 * @author Philippe Charles
 */
public final class CsvWriter implements Closeable {

    /**
     * Creates a new instance from a file.
     *
     * @param file a non-null file
     * @param encoding a non-null encoding
     * @param format a non-null format
     * @return a new writer
     * @throws IllegalArgumentException if the format contains an invalid
     * combination of options
     * @throws IOException if an I/O error occurs
     */
    public static CsvWriter of(Path file, Charset encoding, CsvFormat format) throws IOException {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(encoding, "encoding");
        Objects.requireNonNull(format, "format");

        if (!format.isValid()) {
            throw new IllegalArgumentException("format");
        }

        CharsetEncoder encoder = encoding.newEncoder();
        BufferSizes sizes = BufferSizes.of(file, encoder);
        return make(format, sizes.chars, newWriter(file, encoder, sizes.bytes));
    }

    /**
     * Creates a new instance from a stream.
     *
     * @param stream a non-null stream
     * @param encoding a non-null encoding
     * @param format a non-null format
     * @return a new writer
     * @throws IllegalArgumentException if the format contains an invalid
     * combination of options
     * @throws IOException if an I/O error occurs
     */
    public static CsvWriter of(OutputStream stream, Charset encoding, CsvFormat format) throws IOException {
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
     * Creates a new instance from a writer.
     *
     * @param writer a non-null writer
     * @param format a non-null format
     * @return a new writer
     * @throws IllegalArgumentException if the format contains an invalid
     * combination of options
     * @throws IOException if an I/O error occurs
     */
    public static CsvWriter of(Writer writer, CsvFormat format) throws IOException {
        Objects.requireNonNull(writer, "writer");
        Objects.requireNonNull(format, "format");

        if (!format.isValid()) {
            throw new IllegalArgumentException("format");
        }

        BufferSizes sizes = BufferSizes.of(writer);
        return make(format, sizes.chars, writer);
    }

    private static CsvWriter make(CsvFormat format, OptionalInt charBufferSize, Writer writer) {
        return new CsvWriter(
                Output.of(writer, charBufferSize),
                format.getQuote(), format.getDelimiter(),
                EndOfLineWriter.of(format.getSeparator())
        );
    }

    private final Output output;
    private final char quote;
    private final char delimiter;
    private final EndOfLineWriter endOfLine;
    private boolean requiresDelimiter;

    private CsvWriter(Output output, char quote, char delimiter, EndOfLineWriter endOfLine) {
        this.output = output;
        this.quote = quote;
        this.delimiter = delimiter;
        this.endOfLine = endOfLine;
        this.requiresDelimiter = false;
    }

    /**
     * Writes a new field.
     *
     * @param field a non-null field
     * @throws IOException if an I/O error occurs
     */
    public void writeField(CharSequence field) throws IOException {
        if (pushFields()) {
            output.write(delimiter);
        }

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

    /**
     * Writes an end of line.
     *
     * @throws IOException if an I/O error occurs
     */
    public void writeEndOfLine() throws IOException {
        endOfLine.write(output);
        resetFields();
    }

    @Override
    public void close() throws IOException {
        output.close();
    }

    private boolean pushFields() {
        boolean result = requiresDelimiter;
        requiresDelimiter = true;
        return result;
    }

    private void resetFields() {
        requiresDelimiter = false;
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

    private static final class Output implements Closeable {

        static Output of(Writer writer, OptionalInt bufferSize) {
            return new Output(writer, bufferSize.orElse(BufferSizes.DEFAULT_CHAR_BUFFER_SIZE));
        }

        private final Writer writer;
        private final char[] buffer;
        private int length;

        private Output(Writer writer, int bufferSize) {
            this.writer = writer;
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
                    writer.append(chars);
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
            writer.close();
        }

        private void flush() throws IOException {
            writer.write(buffer, 0, length);
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

    private static Writer newWriter(Path file, CharsetEncoder encoder, OptionalInt byteBufferSize) throws IOException {
        return Channels.newWriter(Files.newByteChannel(file, StandardOpenOption.WRITE), encoder, byteBufferSize.orElse(-1));
    }
}
