package io.smallrye.config.test.source;

import static org.testng.Assert.assertEquals;

import org.eclipse.microprofile.config.Config;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import jakarta.inject.Inject;

public class YamlPropertiesTest extends Arquillian {
    @Deployment
    public static WebArchive deploy() {
        return ShrinkWrap.create(WebArchive.class)
                .addAsResource(new StringAsset("my:\n  yaml: 1234\n"), "META-INF/microprofile-config.yaml")
                .as(WebArchive.class);
    }

    @Inject
    Config config;

    @Test
    public void yaml() {
        assertEquals("1234", config.getValue("my.yaml", String.class));
    }
}
