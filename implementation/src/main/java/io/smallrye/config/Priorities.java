package io.smallrye.config;

import io.smallrye.common.annotation.Experimental;

/**
 * A collection of built-in priority constants for {@link ConfigSourceInterceptor} that are supposed to be
 * ordered based on their {@code jakarta.annotation.Priority} class-level annotation.
 */
@Experimental("Interceptor API to intercept resolution of a configuration name")
public final class Priorities {
    /**
     * Range for early interceptors defined by Platform specifications.
     */
    public static final int PLATFORM = 1000;

    /**
     * Range for interceptors defined by SmallRye Config or Extension Libraries.
     */
    public static final int LIBRARY = 3000;

    /**
     * Range for interceptors defined by User Applications.
     */
    public static final int APPLICATION = 5000;

    private Priorities() {
        throw new UnsupportedOperationException();
    }
}
