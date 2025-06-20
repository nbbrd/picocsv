package de.siegmar.csvbenchmark.picocsv;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import de.siegmar.csvbenchmark.CsvConstants;
import de.siegmar.csvbenchmark.ICsvReader;
import de.siegmar.csvbenchmark.ICsvWriter;
import de.siegmar.csvbenchmark.util.InfiniteDataReader;
import nbbrd.picocsv.Csv;

public final class Factory {

    private static final Csv.Format FORMAT = Csv.Format.DEFAULT
            .toBuilder()
            .delimiter(CsvConstants.SEPARATOR)
            .separator(Csv.Format.UNIX_SEPARATOR)
            .quote(CsvConstants.DELIMITER)
            .build();

    // Read performances seems highly related to the buffer size
    // Slower: the default buf size (8192) is too big for the benchmark data
    private static final int BUF_SIZE_SLOWER = Csv.DEFAULT_CHAR_BUFFER_SIZE;
    // Equivalent: a random low buf size seems efficient
    private static final int BUF_SIZE_EQUIVALENT = 200;
    // Faster: a perfect buf size (163) aligns the stars and performs very well
    private static final int BUF_SIZE_FASTER = CsvConstants.DATA.length();

    private Factory() {
    }

    public static ICsvReader reader() throws IOException {
        return new ICsvReader() {
            private final Csv.Reader csvReader = Csv.Reader.of(FORMAT, Csv.ReaderOptions.DEFAULT,
                    new InfiniteDataReader(CsvConstants.DATA), BUF_SIZE_SLOWER);

            @Override
            public List<String> readRecord() throws IOException {
                final List<String> result = new ArrayList<>();
                if (csvReader.readLine()) {
                    while (csvReader.readField()) {
                        result.add(csvReader.toString());
                    }
                }
                return result;
            }

            @Override
            public void close() throws IOException {
                csvReader.close();
            }
        };
    }

    public static ICsvWriter writer(final Writer writer) throws IOException {
        return new ICsvWriter() {
            private final Csv.Writer csvWriter = Csv.Writer.of(FORMAT, Csv.WriterOptions.DEFAULT,
                    writer, Csv.DEFAULT_CHAR_BUFFER_SIZE);

            @Override
            public void writeRecord(final List<String> fields) throws IOException {
                for (final String field : fields) {
                    csvWriter.writeField(field);
                }
                csvWriter.writeEndOfLine();
            }

            @Override
            public void close() throws IOException {
                csvWriter.close();
            }
        };
    }
}
