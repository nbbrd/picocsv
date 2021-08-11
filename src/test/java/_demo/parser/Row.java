package _demo.parser;

import java.util.Iterator;
import java.util.List;

@lombok.RequiredArgsConstructor
public final class Row implements Iterable<String> {

    private final List<String> fields;
    private final int lineNumber;

    public int getFieldCount() {
        return fields.size();
    }

    public String getField(int index) {
        return fields.get(index);
    }

    @Override
    public Iterator<String> iterator() {
        return fields.iterator();
    }

    public int getLineNumber() {
        return lineNumber;
    }
}
