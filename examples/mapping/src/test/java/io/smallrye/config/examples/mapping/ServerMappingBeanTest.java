package io.smallrye.config.examples.mapping;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.stream.Stream;

import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.smallrye.config.inject.ConfigExtension;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ExtendWith(WeldJunit5Extension.class)
class ServerMappingBeanTest {
    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(ConfigExtension.class, ServerMappingBean.class, Server.class)
            .addBeans()
            .activate(ApplicationScoped.class)
            .inject(this)
            .build();

    @Inject
    ServerMappingBean bean;

    @Test
    void mapping() {
        final Server server = bean.getServer();
        assertEquals("localhost", server.host());
        assertEquals(8080, server.port());
        assertEquals(Duration.ofSeconds(60), server.timeout());
        assertEquals(200, server.threads());

        assertEquals("login.html", server.form().get("login-page"));
        assertEquals("error.html", server.form().get("error-page"));
        assertEquals("index.html", server.form().get("landing-page"));

        assertTrue(server.ssl().isPresent());
        assertEquals(8443, server.ssl().get().port());
        assertEquals(Stream.of("TLSv1.3", "TLSv1.2").collect(toList()), server.ssl().get().protocols());

        assertFalse(server.proxy().isPresent());

        assertFalse(server.log().enabled());
        assertEquals(".log", server.log().suffix());
        assertTrue(server.log().rotate());
        assertEquals(Server.Log.Pattern.COMMON, server.log().pattern());
    }
}
