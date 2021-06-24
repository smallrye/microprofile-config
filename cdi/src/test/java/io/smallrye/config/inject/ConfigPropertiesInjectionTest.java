package io.smallrye.config.inject;

import static io.smallrye.config.inject.KeyValuesConfigSource.config;
import static org.eclipse.microprofile.config.inject.ConfigProperties.UNCONFIGURED_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.NoSuchElementException;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.smallrye.config.ConfigMessages;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;

@ExtendWith(WeldJunit5Extension.class)
class ConfigPropertiesInjectionTest {
    @WeldSetup
    WeldInitiator weld = WeldInitiator
            .from(ConfigExtension.class, ConfigPropertiesInjectionTest.class, Server.class)
            .inject(this)
            .build();

    @Inject
    @ConfigProperties
    Server server;
    @Inject
    @ConfigProperties(prefix = "cloud")
    Server serverCloud;

    @Inject
    Config config;

    @Test
    void configProperties() {
        assertNotNull(server);
        assertEquals("localhost", server.theHost);
        assertEquals(8080, server.port);

        assertNotNull(serverCloud);
        assertEquals("cloud", serverCloud.theHost);
        assertEquals(9090, serverCloud.port);
    }

    @Test
    void select() {
        Server server = CDI.current().select(Server.class, ConfigProperties.Literal.of(UNCONFIGURED_PREFIX)).get();
        assertNotNull(server);
        assertEquals("localhost", server.theHost);
        assertEquals(8080, server.port);

        Server cloud = CDI.current().select(Server.class, ConfigProperties.Literal.of("cloud")).get();
        assertNotNull(cloud);
        assertEquals("cloud", cloud.theHost);
        assertEquals(9090, cloud.port);
    }

    @Test
    void empty() {
        assertNull(config.getConfigValue("host").getValue());
        assertNull(config.getConfigValue("theHost").getValue());
        assertNull(config.getConfigValue("port").getValue());

        assertThrows(NoSuchElementException.class,
                () -> CDI.current().select(Server.class, ConfigProperties.Literal.of("")).get(),
                () -> ConfigMessages.msg.mappingNotFound(Server.class.getName()).getMessage());
    }

    @Dependent
    @ConfigProperties(prefix = "server")
    public static class Server {
        public String theHost;
        public int port;
    }

    @BeforeAll
    static void beforeAll() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("server.theHost", "localhost", "server.port", "8080"))
                .withSources(config("cloud.theHost", "cloud", "cloud.port", "9090"))
                .addDefaultInterceptors()
                .build();
        ConfigProviderResolver.instance().registerConfig(config, Thread.currentThread().getContextClassLoader());
    }

    @AfterAll
    static void afterAll() {
        ConfigProviderResolver.instance().releaseConfig(ConfigProvider.getConfig());
    }
}
