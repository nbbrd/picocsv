package de.siegmar.csvbenchmark.util;

import java.io.Reader;

public class InfiniteDataReader extends Reader {

    private final char[] data;
    private int pos;

    public InfiniteDataReader(final String data) {
        this.data = data.toCharArray();
    }

    @Override
    public int read(final char[] cbuf, final int off, final int len) {
        int copied = 0;
        while (copied < len) {
            final int tlen = Math.min(len - copied, data.length - pos);
            System.arraycopy(data, pos, cbuf, off + copied, tlen);
            copied += tlen;
            pos += tlen;

            if (pos == data.length) {
                pos = 0;
            }
        }

        return copied;
    }

    @Override
    public void close() {
        // NOP
    }

}
