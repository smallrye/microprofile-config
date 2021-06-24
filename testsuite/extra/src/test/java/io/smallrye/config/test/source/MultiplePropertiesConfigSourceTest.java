package io.smallrye.config.test.source;

import static org.testng.Assert.assertEquals;

import org.eclipse.microprofile.config.Config;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import jakarta.inject.Inject;

public class MultiplePropertiesConfigSourceTest extends Arquillian {
    @Deployment
    public static WebArchive deploy() {
        JavaArchive sourceOne = ShrinkWrap
                .create(JavaArchive.class, "source-one.jar")
                .addAsManifestResource(new StringAsset("my.prop.one=1234"), "microprofile-config.properties")
                .as(JavaArchive.class);

        JavaArchive sourceTwo = ShrinkWrap
                .create(JavaArchive.class, "source-two.jar")
                .addAsManifestResource(new StringAsset("my.prop.two=1234"), "microprofile-config.properties")
                .as(JavaArchive.class);

        return ShrinkWrap
                .create(WebArchive.class, "sources.war")
                .addAsLibraries(sourceOne, sourceTwo);
    }

    @Inject
    Config config;

    @Test
    public void multiple() {
        assertEquals("1234", config.getValue("my.prop.one", String.class));
        assertEquals("1234", config.getValue("my.prop.two", String.class));
    }
}
