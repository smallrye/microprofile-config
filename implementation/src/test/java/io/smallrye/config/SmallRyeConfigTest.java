package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.smallrye.config.common.MapBackedConfigSource;

public class SmallRyeConfigTest {
    @Test
    public void addConfigSource() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().addDefaultSources().addDefaultInterceptors().build();
        assertNull(config.getConfigValue("my.prop"));

        config.addConfigSource(KeyValuesConfigSource.config("my.prop", "1"));
        assertEquals("1", config.getRawValue("my.prop"));
    }

    @Test
    public void addMultiple() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().addDefaultSources().addDefaultInterceptors().build();
        assertNull(config.getConfigValue("my.prop"));

        config.addConfigSource(KeyValuesConfigSource.config("my.prop", "1"));
        assertEquals("1", config.getRawValue("my.prop"));

        config.addConfigSource(new MapBackedConfigSource("higher", new HashMap<String, String>() {
            {
                put("my.prop", "2");
            }
        }, 200) {
        });
        assertEquals("2", config.getRawValue("my.prop"));
    }

    @Test
    public void getValues() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(KeyValuesConfigSource.config("my.list", "1,2,3,4"))
                .build();

        List<Integer> values = config.getValues("my.list", Integer.class, ArrayList::new);
        assertEquals(Arrays.asList(1, 2, 3, 4), values);
    }

    @Test
    public void getValuesConverter() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(KeyValuesConfigSource.config("my.list", "1,2,3,4"))
                .build();

        List<Integer> values = config.getValues("my.list", config.getConverter(Integer.class), ArrayList::new);
        assertEquals(Arrays.asList(1, 2, 3, 4), values);
    }

    @Test
    public void getOptionalValues() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(KeyValuesConfigSource.config("my.list", "1,2,3,4"))
                .build();

        Optional<List<Integer>> values = config.getOptionalValues("my.list", Integer.class, ArrayList::new);
        assertTrue(values.isPresent());
        assertEquals(Arrays.asList(1, 2, 3, 4), values.get());
    }

    @Test
    public void getOptionalValuesConverter() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(KeyValuesConfigSource.config("my.list", "1,2,3,4"))
                .build();

        Optional<List<Integer>> values = config.getOptionalValues("my.list", config.getConverter(Integer.class),
                ArrayList::new);
        assertTrue(values.isPresent());
        assertEquals(Arrays.asList(1, 2, 3, 4), values.get());
    }

    @Test
    public void rawValueEquals() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(KeyValuesConfigSource.config("my.prop", "1234"))
                .build();

        assertTrue(config.rawValueEquals("my.prop", "1234"));
        assertFalse(config.rawValueEquals("my.prop", "0"));
    }

    @Test
    public void convert() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().build();

        assertEquals(1234, config.convert("1234", Integer.class).intValue());
        assertNull(config.convert(null, Integer.class));
    }
}
