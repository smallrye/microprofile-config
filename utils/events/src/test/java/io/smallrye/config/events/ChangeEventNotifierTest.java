package io.smallrye.config.events;

import java.util.Optional;

import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.smallrye.config.events.regex.RegexFilter;
import io.smallrye.config.inject.ConfigExtension;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/**
 * Testing that the events fire correctly
 * 
 * @author <a href="mailto:phillip.kruger@redhat.com">Phillip Kruger</a>
 */
@ExtendWith(WeldJunit5Extension.class)
class ChangeEventNotifierTest {
    @WeldSetup
    WeldInitiator weld = WeldInitiator.from(ConfigExtension.class, ChangeEventNotifier.class)
            .addBeans()
            .activate(ApplicationScoped.class)
            .inject(this)
            .build();

    @Test
    void testNewType() {
        ChangeEvent changeEvent = new ChangeEvent(Type.NEW, "test.key", Optional.empty(), "test value", "TestCase");
        ChangeEventNotifier.getInstance().fire(changeEvent);
    }

    @Test
    void testUpdateType() {
        ChangeEvent changeEvent = new ChangeEvent(Type.UPDATE, "test.key", Optional.of("old value"), "test value", "TestCase");
        ChangeEventNotifier.getInstance().fire(changeEvent);
    }

    @Test
    void testRemoveType() {
        ChangeEvent changeEvent = new ChangeEvent(Type.REMOVE, "test.key", Optional.of("old value"), null, "TestCase");
        ChangeEventNotifier.getInstance().fire(changeEvent);
    }

    @Test
    void testCertainKey() {
        ChangeEvent changeEvent = new ChangeEvent(Type.UPDATE, "some.key", Optional.of("old value"), "test value", "TestCase");
        ChangeEventNotifier.getInstance().fire(changeEvent);
    }

    @Test
    void testCertainKeyAndUpdate() {
        ChangeEvent changeEvent = new ChangeEvent(Type.UPDATE, "some.key", Optional.of("old value"), "test value", "TestCase");
        ChangeEventNotifier.getInstance().fire(changeEvent);
    }

    @Test
    void testCertainSource() {
        ChangeEvent changeEvent = new ChangeEvent(Type.UPDATE, "some.key", Optional.of("old value"), "test value",
                "SomeConfigSource");
        ChangeEventNotifier.getInstance().fire(changeEvent);
    }

    @Test
    void testRegex() {
        ChangeEvent changeEvent = new ChangeEvent(Type.NEW, "testcase.key", Optional.empty(), "test value", "TestCase");
        ChangeEventNotifier.getInstance().fire(changeEvent);
    }

    public void listenForNew(@Observes @TypeFilter(Type.NEW) ChangeEvent changeEvent) {
        Assertions.assertEquals(Type.NEW, changeEvent.getType(), "Expecting new type");
    }

    public void listenForUpdate(@Observes @TypeFilter(Type.UPDATE) ChangeEvent changeEvent) {
        Assertions.assertEquals(Type.UPDATE, changeEvent.getType(), "Expecting update type");
    }

    public void listenForRemove(@Observes @TypeFilter(Type.REMOVE) ChangeEvent changeEvent) {
        Assertions.assertEquals(Type.REMOVE, changeEvent.getType(), "Expecting remove type");
    }

    public void listenForCertainKey(@Observes @KeyFilter("some.key") ChangeEvent changeEvent) {
        Assertions.assertEquals("Expecting certain key", "some.key", changeEvent.getKey());
    }

    public void listenForCertainKeyAndUpdate(
            @Observes @TypeFilter(Type.UPDATE) @KeyFilter("some.key") ChangeEvent changeEvent) {
        Assertions.assertEquals("Expecting certain key", "some.key", changeEvent.getKey());
        Assertions.assertEquals(Type.UPDATE, changeEvent.getType(), "Expecting update type");
    }

    public void listenForCertainSource(@Observes @SourceFilter("SomeConfigSource") ChangeEvent changeEvent) {
        Assertions.assertEquals("Expecting certain config source", "SomeConfigSource", changeEvent.getFromSource());
    }

    @RegexFilter("^testcase\\..+")
    public void listenForKeyPattern(@Observes ChangeEvent changeEvent) {
        Assertions.assertTrue(changeEvent.getKey().startsWith("testcase"), "Expecting key to start with certain value");
    }
}
