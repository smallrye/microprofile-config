/*
 * Copyright 2017 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.smallrye.config;

import static java.util.Collections.unmodifiableList;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.UnaryOperator;

import javax.annotation.Priority;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

import io.smallrye.converters.config.SmallRyeConfigConverters;
import io.smallrye.converters.config.SmallRyeConfigConvertersBuilder;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class SmallRyeConfig implements Config, Serializable {

    private static final long serialVersionUID = 8138651532357898263L;

    static final Comparator<ConfigSource> CONFIG_SOURCE_COMPARATOR = new Comparator<ConfigSource>() {
        @Override
        public int compare(ConfigSource o1, ConfigSource o2) {
            int res = Integer.compare(o2.getOrdinal(), o1.getOrdinal());
            // if 2 config sources have the same ordinal,
            // provide consistent order by sorting them
            // according to their name.
            return res != 0 ? res : o2.getName().compareTo(o1.getName());
        }
    };

    private final AtomicReference<List<ConfigSource>> configSourcesRef;
    private final SmallRyeConfigConverters converters;
    private final ConfigSourceInterceptorContext interceptorChain;

    SmallRyeConfig(SmallRyeConfigBuilder builder) {
        this.configSourcesRef = buildConfigSources(builder);
        this.converters = buildConverters(builder);
        this.interceptorChain = buildInterceptorChain(builder);
    }

    @Deprecated
    protected SmallRyeConfig(List<ConfigSource> configSources, Map<Type, Converter<?>> converters) {
        this.configSourcesRef = new AtomicReference<>(Collections.unmodifiableList(configSources));
        this.converters = new SmallRyeConfigConvertersBuilder().withConverters(converters).build();
        this.interceptorChain = buildInterceptorChain(new SmallRyeConfigBuilder());
    }

    private AtomicReference<List<ConfigSource>> buildConfigSources(final SmallRyeConfigBuilder builder) {
        final List<ConfigSource> sourcesToBuild = new ArrayList<>(builder.getSources());
        if (builder.isAddDiscoveredSources()) {
            sourcesToBuild.addAll(builder.discoverSources());
        }
        if (builder.isAddDefaultSources()) {
            sourcesToBuild.addAll(builder.getDefaultSources());
        }

        sourcesToBuild.sort(CONFIG_SOURCE_COMPARATOR);
        // wrap all
        final Function<ConfigSource, ConfigSource> sourceWrappersToBuild = builder.getSourceWrappers();
        final ListIterator<ConfigSource> it = sourcesToBuild.listIterator();
        while (it.hasNext()) {
            it.set(sourceWrappersToBuild.apply(it.next()));
        }

        return new AtomicReference<>(unmodifiableList(sourcesToBuild));
    }

    private SmallRyeConfigConverters buildConverters(final SmallRyeConfigBuilder builder) {
        final SmallRyeConfigConvertersBuilder convertersBuilder = builder.getConvertersBuilder();

        if (builder.isAddDiscoveredConverters()) {
            convertersBuilder.withConverters(builder.discoverConverters());
        }

        return convertersBuilder.build();
    }

    private ConfigSourceInterceptorContext buildInterceptorChain(final SmallRyeConfigBuilder builder) {
        final List<ConfigSourceInterceptor> interceptors = new ArrayList<>(builder.getInterceptors());
        if (builder.isAddDiscoveredInterceptors()) {
            interceptors.addAll(builder.discoverInterceptors());
        }

        interceptors.sort((o1, o2) -> {
            final Integer p1 = Optional.ofNullable(o1.getClass().getAnnotation(Priority.class)).map(Priority::value)
                    .orElse(100);
            final Integer p2 = Optional.ofNullable(o2.getClass().getAnnotation(Priority.class)).map(Priority::value)
                    .orElse(100);

            return Integer.compare(p2, p1);
        });

        SmallRyeConfigSourceInterceptorContext current = new SmallRyeConfigSourceInterceptorContext(
                (ConfigSourceInterceptor) (context, name) -> {
                    for (ConfigSource configSource : getConfigSources()) {
                        String value = configSource.getValue(name);
                        if (value != null) {
                            return ConfigValue.builder()
                                    .withName(name)
                                    .withValue(value)
                                    .withConfigSourceName(configSource.getName())
                                    .withConfigSourceOrdinal(configSource.getOrdinal())
                                    .build();
                        }
                    }
                    return null;
                }, null);

        for (int i = interceptors.size() - 1; i >= 0; i--) {
            current = new SmallRyeConfigSourceInterceptorContext(interceptors.get(i), current);
        }

        return current;
    }

    // no @Override
    public <T, C extends Collection<T>> C getValues(String name, Class<T> itemClass, IntFunction<C> collectionFactory) {
        return getValues(name, getConverter(itemClass), collectionFactory);
    }

    public <T, C extends Collection<T>> C getValues(String name, Converter<T> converter, IntFunction<C> collectionFactory) {
        return getValue(name, converters.newCollectionConverter(converter, collectionFactory));
    }

    @Override
    public <T> T getValue(String name, Class<T> aClass) {
        return getValue(name, getConverter(aClass));
    }

    public <T> T getValue(String name, Converter<T> converter) {
        String value = getRawValue(name);
        final T converted;
        if (value != null) {
            converted = converter.convert(value);
        } else {
            try {
                converted = converter.convert("");
            } catch (IllegalArgumentException ignored) {
                throw propertyNotFound(name);
            }
        }
        if (converted == null) {
            throw propertyNotFound(name);
        }
        return converted;
    }

    /**
     * Determine whether the <em>raw value</em> of a configuration property is exactly equal to the expected given
     * value.
     *
     * @param name the property name (must not be {@code null})
     * @param expected the expected value (may be {@code null})
     * @return {@code true} if the values are equal, {@code false} otherwise
     */
    public boolean rawValueEquals(String name, String expected) {
        return Objects.equals(expected, getRawValue(name));
    }

    /**
     * Get the <em>raw value</em> of a configuration property.
     *
     * @param name the property name (must not be {@code null})
     * @return the raw value, or {@code null} if no property value was discovered for the given property name
     */
    public String getRawValue(String name) {
        final ConfigValue configValue = interceptorChain.proceed(name);
        return configValue != null ? configValue.getValue() : null;
    }

    @Override
    public <T> Optional<T> getOptionalValue(String name, Class<T> aClass) {
        return getValue(name, getOptionalConverter(aClass));
    }

    public <T> Optional<T> getOptionalValue(String name, Converter<T> converter) {
        return getValue(name, converters.newOptionalConverter(converter));
    }

    public <T, C extends Collection<T>> Optional<C> getOptionalValues(String name, Class<T> itemClass,
            IntFunction<C> collectionFactory) {
        return getOptionalValues(name, getConverter(itemClass), collectionFactory);
    }

    public <T, C extends Collection<T>> Optional<C> getOptionalValues(String name, Converter<T> converter,
            IntFunction<C> collectionFactory) {
        return getOptionalValue(name, converters.newCollectionConverter(converter, collectionFactory));
    }

    @Override
    public Iterable<String> getPropertyNames() {
        Set<String> names = new HashSet<>();
        for (ConfigSource configSource : getConfigSources()) {
            names.addAll(configSource.getPropertyNames());
        }
        return names;
    }

    @Override
    public Iterable<ConfigSource> getConfigSources() {
        return configSourcesRef.get();
    }

    /**
     * Add a configuration source to the configuration object. The list of configuration sources is re-sorted
     * to insert the new source into the correct position. Configuration source wrappers configured with
     * {@link SmallRyeConfigBuilder#withWrapper(UnaryOperator)} will not be applied.
     *
     * @param configSource the new config source (must not be {@code null})
     */
    public void addConfigSource(ConfigSource configSource) {
        List<ConfigSource> oldVal, newVal;
        int oldSize;
        do {
            oldVal = configSourcesRef.get();
            oldSize = oldVal.size();
            newVal = Arrays.asList(oldVal.toArray(new ConfigSource[oldSize + 1]));
            newVal.set(oldSize, configSource);
            newVal.sort(CONFIG_SOURCE_COMPARATOR);
        } while (!configSourcesRef.compareAndSet(oldVal, unmodifiableList(newVal)));
    }

    public <T> T convert(String value, Class<T> asType) {
        return value != null ? getConverter(asType).convert(value) : null;
    }

    @SuppressWarnings("unchecked")
    private <T> Converter<Optional<T>> getOptionalConverter(Class<T> asType) {
        return converters.getOptionalConverter(asType);
    }

    @SuppressWarnings("unchecked")
    public <T> Converter<T> getConverter(Class<T> asType) {
        return converters.getConverter(asType);
    }

    public SmallRyeConfigConverters getConverters() {
        return converters;
    }

    private static NoSuchElementException propertyNotFound(final String name) {
        return new NoSuchElementException("Property " + name + " not found");
    }
}
