package _test;

import lombok.NonNull;
import org.assertj.core.util.URLs;

import java.nio.charset.Charset;

import static java.util.Objects.requireNonNull;

public final class Resources {

    private Resources() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static @NonNull String contentOf(@NonNull Class<?> anchor, @NonNull String name, @NonNull Charset charset) {
        return URLs.contentOf(requireNonNull(anchor.getResource(name)), charset);
    }
}
