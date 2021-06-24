package io.smallrye.config;

import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_PROFILE;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.annotation.Priority;

class ConfigSourceInterceptorTest {
    @Test
    void interceptor() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .withSources(KeyValuesConfigSource.config("my.prop", "1234", "override", "4567"))
                .withInterceptors(new LoggingConfigSourceInterceptor(),
                        new OverridingConfigSourceInterceptor())
                .build();

        final String value = config.getValue("my.prop", String.class);
        Assertions.assertEquals("4567", value);
    }

    @Test
    void priority() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .withSources(KeyValuesConfigSource.config("my.prop", "1234"))
                .withInterceptors(new LowerPriorityConfigSourceInterceptor(),
                        new HighPriorityConfigSourceInterceptor())
                .build();

        final String value = config.getValue("my.prop", String.class);
        Assertions.assertEquals("higher", value);
    }

    @Test
    void serviceLoader() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .withSources(KeyValuesConfigSource.config("my.prop.loader", "1234"))
                .addDiscoveredInterceptors()
                .build();

        final String value = config.getValue("my.prop.loader", String.class);
        Assertions.assertEquals("loader", value);
    }

    @Test
    void serviceLoaderAndPriorities() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .withSources(KeyValuesConfigSource.config("my.prop.loader", "1234"))
                .addDiscoveredInterceptors()
                .withInterceptors(new LowerPriorityConfigSourceInterceptor(),
                        new HighPriorityConfigSourceInterceptor())
                .build();

        final String value = config.getValue("my.prop.loader", String.class);
        Assertions.assertEquals("higher", value);
    }

    @Test
    void defaultInterceptors() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .withSources(KeyValuesConfigSource.config("my.prop", "1",
                        "%prof.my.prop", "${%prof.my.prop.profile}",
                        "%prof.my.prop.profile", "2",
                        SMALLRYE_CONFIG_PROFILE, "prof"))
                .addDefaultInterceptors()
                .build();

        assertEquals("2", config.getValue("my.prop", String.class));
    }

    @Test
    void notFailExpansionInactive() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .withSources(KeyValuesConfigSource.config("my.prop", "${expansion}",
                        "%prof.my.prop", "${%prof.my.prop.profile}",
                        "%prof.my.prop.profile", "2",
                        SMALLRYE_CONFIG_PROFILE, "prof"))
                .addDefaultInterceptors()
                .build();

        assertEquals("2", config.getValue("my.prop", String.class));
    }

    @Test
    void names() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .addDefaultInterceptors()
                .withSources(KeyValuesConfigSource.config("my.prop", "1",
                        "%prof.my.prop", "${%prof.my.prop.profile}",
                        "%prof.my.prop.profile", "2",
                        SMALLRYE_CONFIG_PROFILE, "prof"))
                .build();

        Set<String> names = StreamSupport.stream(config.getPropertyNames().spliterator(), false).collect(toSet());
        assertTrue(names.contains("my.prop"));
        assertTrue(names.contains("my.prop.profile"));
        assertFalse(names.contains("%prof.my.prop"));
        assertFalse(names.contains("%prof.my.prop.profile"));
    }

    @Test
    void replaceNames() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withSources(KeyValuesConfigSource.config("my.prop", "1"))
                .withInterceptors(new ConfigSourceInterceptor() {
                    @Override
                    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
                        return context.proceed(name);
                    }

                    @Override
                    public Iterator<String> iterateNames(final ConfigSourceInterceptorContext context) {
                        return Stream.of("another.prop").collect(toSet()).iterator();
                    }
                })
                .build();

        assertEquals("1", config.getRawValue("my.prop"));
        Set<String> names = StreamSupport.stream(config.getPropertyNames().spliterator(), false).collect(toSet());
        assertEquals(1, names.size());
        assertFalse(names.contains("my.prop"));
        assertTrue(names.contains("another.prop"));
    }

    @Test
    void expandActiveProfile() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .addDefaultInterceptors()
                .withSources(KeyValuesConfigSource.config(
                        "app.http.port", "8081",
                        "%dev.app.http.port", "8082",
                        "%test.app.http.port", "8083",
                        "real.port", "${app.http.port}"))
                .withProfile("dev")
                .build();

        assertEquals("8082", config.getRawValue("real.port"));
    }

    private static class LoggingConfigSourceInterceptor implements ConfigSourceInterceptor {
        private static final Logger LOG = Logger.getLogger("io.smallrye.config");

        @Override
        public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
            final ConfigValue configValue = context.proceed(name);
            final String key = configValue.getName();
            final String value = configValue.getValue();
            final String configSource = configValue.getConfigSourceName();

            LOG.infov("The key {0} was loaded from {1} with the value {2}", key, configSource, value);

            return configValue;
        }
    }

    private static class OverridingConfigSourceInterceptor implements ConfigSourceInterceptor {
        @Override
        public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
            return context.proceed("override");
        }
    }

    @Priority(Priorities.APPLICATION + 300)
    private static class HighPriorityConfigSourceInterceptor implements ConfigSourceInterceptor {
        @Override
        public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
            return ConfigValue.builder().withName(name).withValue("higher").build();
        }
    }

    @Priority(Priorities.APPLICATION + 200)
    private static class LowerPriorityConfigSourceInterceptor implements ConfigSourceInterceptor {
        @Override
        public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
            return ConfigValue.builder().withName(name).withValue("lower").build();
        }
    }

}
