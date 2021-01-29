package io.smallrye.config.source.toml;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.time.temporal.Temporal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.tomlj.Toml;
import org.tomlj.TomlArray;
import org.tomlj.TomlParseError;
import org.tomlj.TomlParseResult;

import io.smallrye.common.classloader.ClassPathUtils;
import io.smallrye.config.common.MapBackedConfigSource;

public class TomlConfigSource extends MapBackedConfigSource {
    private static final long serialVersionUID = 8252367937790290542L;

    private static final String NAME_PREFIX = "TomlConfigSource[source=%s]";
    private static final int ORDINAL = ConfigSource.DEFAULT_ORDINAL + 5;

    public TomlConfigSource(String name, Map<String, String> source, int ordinal) {
        super(name, source, ordinal);
    }

    public TomlConfigSource(URL url) throws IOException {
        this(url, ORDINAL);
    }

    public TomlConfigSource(URL url, int ordinal) throws IOException {
        this(String.format(NAME_PREFIX, url.toString()), urlToMap(url), ordinal);
    }

    private static Map<String, String> urlToMap(URL url) throws IOException {
        return ClassPathUtils.readStream(url, inputStream -> {
            try {
                return streamToMap(inputStream);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private static Map<String, String> streamToMap(InputStream inputStream) throws IOException {
        TomlParseResult result = Toml.parse(inputStream);
        List<TomlParseError> errors = result.errors();
        if (!errors.isEmpty()) {
            // TODO - Cleanup
            throw new RuntimeException();
        }
        Map<String, String> target = new HashMap<>();
        for (final String key : result.dottedKeySet()) {
            flatten(key, result.get(key), target);
        }
        return target;
    }

    private static void flatten(String key, Object value, Map<String, String> target) {
        if (value instanceof String) {
            target.put(key, (String) value);
        } else if (value instanceof Number) {
            target.put(key, value.toString());
        } else if (value instanceof Boolean) {
            target.put(key, value.toString());
        } else if (value instanceof Temporal) {
            target.put(key, value.toString());
        } else if (value instanceof TomlArray) {
            flattenArray(key, (TomlArray) value, target);
        } else {
            // TODO - Cleanup
            throw new UnsupportedOperationException();
        }
    }

    private static void flattenArray(String path, TomlArray array, Map<String, String> target) {
        // Not supported yet
        if (array.containsArrays()) {
            return;
        }

        List<Object> objects = array.toList();
        for (int i = 0, objectsSize = objects.size(); i < objectsSize; i++) {
            Object object = objects.get(i);
            flatten(path + "[" + i + "]", object, target);
        }
    }
}
