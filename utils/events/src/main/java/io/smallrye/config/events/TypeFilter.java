package io.smallrye.config.events;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

/**
 * filter by change type
 * 
 * @author <a href="mailto:phillip.kruger@redhat.com">Phillip Kruger</a>
 */
@Qualifier
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TypeFilter {
    Type value();

    class TypeFilterLiteral extends AnnotationLiteral<TypeFilter> implements TypeFilter {
        private final Type type;

        TypeFilterLiteral(Type type) {
            this.type = type;
        }

        @Override
        public Type value() {
            return this.type;
        }
    }
}
