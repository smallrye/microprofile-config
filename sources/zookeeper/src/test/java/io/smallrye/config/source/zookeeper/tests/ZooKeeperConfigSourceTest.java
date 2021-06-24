/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.smallrye.config.source.zookeeper.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.TestingServer;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.smallrye.config.inject.ConfigExtension;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Test the ConfigSource
 */
@ExtendWith(WeldJunit5Extension.class)
class ZooKeeperConfigSourceTest {
    private static final Logger logger = Logger.getLogger(ZooKeeperConfigSourceTest.class.getName());

    private static TestingServer testServer;
    private static CuratorFramework curatorClient;

    private final String APPLICATION_ID = "test1";
    private final String PROPERTY_NAME = "some.property";
    private final String ZK_KEY = "/" + APPLICATION_ID + "/" + PROPERTY_NAME;
    private final String PROPERTY_VALUE = "some.value";

    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(ConfigExtension.class, ZooKeeperConfigSourceTest.class)
            .addBeans()
            .activate(ApplicationScoped.class)
            .inject(this)
            .build();

    @Inject
    @ConfigProperty(name = "injected.property", defaultValue = "injected.property.default")
    String injectedProperty;

    @Inject
    @ConfigProperty(name = "injected.int.property", defaultValue = "13")
    int injectedIntProperty;

    @BeforeAll
    static void setUpClass() throws Exception {
        testServer = new TestingServer(2181, true);

        //Add a property that's going to be injected
        curatorClient = CuratorFrameworkFactory.newClient("localhost:2181", new ExponentialBackoffRetry(1000, 3));
        curatorClient.start();
        curatorClient.createContainers("/test1/injected.property");
        curatorClient.setData().forPath("/test1/injected.property", "injected.property.value".getBytes());

        curatorClient.createContainers("/test1/injected.int.property");
        curatorClient.setData().forPath("/test1/injected.int.property", "17".getBytes());
    }

    @AfterAll
    static void tearDownClass() throws Exception {
        curatorClient.close();

        testServer.close();
        testServer.stop();
    }

    @Test
    void testGettingProperty() {
        logger.info("ZooKeeperConfigSourceTest.testGettingProperty");

        Config cfg = ConfigProvider.getConfig();

        //Check that the ZK ConfigSource will work
        assertNotNull(cfg.getValue("io.smallrye.configsource.zookeeper.url", String.class));

        //Check that a property doesn't exist yet
        try {
            cfg.getValue(PROPERTY_NAME, String.class);
            fail("Property " + PROPERTY_NAME + " should not exist");
        } catch (NoSuchElementException ignored) {
        }

        //Check that the optional version of the property is not present
        assertFalse(cfg.getOptionalValue(PROPERTY_NAME, String.class).isPresent());
        //setup the property in ZK
        try {
            curatorClient.createContainers(ZK_KEY);
            curatorClient.setData().forPath(ZK_KEY, PROPERTY_VALUE.getBytes());
        } catch (Exception e) {
            fail("Cannot set property PROPERTY_VALUE directly in Zookeeper");
        }

        //check the property can be optained by a property
        assertEquals(PROPERTY_VALUE, cfg.getValue(PROPERTY_NAME, String.class));

        Set<String> propertyNames = new HashSet<>();
        cfg.getPropertyNames().forEach(propertyNames::add);
        assertTrue(propertyNames.contains(PROPERTY_NAME));
    }

    @Test
    void testInjection() {
        assertEquals("injected.property.value", injectedProperty);
        assertEquals(17, injectedIntProperty);
    }
}
