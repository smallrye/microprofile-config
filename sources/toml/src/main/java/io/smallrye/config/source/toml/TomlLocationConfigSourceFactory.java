package io.smallrye.config.source.toml;

import java.io.IOException;
import java.net.URL;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.config.AbstractLocationConfigSourceFactory;

public class TomlLocationConfigSourceFactory extends AbstractLocationConfigSourceFactory {
    @Override
    protected String[] getFileExtensions() {
        return new String[] {
                "toml"
        };
    }

    @Override
    protected ConfigSource loadConfigSource(final URL url, final int ordinal) throws IOException {
        return new TomlConfigSource(url, ordinal);
    }
}
