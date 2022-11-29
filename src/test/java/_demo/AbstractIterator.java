package _demo;

import _demo.api2.CsvRowParser;

import java.util.Iterator;
import java.util.NoSuchElementException;

public abstract class AbstractIterator<E> implements Iterator<E> {

    abstract protected E get();

    abstract protected boolean moveNext();

    private enum State {
        COMPUTED, NOT_COMPUTED, DONE
    }

    private State state = State.NOT_COMPUTED;

    @Override
    final public boolean hasNext() {
        switch (state) {
            case COMPUTED:
                return true;
            case DONE:
                return false;
            default:
                if (moveNext()) {
                    state = State.COMPUTED;
                    return true;
                }
                state = State.DONE;
                return false;
        }
    }

    @Override
    final public E next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        state = State.NOT_COMPUTED;
        return get();
    }
}
