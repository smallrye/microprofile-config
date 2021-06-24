/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;
import org.junit.jupiter.api.Test;

import jakarta.annotation.Priority;

class BuilderReuseTest {
    @Test
    void builderReuse() {
        final SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder();
        builder.addDefaultSources();
        final Config config1 = builder.build();
        final Config config2 = builder.build();
        final Iterable<ConfigSource> cs1 = config1.getConfigSources();
        final Iterable<ConfigSource> cs2 = config2.getConfigSources();
        final Iterator<ConfigSource> it1 = cs1.iterator();
        final Iterator<ConfigSource> it2 = cs2.iterator();
        assertTrue(it1.hasNext() && it2.hasNext());
        assertEquals(it1.next().getClass(), it2.next().getClass());
        assertTrue(it1.hasNext() && it2.hasNext());
        assertEquals(it1.next().getClass(), it2.next().getClass());
        assertTrue(it1.hasNext() || it2.hasNext());
        assertEquals(it1.next().getClass(), it2.next().getClass());
        assertFalse(it1.hasNext() || it2.hasNext());
    }

    @Test
    void converterPriority() {
        final SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder();
        builder.addDefaultSources();

        final Converter<Integer> converter1000 = new IntConverter("converter1000");
        final Converter<Integer> converter2000 = new IntConverter("converter2000");
        final Converter<Integer> converter3000 = new IntConverter("converter3000");

        builder.withConverter(Integer.class, 1000, converter1000); //priority is 1000
        final SmallRyeConfig config1 = builder.build();

        builder.withConverter(Integer.class, 2000, converter2000); //priority is 2000
        final SmallRyeConfig config2 = builder.build();

        builder.withConverter(Integer.class, 3000, converter3000); //priority is 3000
        final SmallRyeConfig config3 = builder.build();

        assertEquals(converter1000, config1.getConverter(Integer.class).get());
        assertEquals(converter2000, config2.getConverter(Integer.class).get());
        assertEquals(converter3000, config3.getConverter(Integer.class).get());
    }

    @Priority(2500)
    private static class IntConverter implements Converter<Integer> {
        private final String name;

        public IntConverter(String name) {
            this.name = name;
        }

        @Override
        public Integer convert(String value) {
            return 1; //conversion value doesn't actually matter for this test
        }

        public String toString() {
            return name;
        }
    }
}
