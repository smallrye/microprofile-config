package io.smallrye.config.events.regex;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.smallrye.config.events.ChangeEvent;
import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@RegexFilter(value = "")
@Interceptor
@Priority(100)
public class RegexFilterInterceptor {
    @AroundInvoke
    public Object observer(InvocationContext ctx) throws Exception {

        RegexFilter regexFilterAnnotation = ctx.getMethod().getAnnotation(RegexFilter.class);
        Field onField = regexFilterAnnotation.onField();
        String regex = regexFilterAnnotation.value();

        Optional<ChangeEvent> posibleChangeEvent = getChangeEvent(ctx);

        if (posibleChangeEvent.isPresent()) {
            ChangeEvent changeEvent = posibleChangeEvent.get();
            String value = getValueToApplyRegexOn(changeEvent, onField);
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(value);
            boolean b = matcher.matches();
            if (!b)
                return null;
        } else {
            RegexLogging.log.changeEventParameterMissing(ctx.getMethod().getName());
        }
        return ctx.proceed();
    }

    private String getValueToApplyRegexOn(ChangeEvent changeEvent, Field onField) {
        String value = null;
        switch (onField) {
            case key:
                value = changeEvent.getKey();
                break;
            case fromSource:
                value = changeEvent.getFromSource();
                break;
            case newValue:
                value = changeEvent.getNewValue();
                break;
            case oldValue:
                value = changeEvent.getOldValue().orElse("");
        }

        return value;
    }

    private Optional<ChangeEvent> getChangeEvent(InvocationContext ctx) {
        Object[] parameters = ctx.getParameters();

        for (Object parameter : parameters) {
            if (parameter.getClass().equals(ChangeEvent.class)) {
                ChangeEvent changeEvent = (ChangeEvent) parameter;
                return Optional.of(changeEvent);
            }
        }
        return Optional.empty();
    }
}
