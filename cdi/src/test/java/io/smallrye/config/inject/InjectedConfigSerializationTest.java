package io.smallrye.config.inject;

import static io.smallrye.config.inject.KeyValuesConfigSource.config;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
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
class InjectedConfigSerializationTest {

    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(ConfigExtension.class, Config.class).build();

    @Inject
    Config injectedConfig;

    /**
     * Test that when a {@link Config} is serialized and then deserialized, it's replaced with the registered {@code Config} for
     * the current config classloader.
     */
    @Test
    void injectableConfigIsSerializable() {
        Config defaultConfig = ConfigProvider.getConfig();

        Config customConfig = ConfigProviderResolver.instance().getBuilder()
                .forClassLoader(Thread.currentThread().getContextClassLoader())
                .withSources(KeyValuesConfigSource.config("my.prop", "5678"))
                .build();

        // Check that the config works before serialization
        assertEquals("1234", injectedConfig.getValue("my.prop", String.class));
        assertEquals("1234", defaultConfig.getValue("my.prop", String.class));
        assertEquals("5678", customConfig.getValue("my.prop", String.class));

        // Check that the config is serializable
        // Note: test config in InjectionTest has a non-serializable config source
        Config newInjectedConfig = (Config) assertSerializable(injectedConfig);
        Config newDefaultConfig = (Config) assertSerializable(defaultConfig);
        Config newCustomConfig = (Config) assertSerializable(customConfig);

        // Check that the config still works after serialization
        assertEquals("1234", injectedConfig.getValue("my.prop", String.class));
        assertEquals("1234", newInjectedConfig.getValue("my.prop", String.class));
        assertEquals("1234", defaultConfig.getValue("my.prop", String.class));
        assertEquals("1234", newDefaultConfig.getValue("my.prop", String.class));

        // When a config is deserialized, it is replaced by the default config for the current classloader
        // so the custom config does not give the same result after deserialization
        assertEquals("5678", customConfig.getValue("my.prop", String.class));
        assertEquals("1234", newCustomConfig.getValue("my.prop", String.class));
    }

    /**
     * Asserts that an object can be serialized and deserialized
     * 
     * @param o the object
     * @return the resulting object after serializing and deserializing
     */
    private static Object assertSerializable(Object o) {
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        try {
            ObjectOutputStream out = new ObjectOutputStream(bytesOut);
            out.writeObject(o);
        } catch (IOException e) {
            fail("Could not serialize object: " + o, e);
        }

        Object result = null;
        try {
            ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytesOut.toByteArray());
            ObjectInputStream in = new ObjectInputStream(bytesIn);
            result = in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            fail("Could not deserialize object: " + o, e);
        }
        return result;
    }

    @BeforeAll
    static void beforeAll() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("my.prop", "1234"))
                .addDefaultInterceptors()
                .build();
        ConfigProviderResolver.instance().registerConfig(config, Thread.currentThread().getContextClassLoader());
    }

    @AfterAll
    static void afterAll() {
        ConfigProviderResolver.instance().releaseConfig(ConfigProvider.getConfig());
    }
}
