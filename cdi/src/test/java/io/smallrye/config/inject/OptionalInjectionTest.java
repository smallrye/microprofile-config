package io.smallrye.config.inject;

import static io.smallrye.config.inject.KeyValuesConfigSource.config;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import jakarta.inject.Inject;

@ExtendWith(WeldJunit5Extension.class)
class OptionalInjectionTest {
    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(ConfigExtension.class, OptionalInjectionTest.class)
            .addBeans()
            .inject(this)
            .build();

    @Inject
    @ConfigProperty(name = "optional.int.value")
    OptionalInt optionalInt;
    @Inject
    @ConfigProperty(name = "optional.long.value")
    OptionalLong optionalLong;
    @Inject
    @ConfigProperty(name = "optional.double.value")
    OptionalDouble optionalDouble;

    @Test
    void optionalIntInjection() {
        assertTrue(optionalInt.isPresent());
        assertEquals(1, optionalInt.getAsInt());

        assertTrue(optionalLong.isPresent());
        assertEquals(2, optionalLong.getAsLong());

        assertTrue(optionalDouble.isPresent());
        assertEquals(3.3, optionalDouble.getAsDouble(), 0);
    }

    @BeforeAll
    static void beforeAll() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("optional.int.value", "1", "optional.long.value", "2", "optional.double.value", "3.3"))
                .addDefaultInterceptors()
                .build();
        ConfigProviderResolver.instance().registerConfig(config, Thread.currentThread().getContextClassLoader());
    }

    @AfterAll
    static void afterAll() {
        ConfigProviderResolver.instance().releaseConfig(ConfigProvider.getConfig());
    }
}
