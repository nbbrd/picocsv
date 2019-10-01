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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.file.Path;
import java.util.Objects;
import java.util.OptionalInt;

/**
 *
 * @author Philippe Charles
 */
final class BufferSizes {

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
