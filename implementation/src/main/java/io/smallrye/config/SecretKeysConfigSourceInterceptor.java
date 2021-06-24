package io.smallrye.config;

import java.util.Set;

import jakarta.annotation.Priority;

@Priority(Priorities.LIBRARY + 100)
public class SecretKeysConfigSourceInterceptor implements ConfigSourceInterceptor {
    private static final long serialVersionUID = 7291982039729980590L;

    private final Set<String> secrets;

    public SecretKeysConfigSourceInterceptor(final Set<String> secrets) {
        this.secrets = secrets;
    }

    @Override
    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
        if (SecretKeys.isLocked() && isSecret(name)) {
            throw ConfigMessages.msg.notAllowed(name);
        }
        return context.proceed(name);
    }

    private boolean isSecret(final String name) {
        return secrets.contains(name);
    }
}
