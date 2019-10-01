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

import static _test.QuickReader.newInputFile;
import static _test.QuickReader.newInputStream;
import static _test.QuickWriter.newOutputFile;
import static _test.QuickWriter.newOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import static java.nio.charset.StandardCharsets.UTF_16;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.nio.file.Path;
import java.util.OptionalInt;
import static nbbrd.picocsv.BufferSizes.DEFAULT_BLOCK_BUFFER_SIZE;
import static org.assertj.core.api.Assertions.*;
import org.junit.Test;

/**
 *
 * @author Philippe Charles
 */
public class BufferSizesTest {

    @Test
    public void testFactories() {
        assertThatNullPointerException()
                .isThrownBy(() -> BufferSizes.of((InputStream) null, UTF_8.newDecoder()));

        assertThatNullPointerException()
                .isThrownBy(() -> BufferSizes.of(newInputStream("", UTF_8), null));

        assertThatNullPointerException()
                .isThrownBy(() -> BufferSizes.of((OutputStream) null, UTF_8.newEncoder()));

        assertThatNullPointerException()
                .isThrownBy(() -> BufferSizes.of(newOutputStream(), null));

        assertThatNullPointerException()
                .isThrownBy(() -> BufferSizes.of((Path) null, UTF_8.newDecoder()));

        assertThatNullPointerException()
                .isThrownBy(() -> BufferSizes.of(newInputFile("", UTF_8), (CharsetDecoder) null));

        assertThatNullPointerException()
                .isThrownBy(() -> BufferSizes.of((Path) null, UTF_8.newEncoder()));

        assertThatNullPointerException()
                .isThrownBy(() -> BufferSizes.of(newOutputFile(), (CharsetEncoder) null));
    }

    @Test
    public void testInput() throws IOException {
        assertThat(BufferSizes.of(newInputStream("abc", UTF_8), UTF_8.newDecoder()))
                .isEqualToComparingFieldByField(new BufferSizes(
                        OptionalInt.of(3),
                        OptionalInt.of(3),
                        OptionalInt.of(3))
                );

        assertThat(BufferSizes.of(newInputStream("abc", UTF_16), UTF_16.newDecoder()))
                .isEqualToComparingFieldByField(new BufferSizes(
                        OptionalInt.of(8),
                        OptionalInt.of(8 * 64),
                        OptionalInt.of(8 * 64 / 2))
                );

        assertThat(BufferSizes.of(newInputFile("abc", UTF_8), UTF_8.newDecoder()))
                .isEqualToComparingFieldByField(new BufferSizes(
                        OptionalInt.of(DEFAULT_BLOCK_BUFFER_SIZE),
                        OptionalInt.of(DEFAULT_BLOCK_BUFFER_SIZE * 64),
                        OptionalInt.of(DEFAULT_BLOCK_BUFFER_SIZE * 64))
                );

        assertThat(BufferSizes.of(newInputFile("abc", UTF_16), UTF_16.newDecoder()))
                .isEqualToComparingFieldByField(new BufferSizes(
                        OptionalInt.of(DEFAULT_BLOCK_BUFFER_SIZE),
                        OptionalInt.of(DEFAULT_BLOCK_BUFFER_SIZE * 64),
                        OptionalInt.of(DEFAULT_BLOCK_BUFFER_SIZE * 64 / 2))
                );
    }

    @Test
    public void testOutput() throws IOException {
        assertThat(BufferSizes.of(newOutputStream(), UTF_8.newEncoder()))
                .isEqualToComparingFieldByField(BufferSizes.EMPTY);

        assertThat(BufferSizes.of(newOutputStream(), UTF_16.newEncoder()))
                .isEqualToComparingFieldByField(BufferSizes.EMPTY);

        assertThat(BufferSizes.of(newOutputFile(), UTF_8.newEncoder()))
                .isEqualToComparingFieldByField(new BufferSizes(
                        OptionalInt.of(DEFAULT_BLOCK_BUFFER_SIZE),
                        OptionalInt.of(DEFAULT_BLOCK_BUFFER_SIZE * 64),
                        OptionalInt.of((int) (DEFAULT_BLOCK_BUFFER_SIZE * 64 / 1.1)))
                );

        assertThat(BufferSizes.of(newOutputFile(), UTF_16.newEncoder()))
                .isEqualToComparingFieldByField(new BufferSizes(
                        OptionalInt.of(DEFAULT_BLOCK_BUFFER_SIZE),
                        OptionalInt.of(DEFAULT_BLOCK_BUFFER_SIZE * 64),
                        OptionalInt.of(DEFAULT_BLOCK_BUFFER_SIZE * 64 / 2))
                );
    }

    @Test
    public void testGetSize() throws IOException {
        assertThat(BufferSizes.getSize(OptionalInt.empty(), 123)).isEqualTo(123);
        assertThat(BufferSizes.getSize(OptionalInt.of(-1), 123)).isEqualTo(123);
        assertThat(BufferSizes.getSize(OptionalInt.of(0), 123)).isEqualTo(123);
        assertThat(BufferSizes.getSize(OptionalInt.of(1), 123)).isEqualTo(1);
    }
}
