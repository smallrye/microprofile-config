package io.smallrye.config;

import static io.smallrye.common.expression.Expression.Flag.LENIENT_SYNTAX;
import static io.smallrye.common.expression.Expression.Flag.NO_SMART_BRACES;
import static io.smallrye.common.expression.Expression.Flag.NO_TRIM;

import java.util.Optional;

import org.eclipse.microprofile.config.Config;

import io.smallrye.common.expression.Expression;
import jakarta.annotation.Priority;

@Priority(Priorities.LIBRARY + 800)
public class ExpressionConfigSourceInterceptor implements ConfigSourceInterceptor {
    private static final long serialVersionUID = -539336551011916218L;

    private static final int MAX_DEPTH = 32;

    private final boolean enabled;

    public ExpressionConfigSourceInterceptor() {
        this.enabled = true;
    }

    public ExpressionConfigSourceInterceptor(final ConfigSourceInterceptorContext context) {
        this.enabled = Optional.ofNullable(context.proceed(Config.PROPERTY_EXPRESSIONS_ENABLED))
                .map(ConfigValue::getValue)
                .map(Boolean::valueOf)
                .orElse(Boolean.TRUE);
    }

    @Override
    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
        return getValue(context, name, 1);
    }

    private ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name, final int depth) {
        if (depth == MAX_DEPTH) {
            throw ConfigMessages.msg.expressionExpansionTooDepth(name);
        }

        final ConfigValue configValue = context.proceed(name);

        if (!Expressions.isEnabled() || !enabled) {
            return configValue;
        }

        if (configValue == null) {
            return null;
        }

        final Expression expression = Expression.compile(escapeDollarIfExists(configValue.getValue()), LENIENT_SYNTAX, NO_TRIM,
                NO_SMART_BRACES);
        final String expanded = expression.evaluate((resolveContext, stringBuilder) -> {
            final ConfigValue resolve = getValue(context, resolveContext.getKey(), depth + 1);
            if (resolve != null) {
                stringBuilder.append(resolve.getValue());
            } else if (resolveContext.hasDefault()) {
                resolveContext.expandDefault();
            } else {
                throw ConfigMessages.msg.expandingElementNotFound(resolveContext.getKey(), configValue.getName());
            }
        });

        return configValue.withValue(expanded);
    }

    /**
     * MicroProfile Config defines the backslash escape for dollar to retrieve the raw expression. We don't want to
     * turn {@link Expression.Flag#ESCAPES} on because it may break working configurations.
     *
     * This will replace the expected escape in MicroProfile Config by the escape used in {@link Expression}, a double
     * dollar.
     */
    private String escapeDollarIfExists(final String value) {
        int index = value.indexOf("\\$");
        if (index != -1) {
            int start = 0;
            StringBuilder builder = new StringBuilder();
            while (index != -1) {
                builder.append(value, start, index).append("$$");
                start = index + 2;
                index = value.indexOf("\\$", start);
            }
            builder.append(value.substring(start));
            return builder.toString();
        }
        return value;
    }
}
