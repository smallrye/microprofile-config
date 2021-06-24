package io.smallrye.config.validator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.smallrye.config.ConfigMappingInterface;
import io.smallrye.config.ConfigMappingInterface.CollectionProperty;
import io.smallrye.config.ConfigMappingInterface.MapProperty;
import io.smallrye.config.ConfigMappingInterface.NamingStrategy;
import io.smallrye.config.ConfigMappingInterface.Property;
import io.smallrye.config.ConfigValidationException;
import io.smallrye.config.ConfigValidationException.Problem;
import io.smallrye.config.ConfigValidator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import jakarta.validation.Validator;

public interface BeanValidationConfigValidator extends ConfigValidator {

    Validator getValidator();

    @Override
    default void validateMapping(
            final Class<?> mappingClass,
            final String prefix,
            final Object mappingObject)
            throws ConfigValidationException {

        final List<Problem> problems = new ArrayList<>();
        final ConfigMappingInterface mappingInterface = ConfigMappingInterface.getConfigurationInterface(mappingClass);
        if (mappingInterface != null) {
            validateMappingInterface(mappingInterface, prefix, mappingInterface.getNamingStrategy(), mappingObject, problems);
        } else {
            validateMappingClass(mappingObject, problems);
        }

        if (!problems.isEmpty()) {
            throw new ConfigValidationException(problems.toArray(ConfigValidationException.Problem.NO_PROBLEMS));
        }
    }

    default void validateMappingInterface(
            final ConfigMappingInterface mappingInterface,
            final String currentPath,
            final NamingStrategy namingStrategy,
            final Object mappingObject,
            final List<Problem> problems) {

        for (Property property : mappingInterface.getProperties()) {
            validateProperty(property, currentPath, namingStrategy, mappingObject, false, problems);
        }
    }

    default void validateProperty(
            final Property property,
            final String currentPath,
            final NamingStrategy namingStrategy,
            final Object mappingObject,
            final boolean optional,
            final List<Problem> problems) {

        if (property.isOptional()) {
            validateProperty(property.asOptional().getNestedProperty(), currentPath, namingStrategy, mappingObject, true,
                    problems);
        }

        if ((property.isLeaf() || property.isPrimitive()) && !property.isOptional()) {
            validatePropertyValue(property, currentPath, namingStrategy, mappingObject, problems);
        }

        if (property.isGroup()) {
            try {
                Object group = property.getMethod().invoke(mappingObject);
                // unwrap
                if (optional) {
                    Optional<?> optionalGroup = (Optional<?>) group;
                    if (!optionalGroup.isPresent()) {
                        return;
                    }
                    group = optionalGroup.get();
                }

                validateMappingInterface(property.asGroup().getGroupType(), appendPropertyName(currentPath, property),
                        namingStrategy, group, problems);
            } catch (IllegalAccessException e) {
                throw new IllegalAccessError(e.getMessage());
            } catch (InvocationTargetException e) {
                try {
                    throw e.getCause();
                } catch (RuntimeException | Error e2) {
                    throw e2;
                } catch (Throwable t2) {
                    throw new UndeclaredThrowableException(t2);
                }
            }
        }

        if (property.isCollection()) {
            CollectionProperty collectionProperty = property.asCollection();
            if (collectionProperty.getElement().isGroup()) {
                try {
                    Collection<?> collection = (Collection<?>) property.getMethod().invoke(mappingObject);
                    int i = 0;
                    for (Object element : collection) {
                        validateMappingInterface(collectionProperty.getElement().asGroup().getGroupType(),
                                appendPropertyName(currentPath, property) + "[" + i + "]",
                                namingStrategy, element, problems);
                        i++;
                    }
                } catch (IllegalAccessException e) {
                    throw new IllegalAccessError(e.getMessage());
                } catch (InvocationTargetException e) {
                    try {
                        throw e.getCause();
                    } catch (RuntimeException | Error e2) {
                        throw e2;
                    } catch (Throwable t2) {
                        throw new UndeclaredThrowableException(t2);
                    }
                }
            } else if (collectionProperty.getElement().isLeaf()) {
                validateProperty(collectionProperty.getElement(), currentPath, namingStrategy, mappingObject, optional,
                        problems);
            }
        }

        if (property.isMap()) {
            MapProperty mapProperty = property.asMap();
            if (mapProperty.getValueProperty().isGroup()) {
                try {
                    Map<?, ?> map = (Map<?, ?>) property.getMethod().invoke(mappingObject);
                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        validateMappingInterface(mapProperty.getValueProperty().asGroup().getGroupType(),
                                appendPropertyName(currentPath, property) + "." + entry.getKey(),
                                namingStrategy, entry.getValue(), problems);
                    }
                } catch (IllegalAccessException e) {
                    throw new IllegalAccessError(e.getMessage());
                } catch (InvocationTargetException e) {
                    try {
                        throw e.getCause();
                    } catch (RuntimeException | Error e2) {
                        throw e2;
                    } catch (Throwable t2) {
                        throw new UndeclaredThrowableException(t2);
                    }
                }
            } else if (mapProperty.getValueProperty().isLeaf()) {
                validatePropertyValue(property, currentPath, namingStrategy, mappingObject, problems);
            }
        }
    }

    default void validatePropertyValue(
            final Property property,
            final String currentPath,
            final NamingStrategy namingStrategy,
            final Object mappingObject,
            final List<Problem> problems) {

        try {
            Set<ConstraintViolation<Object>> violations = getValidator().forExecutables().validateReturnValue(mappingObject,
                    property.getMethod(),
                    property.getMethod().invoke(mappingObject));
            for (ConstraintViolation<Object> violation : violations) {
                problems.add(new Problem(interpolateMessage(currentPath, namingStrategy, property, violation)));
            }
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        } catch (InvocationTargetException e) {
            try {
                throw e.getCause();
            } catch (RuntimeException | Error e2) {
                throw e2;
            } catch (Throwable t2) {
                throw new UndeclaredThrowableException(t2);
            }
        }
    }

    default void validateMappingClass(final Object mappingObject, final List<Problem> problems) {
        final Set<ConstraintViolation<Object>> violations = getValidator().validate(mappingObject);
        for (ConstraintViolation<Object> violation : violations) {
            problems.add(new Problem(violation.getPropertyPath() + " " + violation.getMessage()));
        }
    }

    default String appendPropertyName(final String currentPath, final Property property) {
        if (currentPath.isEmpty()) {
            return property.getPropertyName();
        }

        if (property.getPropertyName().isEmpty()) {
            return currentPath;
        }

        return currentPath + "." + property.getPropertyName();
    }

    default String interpolateMessage(
            final String currentPath,
            final NamingStrategy namingStrategy,
            final Property property,
            final ConstraintViolation<?> violation) {
        StringBuilder propertyName = new StringBuilder(currentPath);
        String name = namingStrategy.apply(property.getPropertyName());
        if (!name.isEmpty()) {
            propertyName.append(".").append(name);
        }
        Path propertyPath = violation.getPropertyPath();
        for (Path.Node node : propertyPath) {
            if (node.isInIterable()) {
                if (node.getIndex() != null) {
                    propertyName.append("[").append(node.getIndex()).append("]");
                } else if (node.getKey() != null) {
                    propertyName.append(".").append(node.getKey());
                }
            }
        }
        return propertyName.toString() + " " + violation.getMessage();
    }
}
