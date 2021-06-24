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
package io.smallrye.config.test.collections;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import jakarta.inject.Inject;

/**
 * Test that injected collections (array, List, Set) uses configured values.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
public class CollectionWithConfiguredValueTest extends Arquillian {

    @Deployment
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap
                .create(JavaArchive.class, "CollectionWithConfiguredValueTest.jar")
                .addClasses(CollectionWithConfiguredValueTest.class, CollectionBean.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(new StringAsset(
                        "myPets=snake,ox"), "microprofile-config.properties")
                .as(JavaArchive.class);
        return ShrinkWrap
                .create(WebArchive.class, "CollectionWithConfiguredValueTest.war")
                .addAsLibrary(testJar);
    }

    @Inject
    CollectionBean bean;

    @Test
    public void testCollectionWithConfiguredValues() {
        String[] arrayPets = bean.getArrayPets();
        assertNotNull(arrayPets);
        assertEquals(arrayPets.length, 2);
        assertEquals(arrayPets, new String[] { "snake", "ox" });

        List<String> listPets = bean.getListPets();
        assertNotNull(listPets);
        assertEquals(listPets.size(), 2);
        assertEquals(listPets, new ArrayList<>(Arrays.asList("snake", "ox")));

        Set<String> setPets = bean.getSetPets();
        assertNotNull(setPets);
        assertEquals(setPets.size(), 2);
        assertEquals(setPets, new HashSet<>(Arrays.asList("snake", "ox")));
    }

    @Test
    public void testCollectionWithMissingValues() {
        try {
            bean.getListWithEmptyDefault().get();
            fail("Expected NoSuchElementException");
        } catch (NoSuchElementException expected) {
        }

        try {
            bean.getListWithNoDefault().get();
            fail("Expected NoSuchElementException");
        } catch (NoSuchElementException expected) {
        }
    }
}
