package io.smallrye.config.util.injection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Iterator;
import java.util.Map;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.smallrye.config.inject.ConfigExtension;
import jakarta.inject.Inject;

/**
 * Testing the injection of a Config source name and the Config Source Map
 * 
 * @author <a href="mailto:phillip.kruger@redhat.com">Phillip Kruger</a>
 */
@ExtendWith(WeldJunit5Extension.class)
class ConfigSourceProviderTest extends InjectionTest {
    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(ConfigExtension.class, ConfigSourceProvider.class)
            .addBeans()
            .inject(this)
            .build();

    @Inject
    @Name("SysPropConfigSource")
    ConfigSource systemPropertiesConfigSource;

    @Inject
    @Name("PropertiesConfigSource[source=memory]")
    ConfigSource propertiesConfigSource;

    @Inject
    @ConfigSourceMap
    Map<String, ConfigSource> configSourceMap;

    @Inject
    Config config;

    @Test
    void injectionByName() {
        assertNotNull(systemPropertiesConfigSource);
        assertFalse(systemPropertiesConfigSource.getProperties().isEmpty());
    }

    @Test
    void injectOfPropertiesFile() {
        assertNotNull(propertiesConfigSource);
        assertFalse(propertiesConfigSource.getProperties().isEmpty());
        Map<String, String> properties = propertiesConfigSource.getProperties();
        assertNotNull(properties);
        assertEquals(1, properties.size());
        assertEquals("testvalue", properties.get("testkey"));
    }

    @Test
    void injectionOfMap() {
        assertNotNull(configSourceMap);
        assertFalse(configSourceMap.isEmpty());
    }

    @Test
    void sourcesOrder() {
        Iterator<ConfigSource> sources = config.getConfigSources().iterator();
        Iterator<ConfigSource> mapSources = configSourceMap.values().iterator();

        while (sources.hasNext()) {
            assertEquals(sources.next(), mapSources.next());
        }

        assertFalse(mapSources.hasNext());
    }
}
