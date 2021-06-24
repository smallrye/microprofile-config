package io.smallrye.config;

import java.util.Map;
import java.util.function.Function;

import jakarta.annotation.Priority;

@Priority(Priorities.LIBRARY + 400)
public class FallbackConfigSourceInterceptor implements ConfigSourceInterceptor {
    private static final long serialVersionUID = 1472367702046537565L;

    private final Function<String, String> mapping;

    public FallbackConfigSourceInterceptor(final Function<String, String> mapping) {
        this.mapping = mapping != null ? mapping : Function.identity();
    }

    public FallbackConfigSourceInterceptor(final Map<String, String> mappings) {
        this(name -> mappings.getOrDefault(name, name));
    }

    @Override
    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
        ConfigValue configValue = context.proceed(name);
        if (configValue == null || configValue.getValue().isEmpty()) {
            final String map = mapping.apply(name);
            if (!name.equals(map)) {
                configValue = context.proceed(map);
            }
        }
        return configValue;
    }
}
