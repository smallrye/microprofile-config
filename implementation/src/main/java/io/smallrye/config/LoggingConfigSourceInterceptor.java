package io.smallrye.config;

import static io.smallrye.config.SecretKeys.doLocked;

import jakarta.annotation.Priority;

@Priority(Priorities.LIBRARY + 200)
public class LoggingConfigSourceInterceptor implements ConfigSourceInterceptor {
    private static final long serialVersionUID = 367246512037404779L;

    @Override
    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
        try {
            // Unlocked keys will run here.
            ConfigValue configValue = doLocked(() -> context.proceed(name));
            if (configValue != null)
                ConfigLogging.log.lookup(configValue.getName(), configValue.getLocation(), configValue.getValue());
            else
                ConfigLogging.log.notFound(name);
            return configValue;
        } catch (SecurityException e) {
            // Handled next, to omit the values to log from the secret.
        }

        // Locked keys here.
        final ConfigValue secret = context.proceed(name);
        if (secret != null)
            ConfigLogging.log.lookup(secret.getName(), "secret", "secret");
        else
            ConfigLogging.log.notFound(name);
        return secret;
    }
}
