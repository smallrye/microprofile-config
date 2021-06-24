package io.smallrye.config.inject;

import static io.smallrye.config.Converters.newCollectionConverter;
import static io.smallrye.config.Converters.newMapConverter;
import static io.smallrye.config.Converters.newOptionalConverter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigValue;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.Converter;

import io.smallrye.config.Converters;
import io.smallrye.config.SecretKeys;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.common.AbstractConverter;
import io.smallrye.config.common.AbstractDelegatingConverter;
import jakarta.enterprise.inject.spi.AnnotatedMember;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.InjectionPoint;

/**
 * Actual implementations for producer method in CDI producer {@link ConfigProducer}.
 *
 * @author <a href="https://github.com/guhilling">Gunnar Hilling</a>
 */
public final class ConfigProducerUtil {

    private ConfigProducerUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves a converted configuration value from {@link Config}.
     *
     * @param injectionPoint the {@link InjectionPoint} where the configuration value will be injected
     * @param config the current {@link Config} instance.
     *
     * @return the converted configuration value.
     */
    public static <T> T getValue(InjectionPoint injectionPoint, Config config) {
        return getValue(getName(injectionPoint), injectionPoint.getType(), getDefaultValue(injectionPoint), config);
    }

    /**
     * Retrieves a converted configuration value from {@link Config}.
     *
     * @param name the name of the configuration property.
     * @param type the {@link Type} of the configuration value to convert.
     * @param defaultValue the default value to use if no configuration value is found.
     * @param config the current {@link Config} instance.
     *
     * @return the converted configuration value.
     */
    public static <T> T getValue(String name, Type type, String defaultValue, Config config) {
        if (name == null) {
            return null;
        }
        if (hasCollection(type)) {
            return convertValues(name, type, getRawValue(name, config), defaultValue, config);
        } else if (hasMap(type)) {
            return convertValues(name, type, defaultValue, config);
        }

        return ((SmallRyeConfig) config).convertValue(name, resolveDefault(getRawValue(name, config), defaultValue),
                resolveConverter(type, config));
    }

    /**
     * Converts the direct sub properties of the given parent property as a Map.
     *
     * @param name the name of the parent property for which we want the direct sub properties as a Map.
     * @param type the {@link Type} of the configuration value to convert.
     * @param defaultValue the default value to convert in case no sub properties could be found.
     * @param config the configuration from which the values are retrieved.
     * @param <T> the expected type of the configuration value to convert.
     *
     * @return the converted configuration value.
     */
    private static <T> T convertValues(String name, Type type, String defaultValue, Config config) {
        return ((SmallRyeConfig) config).convertValue(name, null,
                resolveConverter(type, config, (kC, vC) -> new StaticMapConverter<>(name, defaultValue, config, kC, vC)));
    }

    private static <T> T convertValues(String name, Type type, String rawValue, String defaultValue, Config config) {
        List<String> indexedProperties = ((SmallRyeConfig) config).getIndexedProperties(name);
        // If converting a config property which exists (i.e. myProp[1] = aValue) or no indexed properties exist for the config property
        if (rawValue != null || indexedProperties.isEmpty()) {
            return ((SmallRyeConfig) config).convertValue(name, resolveDefault(rawValue, defaultValue),
                    resolveConverter(type, config));
        }

        BiFunction<Converter<T>, IntFunction<Collection<T>>, Collection<T>> indexedConverter = (itemConverter,
                collectionFactory) -> {
            Collection<T> collection = collectionFactory.apply(indexedProperties.size());
            for (String indexedProperty : indexedProperties) {
                // Never null by the rules of converValue
                collection.add(
                        ((SmallRyeConfig) config).convertValue(indexedProperty, getRawValue(indexedProperty, config),
                                itemConverter));
            }
            return collection;
        };

        return resolveConverterForIndexed(type, config, indexedConverter).convert(" ");
    }

    static ConfigValue getConfigValue(InjectionPoint injectionPoint, Config config) {
        String name = getName(injectionPoint);
        if (name == null) {
            return null;
        }

        ConfigValue configValue = config.getConfigValue(name);
        if (configValue.getRawValue() == null) {
            if (configValue instanceof io.smallrye.config.ConfigValue) {
                configValue = ((io.smallrye.config.ConfigValue) configValue).withValue(getDefaultValue(injectionPoint));
            }
        }

        return configValue;
    }

    static String getRawValue(String name, Config config) {
        return SecretKeys.doUnlocked(() -> config.getConfigValue(name).getValue());
    }

    private static String resolveDefault(String rawValue, String defaultValue) {
        return rawValue != null ? rawValue : defaultValue;
    }

    private static <T> Converter<T> resolveConverter(final Type type, final Config config) {
        return resolveConverter(type, config, Converters::newMapConverter);
    }

    @SuppressWarnings("unchecked")
    private static <T> Converter<T> resolveConverter(final Type type, final Config config,
            final BiFunction<Converter<Object>, Converter<Object>, Converter<Map<Object, Object>>> mapConverterFactory) {
        Class<T> rawType = rawTypeOf(type);
        if (type instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) type;
            Type[] typeArgs = paramType.getActualTypeArguments();
            if (rawType == List.class) {
                return (Converter<T>) newCollectionConverter(resolveConverter(typeArgs[0], config), ArrayList::new);
            } else if (rawType == Set.class) {
                return (Converter<T>) newCollectionConverter(resolveConverter(typeArgs[0], config), HashSet::new);
            } else if (rawType == Map.class) {
                return (Converter<T>) mapConverterFactory.apply(resolveConverter(typeArgs[0], config),
                        resolveConverter(typeArgs[1], config));
            } else if (rawType == Optional.class) {
                return (Converter<T>) newOptionalConverter(resolveConverter(typeArgs[0], config, mapConverterFactory));
            } else if (rawType == Supplier.class) {
                return resolveConverter(typeArgs[0], config, mapConverterFactory);
            }
        } else if (rawType == Map.class) {
            // No parameterized types have been provided so it assumes that a Map of String is expected
            return (Converter<T>) mapConverterFactory.apply(resolveConverter(String.class, config),
                    resolveConverter(String.class, config));
        }
        // just try the raw type
        return config.getConverter(rawType).orElseThrow(() -> InjectionMessages.msg.noRegisteredConverter(rawType));
    }

    /**
     * We need to handle indexed properties in a special way, since a Collection may be wrapped in other converters.
     * The issue is that in the original code the value was retrieve by calling the first converter that will delegate
     * to all the wrapped types until it finally gets the result. For indexed properties, because it requires
     * additional key lookups, a special converter is added to perform the work. This is mostly a workaround, since
     * converters do not have the proper API, and probably should not have to handle this type of logic.
     *
     * @see IndexedCollectionConverter
     */
    @SuppressWarnings("unchecked")
    private static <T> Converter<T> resolveConverterForIndexed(
            final Type type,
            final Config config,
            final BiFunction<Converter<T>, IntFunction<Collection<T>>, Collection<T>> indexedConverter) {

        Class<T> rawType = rawTypeOf(type);
        if (type instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) type;
            Type[] typeArgs = paramType.getActualTypeArguments();
            if (rawType == List.class) {
                return (Converter<T>) new IndexedCollectionConverter<>(resolveConverter(typeArgs[0], config), ArrayList::new,
                        indexedConverter);
            } else if (rawType == Set.class) {
                return (Converter<T>) new IndexedCollectionConverter<>(resolveConverter(typeArgs[0], config), HashSet::new,
                        indexedConverter);
            } else if (rawType == Optional.class) {
                return (Converter<T>) newOptionalConverter(resolveConverterForIndexed(typeArgs[0], config, indexedConverter));
            } else if (rawType == Supplier.class) {
                return resolveConverterForIndexed(typeArgs[0], config, indexedConverter);
            }
        }

        throw new IllegalStateException();
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> rawTypeOf(final Type type) {
        if (type instanceof Class<?>) {
            return (Class<T>) type;
        } else if (type instanceof ParameterizedType) {
            return rawTypeOf(((ParameterizedType) type).getRawType());
        } else if (type instanceof GenericArrayType) {
            return (Class<T>) Array.newInstance(rawTypeOf(((GenericArrayType) type).getGenericComponentType()), 0).getClass();
        } else {
            throw InjectionMessages.msg.noRawType(type);
        }
    }

    /**
     * Indicates whether the given type is a type of Map or is a Supplier or Optional of Map.
     * 
     * @param type the type to check
     * @return {@code true} if the given type is a type of Map or is a Supplier or Optional of Map,
     *         {@code false} otherwise.
     */
    private static boolean hasMap(final Type type) {
        Class<?> rawType = rawTypeOf(type);
        if (rawType == Map.class) {
            return true;
        } else if (type instanceof ParameterizedType) {
            return hasMap(((ParameterizedType) type).getActualTypeArguments()[0]);
        }
        return false;
    }

    private static <T> boolean hasCollection(final Type type) {
        Class<T> rawType = rawTypeOf(type);
        if (type instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) type;
            Type[] typeArgs = paramType.getActualTypeArguments();
            if (rawType == List.class) {
                return true;
            } else if (rawType == Set.class) {
                return true;
            } else {
                return hasCollection(typeArgs[0]);
            }
        }
        return false;
    }

    private static String getName(InjectionPoint injectionPoint) {
        for (Annotation qualifier : injectionPoint.getQualifiers()) {
            if (qualifier.annotationType().equals(ConfigProperty.class)) {
                ConfigProperty configProperty = ((ConfigProperty) qualifier);
                return getConfigKey(injectionPoint, configProperty);
            }
        }
        return null;
    }

    private static String getDefaultValue(InjectionPoint injectionPoint) {
        for (Annotation qualifier : injectionPoint.getQualifiers()) {
            if (qualifier.annotationType().equals(ConfigProperty.class)) {
                String str = ((ConfigProperty) qualifier).defaultValue();
                if (!ConfigProperty.UNCONFIGURED_VALUE.equals(str)) {
                    return str;
                }
                Class<?> rawType = rawTypeOf(injectionPoint.getType());
                if (rawType.isPrimitive()) {
                    if (rawType == char.class) {
                        return null;
                    } else if (rawType == boolean.class) {
                        return "false";
                    } else {
                        return "0";
                    }
                }
                return null;
            }
        }
        return null;
    }

    static String getConfigKey(InjectionPoint ip, ConfigProperty configProperty) {
        String key = configProperty.name();
        if (!key.trim().isEmpty()) {
            return key;
        }
        if (ip.getAnnotated() instanceof AnnotatedMember) {
            AnnotatedMember<?> member = (AnnotatedMember<?>) ip.getAnnotated();
            AnnotatedType<?> declaringType = member.getDeclaringType();
            if (declaringType != null) {
                String[] parts = declaringType.getJavaClass().getCanonicalName().split("\\.");
                StringBuilder sb = new StringBuilder(parts[0]);
                for (int i = 1; i < parts.length; i++) {
                    sb.append(".").append(parts[i]);
                }
                sb.append(".").append(member.getJavaMember().getName());
                return sb.toString();
            }
        }
        throw InjectionMessages.msg.noConfigPropertyDefaultName(ip);
    }

    static final class IndexedCollectionConverter<T, C extends Collection<T>> extends AbstractDelegatingConverter<T, C> {
        private static final long serialVersionUID = 5186940408317652618L;

        private final IntFunction<Collection<T>> collectionFactory;
        private final BiFunction<Converter<T>, IntFunction<Collection<T>>, Collection<T>> indexedConverter;

        public IndexedCollectionConverter(
                final Converter<T> resolveConverter,
                final IntFunction<Collection<T>> collectionFactory,
                final BiFunction<Converter<T>, IntFunction<Collection<T>>, Collection<T>> indexedConverter) {
            super(resolveConverter);

            this.collectionFactory = collectionFactory;
            this.indexedConverter = indexedConverter;
        }

        @Override
        @SuppressWarnings("unchecked")
        public C convert(final String value) throws IllegalArgumentException, NullPointerException {
            return (C) indexedConverter.apply((Converter<T>) getDelegate(), collectionFactory);
        }
    }

    /**
     * A {@code Converter} of a Map that gives the same Map content whatever the value to convert. It actually relies on
     * its parameters to convert the sub properties of a fixed parent property as a Map.
     *
     * @param <K> The type of the keys.
     * @param <V> The type of the values.
     */
    static final class StaticMapConverter<K, V> extends AbstractConverter<Map<K, V>> {
        private static final long serialVersionUID = 402894491607011464L;

        /**
         * The name of the parent property for which we want the direct sub properties as a Map.
         */
        private final String name;
        /**
         * The default value to convert in case no sub properties could be found.
         */
        private final String defaultValue;
        /**
         * The configuration from which the values are retrieved.
         */
        private final Config config;
        /**
         * The converter to use for the keys.
         */
        private final Converter<K> keyConverter;
        /**
         * The converter to use the for values.
         */
        private final Converter<V> valueConverter;

        /**
         * Construct a {@code StaticMapConverter} with the given parameters.
         *
         * @param name the name of the parent property for which we want the direct sub properties as a Map
         * @param defaultValue the default value to convert in case no sub properties could be found
         * @param config the configuration from which the values are retrieved
         * @param keyConverter the converter to use for the keys
         * @param valueConverter the converter to use the for values
         */
        StaticMapConverter(String name, String defaultValue, Config config, Converter<K> keyConverter,
                Converter<V> valueConverter) {
            this.name = name;
            this.defaultValue = defaultValue;
            this.config = config;
            this.keyConverter = keyConverter;
            this.valueConverter = valueConverter;
        }

        /**
         * {@inheritDoc}
         *
         * Gives the sub properties as a Map if they exist, otherwise gives the default value converted with a
         * {@code MapConverter}.
         */
        @Override
        public Map<K, V> convert(String value) throws IllegalArgumentException, NullPointerException {
            Map<K, V> result = getValues(name, config, keyConverter, valueConverter);
            if (result == null && defaultValue != null) {
                result = newMapConverter(keyConverter, valueConverter).convert(defaultValue);
            }
            return result;
        }

        /**
         * @return the content of the direct sub properties as the requested type of Map.
         */
        private static <K, V> Map<K, V> getValues(String name, Config config, Converter<K> keyConverter,
                Converter<V> valueConverter) {
            return SecretKeys.doUnlocked(() -> ((SmallRyeConfig) config).getValuesAsMap(name, keyConverter, valueConverter));
        }
    }
}
