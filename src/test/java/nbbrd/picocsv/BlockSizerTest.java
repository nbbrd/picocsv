package nbbrd.picocsv;

import _test.QuickReader;
import _test.Sample;
import org.junit.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

public class BlockSizerTest {

    @Test
    public void testGetBlockSizeFromFile() throws IOException {
        Csv.BlockSizer x = new Csv.BlockSizer();

        assertThatNullPointerException()
                .isThrownBy(() -> x.getBlockSize((Path) null));

        assertThat(x.getBlockSize(Files.createTempFile("x", "y")))
                .isEqualTo(Csv.BlockSizer.DEFAULT_BLOCK_BUFFER_SIZE);
    }

    @Test
    public void testGetBlockSizeFromInputStream() throws IOException {
        Csv.BlockSizer x = new Csv.BlockSizer();

        assertThatNullPointerException()
                .isThrownBy(() -> x.getBlockSize((InputStream) null));

        assertThat(x.getBlockSize(new ByteArrayInputStream(new byte[0])))
                .isEqualTo(0);

        assertThat(x.getBlockSize(new ByteArrayInputStream(new byte[100])))
                .isEqualTo(100);
    }

    @Test
    public void testGetBlockSizeFromOutputStream() throws IOException {
        Csv.BlockSizer x = new Csv.BlockSizer();

        assertThatNullPointerException()
                .isThrownBy(() -> x.getBlockSize((OutputStream) null));

        assertThat(x.getBlockSize(new BufferedOutputStream(new ByteArrayOutputStream())))
                .isEqualTo(Csv.BlockSizer.DEFAULT_BUFFER_OUTPUT_STREAM_SIZE);

        assertThat(x.getBlockSize(new ByteArrayOutputStream()))
                .isEqualTo(Csv.BlockSizer.UNKNOWN_SIZE);
    }

    @Test
    public void testOverflow() {
        Csv.BlockSizer x = new Csv.BlockSizer() {
            @Override
            public long getBlockSize(Path file) throws IOException {
                return Integer.MAX_VALUE + 1l;
            }

            @Override
            public long getBlockSize(InputStream stream) throws IOException {
                return Integer.MAX_VALUE + 1l;
            }

            @Override
            public long getBlockSize(OutputStream stream) throws IOException {
                return Integer.MAX_VALUE + 1l;
            }
        };

        Csv.BlockSizer saved = Csv.BLOCK_SIZER.getAndSet(x);
        try {
            assertThatCode(() -> QuickReader.BYTE_ARRAY.read(QuickReader.VoidParser.noOp(), StandardCharsets.UTF_8, Sample.SIMPLE.getFormat(), Sample.SIMPLE.getContent(), Csv.Parsing.DEFAULT))
                    .doesNotThrowAnyException();
        } finally {
            Csv.BLOCK_SIZER.set(saved);
        }
    }

    @Test
    public void testUnderflow() {
        Csv.BlockSizer x = new Csv.BlockSizer() {
            @Override
            public long getBlockSize(Path file) throws IOException {
                return 0;
            }

            @Override
            public long getBlockSize(InputStream stream) throws IOException {
                return 0;
            }

            @Override
            public long getBlockSize(OutputStream stream) throws IOException {
                return 0;
            }
        };

        Csv.BlockSizer saved = Csv.BLOCK_SIZER.getAndSet(x);
        try {
            assertThatCode(() -> QuickReader.BYTE_ARRAY.read(QuickReader.VoidParser.noOp(), StandardCharsets.UTF_8, Sample.SIMPLE.getFormat(), Sample.SIMPLE.getContent(), Csv.Parsing.DEFAULT))
                    .doesNotThrowAnyException();
        } finally {
            Csv.BLOCK_SIZER.set(saved);
        }
    }
}
