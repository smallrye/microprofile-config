package io.smallrye.config.examples.expansion;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ExampleExpansionBean {
    @Inject
    @ConfigProperty(name = "my.prop")
    private String myProperty;

    public String getMyProperty() {
        return myProperty;
    }
}
