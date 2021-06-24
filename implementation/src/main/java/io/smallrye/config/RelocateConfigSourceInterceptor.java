package io.smallrye.config;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import jakarta.annotation.Priority;

@Priority(Priorities.LIBRARY + 1000)
public class RelocateConfigSourceInterceptor implements ConfigSourceInterceptor {
    private static final long serialVersionUID = 3476637906383945843L;

    private final Function<String, String> mapping;

    public RelocateConfigSourceInterceptor(final Function<String, String> mapping) {
        this.mapping = mapping != null ? mapping : Function.identity();
    }

    public RelocateConfigSourceInterceptor(final Map<String, String> mappings) {
        this((Serializable & Function<String, String>) name -> mappings.getOrDefault(name, name));
    }

    @Override
    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
        final String map = mapping.apply(name);
        ConfigValue configValue = context.proceed(map);
        if (configValue == null && !name.equals(map)) {
            configValue = context.proceed(name);
        }
        return configValue;
    }

    @Override
    public Iterator<String> iterateNames(final ConfigSourceInterceptorContext context) {
        final Set<String> names = new HashSet<>();
        final Iterator<String> namesIterator = context.iterateNames();
        while (namesIterator.hasNext()) {
            final String name = namesIterator.next();
            names.add(name);
            final String mappedName = mapping.apply(name);
            if (mappedName != null) {
                names.add(mappedName);
            }
        }
        return names.iterator();
    }

    @Override
    public Iterator<ConfigValue> iterateValues(final ConfigSourceInterceptorContext context) {
        final Set<ConfigValue> values = new HashSet<>();
        final Iterator<ConfigValue> valuesIterator = context.iterateValues();
        while (valuesIterator.hasNext()) {
            final ConfigValue value = valuesIterator.next();
            values.add(value);
            final String mappedName = mapping.apply(value.getName());
            if (mappedName != null) {
                values.add(ConfigValue.builder()
                        .withName(mappedName)
                        .withValue(value.getValue())
                        .withRawValue(value.getRawValue())
                        .withConfigSourceName(value.getConfigSourceName())
                        .withConfigSourceOrdinal(value.getConfigSourceOrdinal())
                        .withLineNumber(value.getLineNumber())
                        .build());
            }
        }
        return values.iterator();
    }
}
