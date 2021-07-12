package io.smallrye.config.source.toml;

import static io.smallrye.config.AbstractLocationConfigSourceFactory.SMALLRYE_LOCATIONS;
import static java.util.stream.StreamSupport.stream;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

class TomlLocationConfigSourceFactoryTest {
    @Test
    void systemFile() {
        SmallRyeConfig config = buildConfig("./src/test/resources/additional.toml");

        assertEquals("1234", config.getRawValue("my.prop"));
        assertNull(config.getRawValue("more.prop"));
        assertEquals(1, countSources(config));
    }

    @Test
    void systemFolder() {
        SmallRyeConfig config = buildConfig("./src/test/resources");

        assertEquals("1234", config.getRawValue("my.prop"));
        assertEquals(2, countSources(config));
    }

    private static SmallRyeConfig buildConfig(String... locations) {
        return new SmallRyeConfigBuilder()
                .addDiscoveredSources()
                .addDefaultInterceptors()
                .withDefaultValue(SMALLRYE_LOCATIONS, String.join(",", locations))
                .build();
    }

    private static int countSources(SmallRyeConfig config) {
        return (int) stream(config.getConfigSources().spliterator(), false).filter(
                TomlConfigSource.class::isInstance).count();
    }
}
