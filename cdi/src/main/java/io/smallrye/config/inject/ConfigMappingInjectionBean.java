package io.smallrye.config.inject;

import static io.smallrye.config.inject.SecuritySupport.getContextClassLoader;
import static java.util.Optional.ofNullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperties;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.SmallRyeConfig;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionPoint;

public class ConfigMappingInjectionBean<T> implements Bean<T> {
    private final BeanManager bm;
    private final Class<T> klass;
    private final String prefix;
    private final Set<Annotation> qualifiers = new HashSet<>();

    public ConfigMappingInjectionBean(final BeanManager bm, final AnnotatedType<T> type) {
        this.bm = bm;
        this.klass = type.getJavaClass();
        this.prefix = getPrefixFromType(type);
        this.qualifiers.add(type.isAnnotationPresent(ConfigProperties.class) ? ConfigProperties.Literal.of(prefix)
                : Default.Literal.INSTANCE);
    }

    @Override
    public Class<?> getBeanClass() {
        return klass;
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

    @Override
    public boolean isNullable() {
        return false;
    }

    @Override
    public T create(final CreationalContext<T> creationalContext) {
        InjectionPoint injectionPoint = (InjectionPoint) bm.getInjectableReference(new MetadataInjectionPoint(),
                creationalContext);

        SmallRyeConfig config = (SmallRyeConfig) ConfigProvider.getConfig(getContextClassLoader());
        return config.getConfigMapping(klass, getPrefixFromInjectionPoint(injectionPoint).orElse(prefix));
    }

    @Override
    public void destroy(final T instance, final CreationalContext<T> creationalContext) {

    }

    @Override
    public Set<Type> getTypes() {
        return Collections.singleton(klass);
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return qualifiers;
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return Dependent.class;
    }

    @Override
    public String getName() {
        return this.getClass() + "_" + klass.getName();
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return Collections.emptySet();
    }

    @Override
    public boolean isAlternative() {
        return false;
    }

    public static String getPrefixFromType(final Annotated annotated) {
        final Optional<String> prefixFromConfigMappingClass = ofNullable(annotated.getBaseType()).map(type -> (Class<?>) type)
                .map(c -> c.getAnnotation(ConfigMapping.class))
                .map(ConfigMapping::prefix);

        final Optional<String> prefixFromConfigPropertiesClass = ofNullable(annotated.getBaseType())
                .map(type -> (Class<?>) type)
                .map(c -> c.getAnnotation(ConfigProperties.class))
                .map(ConfigProperties::prefix)
                .filter(prefix -> !prefix.equals(ConfigProperties.UNCONFIGURED_PREFIX));

        return Stream.of(prefixFromConfigMappingClass, prefixFromConfigPropertiesClass)
                .flatMap(s -> s.map(Stream::of).orElseGet(Stream::empty))
                .findFirst()
                .orElse("");
    }

    public static Optional<String> getPrefixFromInjectionPoint(final InjectionPoint injectionPoint) {
        final Optional<String> prefixFromConfigMapping = Optional.ofNullable(injectionPoint.getAnnotated())
                .map(a -> a.getAnnotation(ConfigMapping.class))
                .map(ConfigMapping::prefix)
                .filter(prefix -> !prefix.isEmpty());

        final Optional<String> prefixFromConfigProperties = Optional.ofNullable(injectionPoint.getAnnotated())
                .map(a -> a.getAnnotation(ConfigProperties.class))
                .map(ConfigProperties::prefix)
                .filter(prefix -> !prefix.equals(ConfigProperties.UNCONFIGURED_PREFIX));

        final Optional<String> prefixFromQualifier = injectionPoint.getQualifiers().stream()
                .filter(ConfigProperties.class::isInstance)
                .map(ConfigProperties.class::cast)
                .map(ConfigProperties::prefix)
                .filter(prefix -> !prefix.equals(ConfigProperties.UNCONFIGURED_PREFIX))
                .findFirst();

        return Stream.of(prefixFromConfigMapping, prefixFromConfigProperties, prefixFromQualifier)
                .flatMap(s -> s.map(Stream::of).orElseGet(Stream::empty))
                .findFirst();
    }
}
