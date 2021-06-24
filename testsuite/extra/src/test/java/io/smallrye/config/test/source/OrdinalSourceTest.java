package io.smallrye.config.test.source;

import static org.testng.Assert.assertEquals;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import jakarta.inject.Inject;

public class OrdinalSourceTest extends Arquillian {
    @Deployment
    public static WebArchive deploy() {
        return ShrinkWrap.create(WebArchive.class, "ProviderTest.war")
                .addAsResource(new StringAsset("config_ordinal=1234"),
                        "META-INF/microprofile-config.properties")
                .as(WebArchive.class);
    }

    @Inject
    Config config;

    @Test
    public void testOrdinal() {
        for (ConfigSource configSource : config.getConfigSources()) {
            if (configSource.getName().startsWith("PropertiesConfigSource")) {
                assertEquals(configSource.getOrdinal(), 1234);
            }
        }
    }
}
