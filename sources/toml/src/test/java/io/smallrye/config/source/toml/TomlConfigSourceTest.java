package io.smallrye.config.source.toml;

import static org.junit.jupiter.api.Assertions.*;

import java.time.ZonedDateTime;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.WithName;

class TomlConfigSourceTest {
    @Test
    void config() throws Exception {
        TomlConfigSource source = new TomlConfigSource(TomlConfigSourceTest.class.getResource("/example.toml"));

        assertEquals("TOML Example", source.getValue("title"));

        assertEquals("Tom Preston-Werner", source.getValue("owner.name"));
        assertEquals("1979-05-27T07:32:30-08:00", source.getValue("owner.dob"));

        assertEquals("192.168.1.1", source.getValue("database.server"));
        assertEquals("8001", source.getValue("database.ports[0]"));
        assertEquals("8001", source.getValue("database.ports[1]"));
        assertEquals("8002", source.getValue("database.ports[2]"));
        assertEquals("5000", source.getValue("database.connection_max"));
        assertEquals("true", source.getValue("database.enabled"));

        assertEquals("10.0.0.1", source.getValue("servers.alpha.ip"));
        assertEquals("eqdc10", source.getValue("servers.alpha.dc"));
        assertEquals("10.0.0.2", source.getValue("servers.beta.ip"));
        assertEquals("eqdc10", source.getValue("servers.beta.dc"));

        assertEquals("alpha", source.getValue("clients.hosts[0]"));
        assertEquals("omega", source.getValue("clients.hosts[1]"));
    }

    @ConfigMapping
    interface TomlExample {
        String title();

        Owner owner();

        Database database();

        Map<String, Server> servers();

        // List<Client> clients();

        interface Owner {
            String name();

            ZonedDateTime dob();
        }

        interface Database {
            String server();

            // Requires https://github.com/smallrye/smallrye-config/pull/489
            // List<Integer> ports();

            @WithName("connection_max")
            Integer connectionMax();

            Boolean enabled();
        }

        interface Server {
            String ip();

            String dc();
        }

        interface Client {
            // Requires https://github.com/smallrye/smallrye-config/pull/489
            //List<String> hosts();
        }
    }

    @Test
    void mapping() throws Exception {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new TomlConfigSource(TomlConfigSourceTest.class.getResource("/example.toml")))
                .withValidateUnknown(false)
                .withMapping(TomlExample.class)
                .build();

        TomlExample mapping = config.getConfigMapping(TomlExample.class);

        assertEquals("TOML Example", mapping.title());

        assertEquals("Tom Preston-Werner", mapping.owner().name());
        assertEquals(ZonedDateTime.parse("1979-05-27T07:32:30-08:00"), mapping.owner().dob());

        assertEquals("192.168.1.1", mapping.database().server());
        assertEquals(5000, mapping.database().connectionMax());
        assertTrue(mapping.database().enabled());

        assertEquals("10.0.0.1", mapping.servers().get("alpha").ip());
        assertEquals("eqdc10", mapping.servers().get("alpha").dc());
        assertEquals("10.0.0.2", mapping.servers().get("beta").ip());
        assertEquals("eqdc10", mapping.servers().get("beta").dc());
    }
}
