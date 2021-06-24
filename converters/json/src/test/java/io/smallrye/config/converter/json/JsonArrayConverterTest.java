package io.smallrye.config.converter.json;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringReader;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.smallrye.config.inject.ConfigExtension;
import io.smallrye.config.inject.ConfigProducer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonReader;

/**
 * Testing the injection of a JsonArray
 * 
 * @author <a href="mailto:phillip.kruger@redhat.com">Phillip Kruger</a>
 */
@ExtendWith(WeldJunit5Extension.class)
class JsonArrayConverterTest {
    @SuppressWarnings("unchecked")
    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(WeldInitiator.createWeld()
            .addExtensions(ConfigExtension.class)
            .addBeanClass(ConfigProducer.class)
            .addBeanClass(JsonArrayConverterTest.class))
            .addBeans()
            .activate(ApplicationScoped.class)
            .inject(this)
            .build();

    @Inject
    @ConfigProperty(name = "someJsonArray")
    JsonArray someValue;

    @Test
    void testInjection() {
        try (JsonReader jsonReader = Json.createReader(new StringReader("[\"value1\",\"value2\",\"value3\"]"))) {
            JsonArray jsonArray = jsonReader.readArray();
            assertEquals(jsonArray, someValue);
        }
    }
}
