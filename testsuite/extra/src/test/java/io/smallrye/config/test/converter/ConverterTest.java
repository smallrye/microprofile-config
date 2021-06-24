/*
 * Copyright 2020 Red Hat, Inc.
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
package io.smallrye.config.test.converter;

import static org.testng.Assert.assertEquals;

import org.eclipse.microprofile.config.spi.Converter;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import jakarta.inject.Inject;

/**
 * Test that a high-priority converter for {@code Integer} will take precedence to built-in converters
 * for both Integer and int types.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2020 Red Hat inc.
 */
public class ConverterTest extends Arquillian {
    @Deployment
    public static WebArchive deploy() {
        return ShrinkWrap
                .create(WebArchive.class, "CollectionWithConfiguredValueTest.war")
                .addClasses(ConverterTest.class, ConverterBean.class)
                .addClass(IntConverter.class)
                .addAsServiceProvider(Converter.class, IntConverter.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject
    ConverterBean bean;

    @Test
    public void testHighPriorityConverterforInt() {
        assertEquals(bean.getInt(), 102);
    }

    @Test
    public void testHighPriorityConverterforInteger() {
        assertEquals(bean.getInteger().intValue(), 102);
    }
}
