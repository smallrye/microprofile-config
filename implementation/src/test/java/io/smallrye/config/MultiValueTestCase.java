package io.smallrye.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Test;

public class MultiValueTestCase {

    SmallRyeConfig config;

    @Before
    public void setUp() {
        Properties properties = new Properties();
        properties.put("my.pets", "snake,dog,cat,cat");

        config = (SmallRyeConfig) SmallRyeConfigProviderResolver.instance().getBuilder()
                .withSources(new PropertiesConfigSource(properties, "my properties"))
                .build();
    }

    @Test
    public void testGetValuesAsList() {
        List<String> pets = config.getValues("my.pets", String.class, ArrayList::new);
        assertNotNull(pets);
        assertEquals(4, pets.size());
        assertEquals(pets, Arrays.asList("snake", "dog", "cat", "cat"));
    }

    @Test
    public void testGetValuesAsSet() {
        Set<String> pets = config.getValues("my.pets", String.class, HashSet::new);
        assertNotNull(pets);
        assertEquals(3, pets.size());
        assertTrue(pets.contains("snake"));
        assertTrue(pets.contains("dog"));
        assertTrue(pets.contains("cat"));
    }

    @Test
    public void testGetValuesAsSortedSet() {
        Set<String> pets = config.getValues("my.pets", String.class, s -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER));
        assertNotNull(pets);
        assertEquals(3, pets.size());
        assertEquals(new ArrayList(pets), Arrays.asList("cat", "dog", "snake"));
    }
}
