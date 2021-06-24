package io.smallrye.config.test.location;

import static org.testng.Assert.assertEquals;

import org.eclipse.microprofile.config.Config;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import jakarta.inject.Inject;

public class LocationConfigTest extends Arquillian {
    @Deployment
    public static WebArchive deploy() {
        return ShrinkWrap.create(WebArchive.class, "LocationConfigTest.war")
                .addAsResource(new StringAsset("smallrye.config.locations=config.properties,config.yml"),
                        "META-INF/microprofile-config.properties")
                .addAsResource(new StringAsset("my.prop=1234"), "config.properties")
                .addAsResource(new StringAsset("my:\n" +
                        "  yml: 1234\n"), "config.yml")
                .as(WebArchive.class);
    }

    @Inject
    Config config;

    @Test
    public void testLocationConfig() {
        assertEquals(config.getValue("smallrye.config.locations", String.class), "config.properties,config.yml");
        assertEquals(config.getValue("my.prop", String.class), "1234");
        assertEquals(config.getValue("my.yml", String.class), "1234");
    }
}
