/*
 * Copyright 2018 National Bank of Belgium
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
 * See the Licence of the specific language governing permissions and 
 * limitations under the Licence.
 */
package nbbrd.picocsv;

import java.util.Objects;

/**
 * Specifies the format of a CSV file.
 *
 * @author Philippe Charles
 */
public final class CsvFormat {

    /**
     * Predefined format as defined by RFC 4180.
     */
    public static final CsvFormat RFC4180 = CsvFormat
            .builder()
            .separator(NewLine.WINDOWS)
            .delimiter(',')
            .quote('"')
            .build();

    /**
     * Predefined format as alias to RFC 4180.
     */
    public static final CsvFormat DEFAULT = RFC4180;

    public static final CsvFormat EXCEL = CsvFormat
            .builder()
            .separator(NewLine.WINDOWS) // FIXME: ?
            .delimiter(';')
            .quote('"')
            .build();

    private final NewLine separator;
    private final char delimiter;
    private final char quote;

    private CsvFormat(NewLine separator, char delimiter, char quote) {
        this.separator = Objects.requireNonNull(separator, "separator");
        this.delimiter = delimiter;
        this.quote = quote;
    }

    /**
     * Character used to separate lines in the input.
     *
     * @return a non-null line separator
     */
    public NewLine getSeparator() {
        return separator;
    }

    /**
     * Delimiting character of the input.
     *
     * @return
     */
    public char getDelimiter() {
        return delimiter;
    }

    /**
     * Character used to quote strings in the input.
     *
     * @return
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
        final CsvFormat other = (CsvFormat) obj;
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
        return "CsvFormat{" + "separator=" + separator + ", delimiter=" + delimiter + ", quote=" + quote + '}';
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

        public CsvFormat build() {
            return new CsvFormat(separator, delimiter, quote);
        }
    }
}
