package io.smallrye.config.inject;

import static io.smallrye.config.inject.KeyValuesConfigSource.config;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.Converter;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.smallrye.config.ConfigValue;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ExtendWith(WeldJunit5Extension.class)
class ConfigInjectionTest {
    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(ConfigExtension.class, ConfigBean.class)
            .addBeans()
            .activate(ApplicationScoped.class)
            .inject(this)
            .build();

    @Inject
    ConfigBean configBean;

    @Test
    void inject() {
        assertEquals(2, configBean.getReasons().size());
        assertEquals("OK", configBean.getReasons().get(200));
        assertEquals("Created", configBean.getReasons().get(201));
        assertEquals(2, configBean.getReasonsSupplier().get().size());
        assertEquals("OK", configBean.getReasonsSupplier().get().get(200));
        assertEquals("Created", configBean.getReasonsSupplier().get().get(201));
        assertFalse(configBean.getReasonsOptional().isPresent());
        assertEquals(2, configBean.getVersions().size());
        assertEquals(new Version(1, "The version 1.2.3"), configBean.getVersions().get("v1"));
        assertEquals(new Version(2, "The version 2.0.0"), configBean.getVersions().get("v2"));
        assertEquals(2, configBean.getVersionsDefault().size());
        assertEquals(new Version(1, "The version 1;2;3"), configBean.getVersionsDefault().get("v1=1;2;3"));
        assertEquals(new Version(2, "The version 2;1;0"), configBean.getVersionsDefault().get("v2=2;1;0"));
        assertEquals(1, configBean.getVersionDefault().size());
        assertEquals(new Version(0, "The version 0"), configBean.getVersionDefault().get("v0"));
        assertEquals(2, configBean.getNumbersList().size());
        assertEquals(4, configBean.getNumbersList().get("even").size());
        assertTrue(configBean.getNumbersList().get("even").containsAll(Arrays.asList(2, 4, 6, 8)));
        assertEquals(5, configBean.getNumbersList().get("odd").size());
        assertTrue(configBean.getNumbersList().get("odd").containsAll(Arrays.asList(1, 3, 5, 7, 9)));
        assertEquals(2, configBean.getNumbersSet().size());
        assertEquals(4, configBean.getNumbersSet().get("even").size());
        assertTrue(configBean.getNumbersSet().get("even").containsAll(Arrays.asList(2, 4, 6, 8)));
        assertEquals(5, configBean.getNumbersSet().get("odd").size());
        assertTrue(configBean.getNumbersSet().get("odd").containsAll(Arrays.asList(1, 3, 5, 7, 9)));
        assertEquals(2, configBean.getNumbersArray().size());
        assertEquals(4, configBean.getNumbersArray().get("even").length);
        assertTrue(Arrays.asList(configBean.getNumbersArray().get("even")).containsAll(Arrays.asList(2, 4, 6, 8)));
        assertEquals(5, configBean.getNumbersArray().get("odd").length);
        assertTrue(Arrays.asList(configBean.getNumbersArray().get("odd")).containsAll(Arrays.asList(1, 3, 5, 7, 9)));
        assertEquals(3, configBean.getNumbers().size());
        assertEquals(1, configBean.getNumbers().get("one"));
        assertEquals(2, configBean.getNumbers().get("two"));
        assertEquals(3, configBean.getNumbers().get("three"));
        assertEquals("1234", configBean.getMyProp());
        assertEquals("1234", configBean.getExpansion());
        assertEquals("12345678", configBean.getSecret());
        assertEquals("5678", configBean.getMyPropProfile());
        assertThrows(SecurityException.class, () -> configBean.getConfig().getValue("secret", String.class),
                "Not allowed to access secret key secret");
    }

    @Test
    void injectConfigValue() {
        final ConfigValue configValue = configBean.getConfigValue();
        assertNotNull(configValue);
        assertEquals("my.prop", configValue.getName());
        assertEquals("1234", configValue.getValue());
        assertEquals("KeyValuesConfigSource", configValue.getConfigSourceName());
        assertEquals(100, configValue.getConfigSourceOrdinal());

        final ConfigValue configValueMissing = configBean.getConfigValueMissing();
        assertNotNull(configValueMissing);
        assertEquals("my.prop.missing", configValueMissing.getName());
        assertEquals("default", configValueMissing.getValue());
        assertNull(configValueMissing.getConfigSourceName());
    }

    @Test
    void optionals() {
        assertFalse(configBean.getUnknown().isPresent());
    }

    @Test
    void converters() {
        assertFalse(configBean.getConvertedValueOptional().isPresent());
    }

    @ApplicationScoped
    static class ConfigBean {
        @Inject
        @ConfigProperty(name = "optional.reasons")
        Optional<Map<Integer, String>> reasonsOptional;
        @Inject
        @ConfigProperty(name = "reasons")
        Supplier<Map<Integer, String>> reasonsSupplier;
        @Inject
        @ConfigProperty(name = "reasons")
        Map<Integer, String> reasons;
        @Inject
        @ConfigProperty(name = "versions")
        Map<String, Version> versions;
        @Inject
        @ConfigProperty(name = "default.versions", defaultValue = "v0.1=0.The version 0;v1\\=1\\;2\\;3=1.The version 1\\;2\\;3;v2\\=2\\;1\\;0=2.The version 2\\;1\\;0")
        Map<String, Version> versionsDefault;
        @Inject
        @ConfigProperty(name = "default.version", defaultValue = "v0=0.The version 0")
        Map<String, Version> versionDefault;
        @Inject
        @ConfigProperty(name = "nums")
        Map<String, Integer> numbers;
        @Inject
        @ConfigProperty(name = "lnums")
        Map<String, List<Integer>> numbersList;
        @Inject
        @ConfigProperty(name = "snums")
        Map<String, Set<Integer>> numbersSet;
        @Inject
        @ConfigProperty(name = "anums")
        Map<String, Integer[]> numbersArray;
        @Inject
        @ConfigProperty(name = "my.prop")
        String myProp;
        @Inject
        @ConfigProperty(name = "expansion")
        String expansion;
        @Inject
        @ConfigProperty(name = "secret")
        String secret;
        @Inject
        @ConfigProperty(name = "my.prop.profile")
        String myPropProfile;
        @Inject
        Config config;
        @Inject
        @ConfigProperty(name = "my.prop")
        ConfigValue configValue;
        @Inject
        @ConfigProperty(name = "my.prop.missing", defaultValue = "default")
        ConfigValue configValueMissing;
        @Inject
        @ConfigProperty(name = "unknown")
        Optional<String> unknown;
        @Inject
        @ConfigProperty(name = "converted")
        Optional<ConvertedValue> convertedValueOptional;

        Optional<Map<Integer, String>> getReasonsOptional() {
            return reasonsOptional;
        }

        Supplier<Map<Integer, String>> getReasonsSupplier() {
            return reasonsSupplier;
        }

        Map<Integer, String> getReasons() {
            return reasons;
        }

        Map<String, Version> getVersions() {
            return versions;
        }

        Map<String, Version> getVersionsDefault() {
            return versionsDefault;
        }

        Map<String, Version> getVersionDefault() {
            return versionDefault;
        }

        Map<String, List<Integer>> getNumbersList() {
            return numbersList;
        }

        Map<String, Set<Integer>> getNumbersSet() {
            return numbersSet;
        }

        Map<String, Integer[]> getNumbersArray() {
            return numbersArray;
        }

        Map<String, Integer> getNumbers() {
            return numbers;
        }

        String getMyProp() {
            return myProp;
        }

        String getExpansion() {
            return expansion;
        }

        String getSecret() {
            return secret;
        }

        String getMyPropProfile() {
            return myPropProfile;
        }

        Config getConfig() {
            return config;
        }

        ConfigValue getConfigValue() {
            return configValue;
        }

        ConfigValue getConfigValueMissing() {
            return configValueMissing;
        }

        Optional<String> getUnknown() {
            return unknown;
        }

        Optional<ConvertedValue> getConvertedValueOptional() {
            return convertedValueOptional;
        }
    }

    @BeforeAll
    static void beforeAll() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("my.prop", "1234", "expansion", "${my.prop}", "secret", "12345678",
                        "mp.config.profile", "prof", "my.prop.profile", "1234", "%prof.my.prop.profile", "5678",
                        "bad.property.expression.prop", "${missing.prop}", "reasons.200", "OK", "reasons.201", "Created",
                        "versions.v1", "1.The version 1.2.3", "versions.v1.2", "1.The version 1.2.0", "versions.v2",
                        "2.The version 2.0.0",
                        "lnums.even", "2,4,6,8", "lnums.odd", "1,3,5,7,9",
                        "snums.even", "2,4,6,8", "snums.odd", "1,3,5,7,9",
                        "anums.even", "2,4,6,8", "anums.odd", "1,3,5,7,9",
                        "nums.one", "1", "nums.two", "2", "nums.three", "3"))
                .withSecretKeys("secret")
                .withConverter(ConvertedValue.class, 100, new ConvertedValueConverter())
                .withConverter(Version.class, 100, new VersionConverter())
                .addDefaultInterceptors()
                .build();
        ConfigProviderResolver.instance().registerConfig(config, Thread.currentThread().getContextClassLoader());
    }

    @AfterAll
    static void afterAll() {
        ConfigProviderResolver.instance().releaseConfig(ConfigProvider.getConfig());
    }

    static class Version {
        int id;
        String name;

        Version(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Version version = (Version) o;
            return id == version.id && Objects.equals(name, version.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name);
        }
    }

    static class VersionConverter implements Converter<Version> {

        @Override
        public Version convert(String value) {
            return new Version(Integer.parseInt(value.substring(0, 1)), value.substring(2));
        }
    }

    static class ConvertedValue {
        private final String value;

        public ConvertedValue(final String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final ConvertedValue that = (ConvertedValue) o;
            return value.equals(that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    static class ConvertedValueConverter implements Converter<ConvertedValue> {
        @Override
        public ConvertedValue convert(final String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }
            return new ConvertedValue("out");
        }
    }
}
