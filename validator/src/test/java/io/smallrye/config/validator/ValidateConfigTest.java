package io.smallrye.config.validator;

import static io.smallrye.config.validator.KeyValuesConfigSource.config;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.junit.jupiter.api.Test;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.ConfigValidationException;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.WithParentName;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class ValidateConfigTest {
    @Test
    void validateConfigMapping() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withValidator(new BeanValidationConfigValidatorImpl())
                .withSources(config(
                        "server.host", "localhost",
                        "server.port", "8080",
                        "server.log.days", "20",
                        "server.proxy.enable", "true",
                        "server.proxy.timeout", "20",
                        "server.form.login-page", "login.html",
                        "server.form.error-page", "error.html",
                        "server.form.landing-page", "index.html",
                        "server.cors.origins[0].host", "some-server",
                        "server.cors.origins[0].port", "9000",
                        "server.cors.origins[1].host", "localhost",
                        "server.cors.origins[1].port", "1",
                        "server.cors.methods[0]", "GET",
                        "server.cors.methods[1]", "POST",
                        "server.info.name", "Bond",
                        "server.info.code", "007",
                        "server.info.alias[0]", "James",
                        "server.info.admins.root.username", "root"))
                .withMapping(Server.class, "server")
                .build();

        ConfigValidationException validationException = assertThrows(ConfigValidationException.class,
                () -> config.getConfigMapping(Server.class, "server"));
        List<String> validations = new ArrayList<>();
        for (int i = 0; i < validationException.getProblemCount(); i++) {
            validations.add(validationException.getProblem(i).getMessage());
        }
        assertTrue(validations.contains("server.port must be less than or equal to 10"));
        assertTrue(validations.contains("server.log.days must be less than or equal to 15"));
        assertTrue(validations.contains("server.proxy.timeout must be less than or equal to 10"));
        assertTrue(validations.contains("server.cors.origins[0].host size must be between 0 and 10"));
        assertTrue(validations.contains("server.cors.origins[0].port must be less than or equal to 10"));
        assertTrue(validations.contains("server.cors.methods[1] size must be between 0 and 3"));
        assertTrue(validations.contains("server.form.login-page size must be between 0 and 3"));
        assertTrue(validations.contains("server.form.error-page size must be between 0 and 3"));
        assertTrue(validations.contains("server.form.landing-page size must be between 0 and 3"));
        assertTrue(validations.contains("server.info.name size must be between 0 and 3"));
        assertTrue(validations.contains("server.info.code must be less than or equal to 3"));
        assertTrue(validations.contains("server.info.alias[0] size must be between 0 and 3"));
        assertTrue(validations.contains("server.info.admins.root.username size must be between 0 and 3"));
    }

    @Test
    void validateNamingStrategy() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withValidator(new BeanValidationConfigValidatorImpl())
                .withSources(config(
                        "server.the_host", "localhost",
                        "server.the_port", "8080"))
                .withMapping(ServerNamingStrategy.class, "server")
                .build();

        ConfigValidationException validationException = assertThrows(ConfigValidationException.class,
                () -> config.getConfigMapping(ServerNamingStrategy.class, "server"));
        List<String> validations = new ArrayList<>();
        for (int i = 0; i < validationException.getProblemCount(); i++) {
            validations.add(validationException.getProblem(i).getMessage());
        }

        assertTrue(validations.contains("server.the_port must be less than or equal to 10"));
    }

    @Test
    void validateConfigProperties() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withValidator(new BeanValidationConfigValidatorImpl())
                .withMapping(Client.class, "client")
                .build();

        ConfigValidationException validationException = assertThrows(ConfigValidationException.class,
                () -> config.getConfigMapping(Client.class, "client"));
        assertEquals(1, validationException.getProblemCount());
        List<String> validations = new ArrayList<>();
        validations.add(validationException.getProblem(0).getMessage());
        assertTrue(validations.contains("port must be less than or equal to 10"));
    }

    @Test
    void validateParent() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withValidator(new BeanValidationConfigValidatorImpl())
                .withSources(config(
                        "server.host", "localhost",
                        "server.port", "80"))
                .withMapping(ServerParent.class, "server")
                .build();

        ConfigValidationException validationException = assertThrows(ConfigValidationException.class,
                () -> config.getConfigMapping(ServerParent.class, "server"));
        assertEquals("server.port must be greater than or equal to 8000", validationException.getProblem(0).getMessage());
    }

    @ConfigMapping(prefix = "server")
    public interface Server {
        String host();

        @Max(10)
        int port();

        Map<String, @Size(max = 3) String> form();

        Optional<Proxy> proxy();

        Log log();

        Cors cors();

        Info info();

        interface Proxy {
            boolean enable();

            @Max(10)
            int timeout();
        }

        interface Log {
            @Max(15)
            int days();
        }

        interface Cors {
            List<Origin> origins();

            List<@Size(max = 3) String> methods();

            interface Origin {
                @Size(max = 10)
                String host();

                @Max(10)
                int port();
            }
        }

        interface Info {
            Optional<@Size(max = 3) String> name();

            @Max(3)
            OptionalInt code();

            Optional<List<@Size(max = 3) String>> alias();

            Map<String, Admin> admins();

            interface Admin {
                @Size(max = 3)
                String username();
            }
        }
    }

    @ConfigMapping(prefix = "server", namingStrategy = ConfigMapping.NamingStrategy.SNAKE_CASE)
    public interface ServerNamingStrategy {
        String theHost();

        @Max(10)
        int thePort();
    }

    @ConfigProperties(prefix = "client")
    public static class Client {
        public String host = "localhost";
        @Max(10)
        public int port = 8080;
    }

    @ConfigMapping(prefix = "server")
    public interface ServerParent {
        @WithParentName
        Parent parent();

        interface Parent {
            String host();

            @Min(8000)
            int port();
        }
    }
}
