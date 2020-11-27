package io.smallrye.config.source.yaml;

import static java.util.Collections.singletonMap;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import io.smallrye.common.constraint.Assert;
import io.smallrye.config.common.MapBackedConfigSource;

/**
 * Yaml config source
 *
 * @author <a href="mailto:phillip.kruger@redhat.com">Phillip Kruger</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class YamlConfigSource extends MapBackedConfigSource {
    private static final long serialVersionUID = -418186029484956531L;

    private static final String NAME_PREFIX = "YamlConfigSource[source=";
    private static final Yaml DUMPER;

    static {
        final DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.FLOW);
        dumperOptions.setDefaultScalarStyle(DumperOptions.ScalarStyle.FOLDED);
        DUMPER = new Yaml(dumperOptions);
    }

    static final int ORDINAL = ConfigSource.DEFAULT_ORDINAL + 10;

    private final Set<String> propertyNames;

    public YamlConfigSource(String name, Map<String, String> source, int ordinal) {
        super(name, source, ordinal, false);
        this.propertyNames = filterIndexedNames(source.keySet());
    }

    public YamlConfigSource(String name, InputStream stream) throws IOException {
        this(name, stream, ORDINAL);
    }

    public YamlConfigSource(URL url) throws IOException {
        this(NAME_PREFIX + url.toString() + "]", url.openStream());
    }

    public YamlConfigSource(URL url, int ordinal) throws IOException {
        this(NAME_PREFIX + url.toString() + "]", url.openStream(), ordinal);
    }

    public YamlConfigSource(String name, InputStream stream, int defaultOrdinal) throws IOException {
        this(name, streamToMap(stream), defaultOrdinal);
    }

    public YamlConfigSource(String name, String source) {
        this(name, source, ORDINAL);
    }

    public YamlConfigSource(String name, String source, int ordinal) {
        this(name, stringToMap(source), ordinal);
    }

    @Override
    public Set<String> getPropertyNames() {
        return propertyNames;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> streamToMap(InputStream inputStream) throws IOException {
        Assert.checkNotNullParam("inputStream", inputStream);
        final Map<String, String> yamlInput = new TreeMap<>();
        try {
            final Iterable<Object> objects = new Yaml().loadAll(inputStream);
            for (Object object : objects) {
                if (object instanceof Map) {
                    yamlInput.putAll(yamlInputToMap((Map<Object, Object>) object));
                }
            }
            inputStream.close();
        } catch (Throwable t) {
            try {
                inputStream.close();
            } catch (Throwable t2) {
                t.addSuppressed(t2);
            }
            throw t;
        }
        return yamlInput;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> stringToMap(String str) {
        final Map<Object, Object> yamlInput = new Yaml().loadAs(str, HashMap.class);
        return yamlInputToMap(yamlInput);
    }

    private static Map<String, String> yamlInputToMap(final Map<Object, Object> yamlInput) {
        final Map<String, String> properties = new TreeMap<>();
        if (yamlInput != null) {
            flattenYaml("", yamlInput, properties);
        }
        return properties;
    }

    @SuppressWarnings("unchecked")
    private static void flattenYaml(String path, Map<Object, Object> source, Map<String, String> target) {
        source.forEach((originalKey, value) -> {
            String key;
            if (originalKey == null) {
                key = "";
            } else {
                key = originalKey.toString();
            }

            if (key.contains(".")) {
                key = "\"" + key + "\"";
            }

            if (!key.isEmpty() && path != null && !path.isEmpty()) {
                key = path + "." + key;
            } else if (path != null && !path.isEmpty()) {
                key = path;
            }

            if (value instanceof String) {
                target.put(key, (String) value);
            } else if (value instanceof Map) {
                flattenMap(key, (Map<Object, Object>) value, target);
                flattenYaml(key, (Map<Object, Object>) value, target);
            } else if (value instanceof List) {
                final List<Object> list = (List<Object>) value;
                flattenList(key, list, target);
                for (int i = 0; i < list.size(); i++) {
                    flattenYaml(key, singletonMap("[" + i + "]", list.get(i)), target);
                }
            } else {
                target.put(key, (value != null ? value.toString() : ""));
            }
        });
    }

    private static void flattenList(String key, List<Object> source, Map<String, String> target) {
        if (source.stream().allMatch(o -> o instanceof String)) {
            target.put(key, source.stream().map(o -> {
                StringBuilder sb = new StringBuilder();
                escapeCommas(sb, o.toString(), 1);
                return sb.toString();
            }).collect(Collectors.joining(",")));
        } else {
            target.put(key, DUMPER.dump(singletonMap(key.substring(key.lastIndexOf(".") + 1), source)));
        }
    }

    private static void flattenMap(String key, Map<Object, Object> source, Map<String, String> target) {
        String lastKey = key.substring(key.lastIndexOf(".") + 1);
        target.put(key, lastKey.contains("[") && lastKey.contains("]") ? DUMPER.dump(source)
                : DUMPER.dump(singletonMap(lastKey, source)));
    }

    private static void escapeCommas(StringBuilder b, String src, int escapeLevel) {
        int cp;
        for (int i = 0; i < src.length(); i += Character.charCount(cp)) {
            cp = src.codePointAt(i);
            if (cp == '\\' || cp == ',') {
                for (int j = 0; j < escapeLevel; j++) {
                    b.append('\\');
                }
            }
            b.appendCodePoint(cp);
        }
    }

    private static Set<String> filterIndexedNames(Set<String> names) {
        final Pattern pattern = Pattern.compile(".*\\[[0-9]+].*");
        return names.stream().filter(s -> !pattern.matcher(s).find()).collect(Collectors.toSet());
    }
}
