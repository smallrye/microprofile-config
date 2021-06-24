package io.smallrye.config.events;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

/**
 * Filter by a config source
 * 
 * @author <a href="mailto:phillip.kruger@redhat.com">Phillip Kruger</a>
 */
@Qualifier
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SourceFilter {
    String value();

    class SourceFilterLiteral extends AnnotationLiteral<SourceFilter> implements SourceFilter {
        private final String name;

        SourceFilterLiteral(String name) {
            this.name = name;
        }

        @Override
        public String value() {
            return this.name;
        }
    }
}
