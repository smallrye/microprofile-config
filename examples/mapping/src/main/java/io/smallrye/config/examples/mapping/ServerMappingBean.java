package io.smallrye.config.examples.mapping;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ServerMappingBean {
    @Inject
    Server server;

    public Server getServer() {
        return server;
    }
}
