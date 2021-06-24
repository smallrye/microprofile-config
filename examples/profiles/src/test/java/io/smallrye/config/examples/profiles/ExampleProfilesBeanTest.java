package io.smallrye.config.examples.profiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.smallrye.config.inject.ConfigExtension;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ExtendWith(WeldJunit5Extension.class)
class ExampleProfilesBeanTest {
    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(ConfigExtension.class, ExampleProfilesBean.class)
            .addBeans()
            .activate(ApplicationScoped.class)
            .inject(this)
            .build();

    @Inject
    ExampleProfilesBean bean;

    @Test
    void profiles() {
        assertEquals("production", bean.getMyProperty());
    }
}
