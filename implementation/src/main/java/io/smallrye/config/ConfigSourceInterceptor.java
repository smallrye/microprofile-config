package io.smallrye.config;

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;

import io.smallrye.common.annotation.Experimental;

/**
 * The ConfigSourceInterceptor allows to intercept the resolution of a configuration name before the
 * configuration value is resolved by the Config and before any conversion taking place. It can also intercept
 * iteration of configuration names and values.
 * <p>
 *
 * This is useful to provide logging, transform names or substitute values.
 * <p>
 *
 * Implementations of {@link ConfigSourceInterceptor} are loaded via the {@link java.util.ServiceLoader} mechanism and
 * can be registered by providing a resource named {@code META-INF/services/io.smallrye.config.ConfigSourceInterceptor},
 * which contains the fully qualified {@code ConfigSourceInterceptor} implementation class name as its content.
 * <p>
 *
 * Alternatively, a {@link ConfigSourceInterceptor} can also be loaded with a {@link ConfigSourceInterceptorFactory}.
 * <p>
 *
 * A {@link ConfigSourceInterceptor} implementation class can specify a priority by way of the standard
 * {@code jakarta.annotation.Priority} annotation. If no priority is explicitly assigned, the default priority value
 * of {@code io.smallrye.config.Priorities#APPLICATION} is assumed. If multiple interceptors are registered with the
 * same priority, then their execution order may be non deterministic.
 */
@Experimental("Interceptor API to intercept resolution of a configuration name")
public interface ConfigSourceInterceptor extends Serializable {
    /**
     * Intercept the resolution of a configuration name and either return the corresponding {@link ConfigValue} or a
     * custom {@link ConfigValue} built by the interceptor. Calling
     * {@link ConfigSourceInterceptorContext#proceed(String)} will continue to execute the interceptor chain. The chain
     * can be short-circuited by returning another instance of {@link ConfigValue}.
     *
     * @param context the interceptor context. See {@link ConfigSourceInterceptorContext}
     * @param name the configuration name being intercepted.
     *
     * @return a {@link ConfigValue} with information about the name, value, config source and ordinal, or {@code null}
     *         if the value isn't present.
     */
    ConfigValue getValue(ConfigSourceInterceptorContext context, String name);

    /**
     * Intercept the resolution of the configuration names. The Iterator names may be a subset of the
     * total names retrieved from all the registered ConfigSources. Calling
     * {@link ConfigSourceInterceptorContext#iterateNames()} will continue to execute the interceptor chain. The chain
     * can be short-circuited by returning another instance of the Iterator.
     *
     * @param context the interceptor context. See {@link ConfigSourceInterceptorContext}
     *
     * @return an Iterator of Strings with configuration names.
     */
    default Iterator<String> iterateNames(ConfigSourceInterceptorContext context) {
        return context.iterateNames();
    }

    /**
     * Intercept the resolution of the configuration {@link ConfigValue}. Calling
     * {@link ConfigSourceInterceptorContext#iterateNames()} will continue to execute the interceptor chain. The chain
     * can be short-circuited by returning another instance of the Iterator.
     *
     * @param context the interceptor context. See {@link ConfigSourceInterceptorContext}
     *
     * @return an Iterator of {@link ConfigValue} with information about the name, value, config source and ordinal.
     */
    default Iterator<ConfigValue> iterateValues(ConfigSourceInterceptorContext context) {
        return context.iterateValues();
    }

    ConfigSourceInterceptor EMPTY = new ConfigSourceInterceptor() {
        private static final long serialVersionUID = 5749001327530543433L;

        @Override
        public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
            return null;
        }

        @Override
        public Iterator<String> iterateNames(final ConfigSourceInterceptorContext context) {
            return Collections.emptyIterator();
        }

        @Override
        public Iterator<ConfigValue> iterateValues(final ConfigSourceInterceptorContext context) {
            return Collections.emptyIterator();
        }
    };
}
