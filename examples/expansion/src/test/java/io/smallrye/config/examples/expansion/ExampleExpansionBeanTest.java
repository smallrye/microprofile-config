package io.smallrye.config.examples.expansion;

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
class ExampleExpansionBeanTest {
    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(ConfigExtension.class, ExampleExpansionBean.class)
            .addBeans()
            .activate(ApplicationScoped.class)
            .inject(this)
            .build();

    @Inject
    ExampleExpansionBean bean;

    @Test
    void expand() {
        assertEquals("expanded", bean.getMyProperty());
    }
}
