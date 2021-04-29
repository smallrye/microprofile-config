package io.smallrye.config.inject;

import static io.smallrye.config.inject.KeyValuesConfigSource.config;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.Converter;
import org.jboss.weld.exceptions.DeploymentException;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Event;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.ConfigMessages;
import io.smallrye.config.ConfigValidationException;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

/**
 * The Exception messages caused by Config CDI should have the format:<br>
 * 
 * <pre>
 * {@link org.jboss.weld.exceptions.DeploymentException}: SRCFG0200X: < a SmallRye {@link InjectionMessages}> (+ where appropriate) SRCFG0000X: < a SmallRye {@link ConfigMessages}>
 * ...
 * caused by:
 * {@link io.smallrye.config.inject.ConfigInjectionException}: SRCFG0200X: < a SmallRye {@link InjectionMessages}> (+ where appropriate) SRCFG0000X: < a SmallRye {@link ConfigMessages}>
 * ...
 * caused by: (where appropriate)
 * the.root.cause.Exception: SRCFG0000X: < a SmallRye {@link ConfigMessages}>
 * ...
 * </pre>
 * 
 * If n Exception messages are thrown during
 * {@link ConfigExtension#validate(javax.enterprise.inject.spi.AfterDeploymentValidation)},
 * as defined by
 * {@link org.jboss.weld.exceptions.DeploymentException#DeploymentException(List)}
 * the messages will be bundled together as follows:
 * 
 * <pre>
 * {@link org.jboss.weld.exceptions.DeploymentException}: Exception List with n exceptions:
 * Exception 0 :
 * {@link io.smallrye.config.inject.ConfigInjectionException}: SRCFG0200X: < a SmallRye {@link InjectionMessages}> (+ where appropriate) SRCFG0000X: < a SmallRye {@link ConfigMessages}>
 * ...
 * caused by: (where appropriate)
 * the.root.cause.Exception: SRCFG0000X: < a SmallRye {@link ConfigMessages}>
 * ...
 * Exception n :
 * {@link io.smallrye.config.inject.ConfigInjectionException}: SRCFG0200X: < a SmallRye {@link InjectionMessages}> (+ where appropriate) SRCFG0000X: < a SmallRye {@link ConfigMessages}>
 * ...
 * caused by: (where appropriate)
 * the.root.cause.Exception: SRCFG0000X: < a SmallRye {@link ConfigMessages}>
 * </pre>
 * 
 * where each Exception is a supressedException.
 */
public class ValidateInjectionTest {

	@BeforeAll
	static void beforeAll() {
		SmallRyeConfig config = new SmallRyeConfigBuilder()
				.withSources(config("server.host", "localhost", "server.port", "8080"))
				.withSources(config("bad.property.expression.prop", "${missing.prop}"))
				.withConverter(ConvertedValue.class, 100, new ConvertedValueConverter()).addDefaultInterceptors()
				.build();
		ConfigProviderResolver.instance().registerConfig(config, Thread.currentThread().getContextClassLoader());
	}

	@AfterAll
	static void afterAll() {
		ConfigProviderResolver.instance().releaseConfig(ConfigProvider.getConfig());
	}

	static class ConvertedValue {
		private final String value;

		public ConvertedValue(final String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}

		@Override
		public boolean equals(final Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			final ConvertedValue that = (ConvertedValue) o;
			return value.equals(that.value);
		}

		@Override
		public int hashCode() {
			return Objects.hash(value);
		}
	}

	static class ConvertedValueConverter implements Converter<ConvertedValue> {
		@Override
		public ConvertedValue convert(final String value) {
			if (value == null || value.isEmpty()) {
				return null;
			}
			return new ConvertedValue("out");
		}
	}

    @Test
    void missingProperty() throws Exception {
        DeploymentException exception = getDeploymentException(MissingPropertyTest.class);
        assertThat(exception).hasMessage(
                "SRCFG02000: Failed to Inject @ConfigProperty for key missing.property into io.smallrye.config.inject.ValidateInjectionTest$MissingPropertyTest$MissingPropertyBean.missingProp since the config property could not be found in any config source");

        assertThat(exception.getCause()).isInstanceOf(ConfigInjectionException.class);
        assertThat(exception.getCause()).hasMessage(
                "SRCFG02000: Failed to Inject @ConfigProperty for key missing.property into io.smallrye.config.inject.ValidateInjectionTest$MissingPropertyTest$MissingPropertyBean.missingProp since the config property could not be found in any config source");
    }

    @Test
    void emptyProperty() throws Exception {
        DeploymentException exception = getDeploymentException(EmptyPropertyTest.class);
        assertThat(exception)
                .hasMessageStartingWith(
                        "SRCFG02001: Failed to Inject @ConfigProperty for key empty.property into io.smallrye.config.inject.ValidateInjectionTest$EmptyPropertyTest$EmptyPropertyBean.emptyProp SRCFG00040:");

        assertThat(exception.getCause()).isInstanceOf(ConfigInjectionException.class);

        assertThat(exception.getCause().getCause()).isInstanceOf(NoSuchElementException.class);
        assertThat(exception.getCause().getCause()).hasMessage(
                "SRCFG00040: The config property empty.property is defined as the empty String (\"\") which the following Converter considered to be null: io.smallrye.config.Converters$BuiltInConverter");
    }

    @Test
    void badProperty() throws Exception {
        DeploymentException exception = getDeploymentException(BadPropertyTest.class);
        assertThat(exception)
                .hasMessageStartingWith(
                        "SRCFG02001: Failed to Inject @ConfigProperty for key bad.property into io.smallrye.config.inject.ValidateInjectionTest$BadPropertyTest$BadPropertyBean.badProp SRCFG00041:");

        assertThat(exception.getCause()).isInstanceOf(ConfigInjectionException.class);

        assertThat(exception.getCause().getCause()).isInstanceOf(NoSuchElementException.class);
        assertThat(exception.getCause().getCause()).hasMessage(
                "SRCFG00041: The config property bad.property with the config value \",\" was converted to null from the following Converter: io.smallrye.config.Converters$ArrayConverter");
    }

    @Test
    void customConverterMissingProperty() {
        DeploymentException exception = getDeploymentException(CustomConverterMissingPropertyTest.class);
        assertThat(exception).hasMessage(
                "SRCFG02000: Failed to Inject @ConfigProperty for key missing.property into io.smallrye.config.inject.ValidateInjectionTest$CustomConverterMissingPropertyTest$CustomConverterMissingPropertyBean.missingProp since the config property could not be found in any config source");

        assertThat(exception.getCause()).isInstanceOf(ConfigInjectionException.class);
        assertThat(exception.getCause()).hasMessage(
                "SRCFG02000: Failed to Inject @ConfigProperty for key missing.property into io.smallrye.config.inject.ValidateInjectionTest$CustomConverterMissingPropertyTest$CustomConverterMissingPropertyBean.missingProp since the config property could not be found in any config source");
    }

    @Test
    void MissingConverter() {
        DeploymentException exception = getDeploymentException(MissingConverterTest.class);
        assertThat(exception).hasMessageStartingWith(
                "SRCFG02001: Failed to Inject @ConfigProperty for key my.prop into io.smallrye.config.inject.ValidateInjectionTest$MissingConverterTest$MissingConverterBean.myProp SRCFG02007:");

        assertThat(exception.getCause()).isInstanceOf(ConfigInjectionException.class);

        assertThat(exception.getCause().getCause()).isInstanceOf(java.lang.IllegalArgumentException.class);
        assertThat(exception.getCause().getCause()).hasMessage(
                "SRCFG02007: No Converter registered for class io.smallrye.config.inject.ValidateInjectionTest$MissingConverterTest$MyType");
    }

    @Test
    void skipProperties() {
        DeploymentException exception = getDeploymentException(SkipPropertiesTest.class);
        assertThat(exception).hasMessage(
                "SRCFG02000: Failed to Inject @ConfigProperty for key missing.property into io.smallrye.config.inject.ValidateInjectionTest$SkipPropertiesTest$SkipPropertiesBean.missingProp since the config property could not be found in any config source");

        assertThat(exception.getCause()).isInstanceOf(ConfigInjectionException.class);
    }

    @Test
    void constructorUnnamedProperty() {
        DeploymentException exception = getDeploymentException(ConstructorUnnamedPropertyTest.class);
        assertThat(exception).hasMessageStartingWith(
                "SRCFG02001: Failed to Inject @ConfigProperty for key null into io.smallrye.config.inject.ValidateInjectionTest$ConstructorUnnamedPropertyTest$ConstructorUnnamedPropertyBean(String) SRCFG02002:");
        assertThat(exception).hasMessageContaining("ConstructorUnnamedPropertyBean(@ConfigProperty String)");

        assertThat(exception.getCause()).isInstanceOf(ConfigInjectionException.class);

        assertThat(exception.getCause().getCause()).isInstanceOf(IllegalStateException.class);
        assertThat(exception.getCause().getCause()).hasMessage(
                "SRCFG02002: Could not find default name for @ConfigProperty InjectionPoint [BackedAnnotatedParameter] Parameter 1 of [BackedAnnotatedConstructor] "
                        + "@Inject public io.smallrye.config.inject.ValidateInjectionTest$ConstructorUnnamedPropertyTest$ConstructorUnnamedPropertyBean(@ConfigProperty String)");
    }

    @Test
    void methodUnnamedProperty() {
        DeploymentException exception = getDeploymentException(MethodUnnamedPropertyTest.class);
        assertThat(exception).hasMessageStartingWith(
                "SRCFG02001: Failed to Inject @ConfigProperty for key null into io.smallrye.config.inject.ValidateInjectionTest$MethodUnnamedPropertyTest$MethodUnnamedPropertyBean.methodUnnamedProperty(String) SRCFG02002:");
        assertThat(exception).hasMessageContaining("MethodUnnamedPropertyBean.methodUnnamedProperty(@ConfigProperty String)");

        assertThat(exception.getCause()).isInstanceOf(ConfigInjectionException.class);

        assertThat(exception.getCause().getCause()).isInstanceOf(IllegalStateException.class);
        assertThat(exception.getCause().getCause()).hasMessage(
                "SRCFG02002: Could not find default name for @ConfigProperty InjectionPoint [BackedAnnotatedParameter] Parameter 1 of [BackedAnnotatedMethod] "
                        + "@Inject private io.smallrye.config.inject.ValidateInjectionTest$MethodUnnamedPropertyTest$MethodUnnamedPropertyBean.methodUnnamedProperty(@ConfigProperty String)");
    }

    @Test
    void badConfigPropertiesInjection() {
        DeploymentException exception = getDeploymentException(BadConfigPropertiesInjectionTest.class);
        assertThat(exception).hasMessageStartingWith(
                "SRCFG02003: Failed to create @ConfigProperties bean with prefix server for class io.smallrye.config.inject.ValidateInjectionTest$BadConfigPropertiesInjectionTest$ServerDetailsBean. Configuration validation failed");
        assertThat(exception).hasMessageContaining(
                "java.lang.IllegalArgumentException: SRCFG00039: The config property server.host with the config value \"localhost\" threw an Exception whilst being converted");
        assertThat(exception).hasMessageContaining(
                "java.util.NoSuchElementException: SRCFG00014: The config property server.missingPort is required but it could not be found in any config source");

        assertThat(exception.getCause()).isInstanceOf(ConfigInjectionException.class);

        assertThat(exception.getCause().getCause()).isInstanceOf(ConfigValidationException.class);
    }

    @Test
    void badConfigMappingInjection() {
        DeploymentException exception = getDeploymentException(BadConfigMappingInjectionTest.class);
        assertThat(exception).hasMessageStartingWith(
                "SRCFG02004: Failed to create @ConfigMapping bean with prefix server for interface io.smallrye.config.inject.ValidateInjectionTest$BadConfigMappingInjectionTest$ServerDetailsBean. Configuration validation failed:");
        assertThat(exception).hasMessageContaining(
                "java.lang.IllegalArgumentException: SRCFG00039: The config property server.host with the config value \"localhost\" threw an Exception whilst being converted");
        assertThat(exception).hasMessageContaining(
                "java.util.NoSuchElementException: SRCFG00014: The config property server.missing-port is required but it could not be found in any config source");

        assertThat(exception.getCause()).isInstanceOf(ConfigInjectionException.class);

        assertThat(exception.getCause().getCause()).isInstanceOf(ConfigValidationException.class);
    }

    @Test
    void missingPropertyExpressionInjection() {
        DeploymentException exception = getDeploymentException(MissingPropertyExpressionInjectionTest.class);
        assertThat(exception).hasMessageStartingWith(
                "SRCFG02001: Failed to Inject @ConfigProperty for key bad.property.expression.prop into io.smallrye.config.inject.ValidateInjectionTest$MissingPropertyExpressionInjectionTest$MissingPropertyExpressionBean.missingExpressionProp SRCFG00011");
        assertThat(exception.getCause()).isInstanceOf(ConfigInjectionException.class);

        assertThat(exception.getCause().getCause()).isInstanceOf(NoSuchElementException.class);
        assertThat(exception.getCause().getCause()).hasMessage(
                "SRCFG00011: Could not expand value missing.prop in property bad.property.expression.prop");
    }

    @Test
    void missingIndexedPropertiesInjection() {
        DeploymentException exception = getDeploymentException(MissingIndexedPropertiesInjectionTest.class);

        assertThat(exception).hasMessage(
                "SRCFG02000: Failed to Inject @ConfigProperty for key missing.indexed[0] into io.smallrye.config.inject.ValidateInjectionTest$MissingIndexedPropertiesInjectionTest$MissingIndexedPropertiesBean.missingIndexedProp since the config property could not be found in any config source");

        assertThat(exception.getCause()).isInstanceOf(ConfigInjectionException.class);
        assertThat(exception.getCause()).hasMessage(
                "SRCFG02000: Failed to Inject @ConfigProperty for key missing.indexed[0] into io.smallrye.config.inject.ValidateInjectionTest$MissingIndexedPropertiesInjectionTest$MissingIndexedPropertiesBean.missingIndexedProp since the config property could not be found in any config source");
    }

    @Test
    void BadIndexedPropertiesInjection() {
        DeploymentException exception = getDeploymentException(BadIndexedPropertiesInjectionTest.class);

        assertThat(exception)
                .hasMessageStartingWith(
                        "SRCFG02001: Failed to Inject @ConfigProperty for key server.hosts into io.smallrye.config.inject.ValidateInjectionTest$BadIndexedPropertiesInjectionTest$BadIndexedPropertiesBean.badIndexedProp SRCFG00039:");

        assertThat(exception.getCause()).isInstanceOf(ConfigInjectionException.class);

        assertThat(exception.getCause().getCause()).isInstanceOf(IllegalArgumentException.class);
        assertThat(exception.getCause().getCause()).hasMessage(
                "SRCFG00039: The config property server.hosts with the config value \"localhost\" threw an Exception whilst being converted");

        assertThat(exception.getCause().getCause().getCause()).isInstanceOf(NumberFormatException.class);
        assertThat(exception.getCause().getCause().getCause()).hasMessage(
                "SRCFG00029: Expected an integer value, got \"localhost\"");

    }

    @Test
    void manyInjectionExceptions() {
        DeploymentException exception = getDeploymentException(ManyInjectionExceptionsTest.class);
        assertThat(exception).hasMessageStartingWith("Exception List with 6 exceptions:");

        assertThat(exception).hasMessageContaining(
                "SRCFG02000: Failed to Inject @ConfigProperty for key missing.property into io.smallrye.config.inject.ValidateInjectionTest$ManyInjectionExceptionsTest$ManyInjectionExceptionsBean.missingProp since the config property could not be found in any config source");
        assertThat(exception).hasMessageContaining(
                "SRCFG02001: Failed to Inject @ConfigProperty for key empty.property into io.smallrye.config.inject.ValidateInjectionTest$ManyInjectionExceptionsTest$ManyInjectionExceptionsBean.emptyProp SRCFG00040: The config property empty.property is defined as the empty String (\"\") which the following Converter considered to be null: io.smallrye.config.Converters$BuiltInConverter");
        assertThat(exception).hasMessageContaining(
                "SRCFG02001: Failed to Inject @ConfigProperty for key bad.property into io.smallrye.config.inject.ValidateInjectionTest$ManyInjectionExceptionsTest$ManyInjectionExceptionsBean.badProp SRCFG00041: The config property bad.property with the config value \",\" was converted to null from the following Converter: io.smallrye.config.Converters$ArrayConverter");
        String newLineChar = System.lineSeparator();
        assertThat(exception).hasMessageContaining(
                "SRCFG02003: Failed to create @ConfigProperties bean with prefix server for class io.smallrye.config.inject.ValidateInjectionTest$ManyInjectionExceptionsTest$ServerDetailsPropertiesBean. Configuration validation failed:"
                        + newLineChar
                        + "	java.lang.IllegalArgumentException: SRCFG00039: The config property server.host with the config value \"localhost\" threw an Exception whilst being converted");
        assertThat(exception).hasMessageContaining(
                "SRCFG02003: Failed to create @ConfigProperties bean with prefix client for class io.smallrye.config.inject.ValidateInjectionTest$ManyInjectionExceptionsTest$ClientDetailsPropertiesBean. Configuration validation failed:"
                        + newLineChar
                        + "	java.util.NoSuchElementException: SRCFG00014: The config property client.host is required but it could not be found in any config source");
        assertThat(exception).hasMessageContaining(
                "SRCFG02004: Failed to create @ConfigMapping bean with prefix server for interface io.smallrye.config.inject.ValidateInjectionTest$ManyInjectionExceptionsTest$ServerDetailsMappingBean. Configuration validation failed:"
                        + newLineChar
                        + "	java.lang.IllegalArgumentException: SRCFG00039: The config property server.host with the config value \"localhost\" threw an Exception whilst being converted");

        assertThat(exception.getSuppressed()).hasSize(6);
        assertThat(exception.getSuppressed()).allMatch((e) -> e instanceof ConfigInjectionException);
    }

    @Test
    void unqualifiedConfigPropertiesInjection() {
        JupiterTestEngine engine = new JupiterTestEngine();
        LauncherDiscoveryRequest request = request().selectors(selectClass(UnqualifiedConfigPropertiesInjectionTest.class))
                .build();
        EngineExecutionResults results = EngineTestKit.execute(engine, request);
        results.testEvents().failed()
                .assertEventsMatchExactly(finishedWithFailure(instanceOf(IllegalArgumentException.class)));
    }

    private <T> DeploymentException getDeploymentException(Class<T> clazz) {
        JupiterTestEngine engine = new JupiterTestEngine();
        LauncherDiscoveryRequest request = request().selectors(selectClass(clazz)).build();
        EngineExecutionResults results = EngineTestKit.execute(engine, request);

        List<Event> failingEvents = results.testEvents().failed().list();
        assertThat(failingEvents).hasSize(1); // For each Engine execution, there should be only 1 failing event

        Throwable exception = failingEvents.get(0)
                .getPayload(TestExecutionResult.class).get().getThrowable().get();
        assertThat(exception).isInstanceOf(DeploymentException.class); // the exception should be a DeploymentException  

        return (DeploymentException) exception;
    }

    @ExtendWith(WeldJunit5Extension.class)
    static class MissingPropertyTest {
        @WeldSetup
        WeldInitiator weld = WeldInitiator.from(ConfigExtension.class, MissingPropertyBean.class)
                .addBeans()
                .activate(ApplicationScoped.class)
                .inject(this)
                .build();

        @Inject
        MissingPropertyBean missingPropertyBean;

        @Test
        void fail() {
            Assertions.fail();
        }

        @ApplicationScoped
        static class MissingPropertyBean {
            @Inject
            @ConfigProperty(name = "missing.property")
            String missingProp;

        }
    }

    @ExtendWith(WeldJunit5Extension.class)
    static class EmptyPropertyTest {
        @WeldSetup
        WeldInitiator weld = WeldInitiator.from(ConfigExtension.class, EmptyPropertyBean.class)
                .addBeans()
                .activate(ApplicationScoped.class)
                .inject(this)
                .build();

        @Inject
        EmptyPropertyBean emptyPropertyBean;

        @Test
        void fail() {
            Assertions.fail();
        }

        @ApplicationScoped
        static class EmptyPropertyBean {
            @Inject
            @ConfigProperty(name = "empty.property")
            String emptyProp;

        }
    }

    @ExtendWith(WeldJunit5Extension.class)
    static class BadPropertyTest {
        @WeldSetup
        WeldInitiator weld = WeldInitiator.from(ConfigExtension.class, BadPropertyBean.class)
                .addBeans()
                .activate(ApplicationScoped.class)
                .inject(this)
                .build();

        @Inject
        BadPropertyBean badPropertyBean;

        @Test
        void fail() {
            Assertions.fail();
        }

        @ApplicationScoped
        static class BadPropertyBean {
            @Inject
            @ConfigProperty(name = "bad.property") // a single comma: ","
            String[] badProp; // this conversion should fail

        }
    }

    @ExtendWith(WeldJunit5Extension.class)
    static class CustomConverterMissingPropertyTest {
        @WeldSetup
        WeldInitiator weld = WeldInitiator.from(ConfigExtension.class, CustomConverterMissingPropertyBean.class)
                .addBeans()
                .activate(ApplicationScoped.class)
                .inject(this)
                .build();

        @Inject
        CustomConverterMissingPropertyBean customConverterMissingPropertyBean;

        @Test
        void fail() {
            Assertions.fail();
        }

        @ApplicationScoped
        static class CustomConverterMissingPropertyBean {
            @Inject
            @ConfigProperty(name = "missing.property")
            ConvertedValue missingProp;
        }
    }

    @ExtendWith(WeldJunit5Extension.class)
    static class MissingConverterTest {
        @WeldSetup
        WeldInitiator weld = WeldInitiator.from(ConfigExtension.class, MissingConverterBean.class)
                .addBeans()
                .activate(ApplicationScoped.class)
                .inject(this)
                .build();

        @Inject
        MissingConverterBean missingConverterBean;

        @Test
        void fail() {
            Assertions.fail();
        }

        @SuppressWarnings("serial")
        static class MyType implements Converter<MyType> {
            @Override
            public MyType convert(String value) {
                return null;
            }
        }

        @ApplicationScoped
        static class MissingConverterBean {
            @Inject
            @ConfigProperty(name = "my.prop") // exists
            MyType myProp; // MyType is a Converter, which is not registered
        }
    }

    @ExtendWith(WeldJunit5Extension.class)
    static class SkipPropertiesTest {
        @WeldSetup
        WeldInitiator weld = WeldInitiator.from(SortInjectionPointsExtension.class, SkipPropertiesBean.class)
                .addBeans()
                .activate(ApplicationScoped.class)
                .inject(this)
                .build();

        @Inject
        SkipPropertiesBean skipPropertiesBean;

        @Test
        void fail() {
            Assertions.fail();
        }

        @ApplicationScoped
        static class SkipPropertiesBean {
            @Inject
            @ConfigProperty(name = "skip.property")
            ConfigValue skipProp;
            @Inject
            @ConfigProperty(name = "missing.property")
            String missingProp;
        }

        public static class SortInjectionPointsExtension extends ConfigExtension {
            // Make sure we test the skipped property first for the validation to continue.
            @Override
            protected Set<InjectionPoint> getConfigPropertyInjectionPoints() {
                return super.getConfigPropertyInjectionPoints().stream().sorted((o1, o2) -> {
                    if (o1.getMember().getName().equals("skip")) {
                        return -1;
                    }
                    return 0;
                }).collect(Collectors.toCollection(LinkedHashSet::new));
            }
        }
    }

    @ExtendWith(WeldJunit5Extension.class)
    static class ConstructorUnnamedPropertyTest {
        @WeldSetup
        WeldInitiator weld = WeldInitiator.from(ConfigExtension.class, ConstructorUnnamedPropertyBean.class)
                .addBeans()
                .activate(ApplicationScoped.class)
                .inject(this)
                .build();

        @Inject
        ConstructorUnnamedPropertyBean constructorUnnamedPropertiesBean;

        @Test
        void fail() {
            Assertions.fail();
        }

        @ApplicationScoped
        static class ConstructorUnnamedPropertyBean {

            @Inject
            public ConstructorUnnamedPropertyBean(@ConfigProperty final String unnamed) {
            }
        }
    }

    @ExtendWith(WeldJunit5Extension.class)
    static class MethodUnnamedPropertyTest {
        @WeldSetup
        WeldInitiator weld = WeldInitiator.from(ConfigExtension.class, MethodUnnamedPropertyBean.class)
                .addBeans()
                .activate(ApplicationScoped.class)
                .inject(this)
                .build();

        @Inject
        MethodUnnamedPropertyBean bean;

        @Test
        void fail() {
            Assertions.fail();
        }

        @ApplicationScoped
        static class MethodUnnamedPropertyBean {

            @Inject
            private void methodUnnamedProperty(@ConfigProperty String unnamed) {
            }

        }
    }

    @ExtendWith(WeldJunit5Extension.class)
    static class BadConfigPropertiesInjectionTest  {
        @WeldSetup
        WeldInitiator weld = WeldInitiator.from(ConfigExtension.class, ServerDetailsBean.class)
                .addBeans()
                .activate(ApplicationScoped.class)
                .inject(this)
                .build();

        @Test
        void fail() {
            Assertions.fail();
        }

        @Dependent
        @ConfigProperties(prefix = "server")
        public static class ServerDetailsBean {
            public int host; // server.host cannot be converted to type int
            public int missingPort; // server.missingPort doesn't exist
        }
    }

    @ExtendWith(WeldJunit5Extension.class)
    static class BadConfigMappingInjectionTest {
        @WeldSetup
        WeldInitiator weld = WeldInitiator.from(ConfigExtension.class, ServerDetailsBean.class)
                .addBeans()
                .activate(ApplicationScoped.class)
                .inject(this)
                .build();

        @Test
        void fail() {
            Assertions.fail();
        }

        @Dependent
        @ConfigMapping(prefix = "server")
        interface ServerDetailsBean {
            int host(); // server.host cannot be converted to type int

            int missingPort(); // server.missingPort doesn't exist
        }
    }

    @ExtendWith(WeldJunit5Extension.class)
    static class MissingPropertyExpressionInjectionTest {
        @WeldSetup
        WeldInitiator weld = WeldInitiator.from(ConfigExtension.class, MissingPropertyExpressionBean.class)
                .addBeans()
                .activate(ApplicationScoped.class)
                .inject(this)
                .build();

        @Inject
        MissingPropertyExpressionBean bean;

        @Test
        void fail() {
            Assertions.fail();
        }

        @ApplicationScoped
        static class MissingPropertyExpressionBean {
            @Inject
            @ConfigProperty(name = "bad.property.expression.prop") // Exists but contains ${missing.prop} which doesn't 
            String missingExpressionProp;
        }
    }

    @ExtendWith(WeldJunit5Extension.class)
    static class MissingIndexedPropertiesInjectionTest {
        @WeldSetup
        WeldInitiator weld = WeldInitiator.from(ConfigExtension.class, MissingIndexedPropertiesBean.class)
                .addBeans()
                .activate(ApplicationScoped.class)
                .inject(this)
                .build();

        @Inject
        MissingIndexedPropertiesBean bean;

        @Test
        void fail() {
            Assertions.fail();
        }

        @ApplicationScoped
        static class MissingIndexedPropertiesBean {
            @Inject
            @ConfigProperty(name = "missing.indexed[0]") // missing.indexed[0] doesn't exist
            String missingIndexedProp;
        }
    }

    @ExtendWith(WeldJunit5Extension.class)
    static class BadIndexedPropertiesInjectionTest {
        @WeldSetup
        WeldInitiator weld = WeldInitiator.from(ConfigExtension.class, BadIndexedPropertiesBean.class)
                .addBeans()
                .activate(ApplicationScoped.class)
                .inject(this)
                .build();

        @Inject
        BadIndexedPropertiesBean bean;

        @Test
        void fail() {
            Assertions.fail();
        }

        @ApplicationScoped
        static class BadIndexedPropertiesBean {
            @Inject
            @ConfigProperty(name = "server.hosts") // "server.hosts[0]" and "server.hosts[1]" exist, but are Strings not Integers
            List<Integer> badIndexedProp;
        }
    }

    @ExtendWith(WeldJunit5Extension.class)
    static class ManyInjectionExceptionsTest {
        @WeldSetup
        WeldInitiator weld = WeldInitiator
                .from(ConfigExtension.class, ManyInjectionExceptionsBean.class, ServerDetailsPropertiesBean.class,
                        ClientDetailsPropertiesBean.class, ServerDetailsMappingBean.class)
                .addBeans()
                .activate(ApplicationScoped.class)
                .inject(this)
                .build();

        @Inject
        ManyInjectionExceptionsBean bean;

        @Test
        void fail() {
            Assertions.fail();
        }

        @ApplicationScoped
        static class ManyInjectionExceptionsBean {
            @Inject
            @ConfigProperty(name = "missing.property")
            String missingProp;

            @Inject
            @ConfigProperty(name = "empty.property")
            String emptyProp;

            @Inject
            @ConfigProperty(name = "bad.property")
            String[] badProp;
        }

        @Dependent
        @ConfigProperties(prefix = "server")
        public static class ServerDetailsPropertiesBean {
            public int host; // server.host cannot be converted to type int
        }

        @Dependent
        @ConfigProperties(prefix = "client")
        public static class ClientDetailsPropertiesBean {
            public int host; // client.host doesn't exist
        }

        @Dependent
        @ConfigMapping(prefix = "server")
        interface ServerDetailsMappingBean {
            int host(); // server.host cannot be converted to type int
        }
    }

    @ExtendWith(WeldJunit5Extension.class)
    static class UnqualifiedConfigPropertiesInjectionTest {
        @WeldSetup
        WeldInitiator weld = WeldInitiator.from(ConfigExtension.class, ServerDetailsBean.class)
                .addBeans()
                .activate(ApplicationScoped.class)
                .inject(this)
                .build();

        @Inject
        // Unqualified due to @ConfigProperties missing here
        ServerDetailsBean server;

        @Test
        void fail() {
            Assertions.fail();
        }

        @Dependent
        @ConfigProperties(prefix = "server")
        public static class ServerDetailsBean {
            public String host;
            public int port;
        }
    }
}