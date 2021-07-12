package io.smallrye.config.source.toml;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

import io.smallrye.config.AbstractLocationConfigSourceLoader;

public class TomlConfigSourceProvider extends AbstractLocationConfigSourceLoader implements ConfigSourceProvider {
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

    @Override
    public List<ConfigSource> getConfigSources(final ClassLoader classLoader) {
        final List<ConfigSource> sources = new ArrayList<>();
        sources.addAll(loadConfigSources("META-INF/microprofile-config.toml", classLoader));
        sources.addAll(loadConfigSources("WEB-INF/classes/META-INF/microprofile-config.toml", classLoader));
        return sources;
    }
}
