package io.smallrye.config.events;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

/**
 * Filter the event on the key
 * 
 * @author <a href="mailto:phillip.kruger@redhat.com">Phillip Kruger</a>
 */
@Qualifier
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface KeyFilter {
    String value();

    class KeyFilterLiteral extends AnnotationLiteral<KeyFilter> implements KeyFilter {
        private final String key;

        KeyFilterLiteral(String key) {
            this.key = key;
        }

        @Override
        public String value() {
            return this.key;
        }
    }
}
