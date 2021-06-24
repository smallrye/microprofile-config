package io.smallrye.config.inject;

import static io.smallrye.config.inject.KeyValuesConfigSource.config;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ExtendWith(WeldJunit5Extension.class)
class SupplierInjectionTest {
    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(ConfigExtension.class, SupplierBean.class)
            .addBeans()
            .activate(ApplicationScoped.class)
            .inject(this)
            .build();

    @Inject
    SupplierBean supplierBean;

    @Test
    void supplier() {
        assertEquals("1234", supplierBean.getMyProp());
        assertEquals("1234", supplierBean.getSupplierMyProp().get());
        assertEquals(1234, supplierBean.getSupplierInteger().get().intValue());
        assertEquals(1234, supplierBean.getSupplierOptionalInteger().get().get().intValue());
    }

    @Test
    void dynamicSupplier() {
        assertEquals(1, supplierBean.getSupplierDynamic().get().intValue());
        assertEquals(2, supplierBean.getSupplierDynamic().get().intValue());
        assertEquals(3, supplierBean.getSupplierDynamic().get().intValue());
    }

    @ApplicationScoped
    static class SupplierBean {
        @Inject
        @ConfigProperty(name = "my.prop")
        String myProp;
        @Inject
        @ConfigProperty(name = "my.prop")
        Supplier<String> supplierMyProp;
        @Inject
        @ConfigProperty(name = "my.prop")
        Supplier<Integer> supplierInteger;
        @Inject
        @ConfigProperty(name = "my.prop")
        Supplier<Optional<Integer>> supplierOptionalInteger;
        @Inject
        @ConfigProperty(name = "my.counter")
        Supplier<Integer> supplierDynamic;

        String getMyProp() {
            return myProp;
        }

        Supplier<String> getSupplierMyProp() {
            return supplierMyProp;
        }

        Supplier<Integer> getSupplierInteger() {
            return supplierInteger;
        }

        Supplier<Optional<Integer>> getSupplierOptionalInteger() {
            return supplierOptionalInteger;
        }

        Supplier<Integer> getSupplierDynamic() {
            return supplierDynamic;
        }
    }

    @BeforeAll
    static void beforeAll() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(config("my.prop", "1234"))
                .withSources(new ConfigSource() {
                    int counter = 1;

                    @Override
                    public Map<String, String> getProperties() {
                        return new HashMap<>();
                    }

                    @Override
                    public Set<String> getPropertyNames() {
                        return new HashSet<>();
                    }

                    @Override
                    public String getValue(final String propertyName) {
                        return "my.counter".equals(propertyName) ? "" + counter++ : null;
                    }

                    @Override
                    public String getName() {
                        return this.getClass().getName();
                    }
                })
                .addDefaultInterceptors()
                .build();
        ConfigProviderResolver.instance().registerConfig(config, Thread.currentThread().getContextClassLoader());
    }

    @AfterAll
    static void afterAll() {
        ConfigProviderResolver.instance().releaseConfig(ConfigProvider.getConfig());
    }
}
