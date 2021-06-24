/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.smallrye.config.test.provider;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

/**
 * Test that configuration is injected into Provider.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
public class ProviderWithListTest extends Arquillian {
    @Deployment
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap
                .create(JavaArchive.class, "ProviderTest.jar")
                .addClasses(ProviderWithListTest.class, Email.class, ProviderBeanWithList.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(new StringAsset(
                        "objectIds=a,b,c\nnumbers=4,5,6"), "microprofile-config.properties")
                .as(JavaArchive.class);
        return ShrinkWrap
                .create(WebArchive.class, "ProviderTest.war")
                .addAsLibrary(testJar);
    }

    @Inject
    ProviderBeanWithList bean;

    @Test
    public void testProvider() {
        Provider<List<String>> provider = bean.objectdIds;
        assertNotNull(provider);
        List<String> stuff = provider.get();
        assertNotNull(stuff);
        assertEquals(stuff.size(), 3);
        assertEquals(stuff.get(0), "a");
        assertEquals(stuff.get(1), "b");
        assertEquals(stuff.get(2), "c");

        Provider<List<Integer>> numberProvider = bean.numbers;
        assertNotNull(numberProvider);
        List<Integer> numbers = numberProvider.get();
        assertNotNull(numbers);
        assertEquals(numbers.size(), 3);
        assertEquals((int) numbers.get(0), 4);
    }
}
