/*
 * Copyright 2017 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.smallrye.config;

import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.eclipse.microprofile.config.spi.Converter;

/**
 * Based on GERONIMO-6595 support implicit converters.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
class ImplicitConverters {

    static <T> Converter<T> getConverter(Class<T> clazz) {
        // implicit converters required by the specification
        Converter<T> converter = getConverterFromStaticMethod(clazz, "of", String.class);
        if (converter == null) {
            converter = getConverterFromStaticMethod(clazz, "valueOf", String.class);
            if (converter == null) {
                converter = getConverterFromConstructor(clazz, String.class);
                if (converter == null) {
                    converter = getConverterFromStaticMethod(clazz, "parse", CharSequence.class);
                    if (converter == null) {
                        // additional implicit converters
                        converter = getConverterFromConstructor(clazz, CharSequence.class);
                        if (converter == null) {
                            converter = getConverterFromStaticMethod(clazz, "valueOf", CharSequence.class);
                            if (converter == null) {
                                converter = getConverterFromStaticMethod(clazz, "parse", String.class);
                            }
                        }
                    }
                }
            }
        }
        return converter;
    }

    private static <T> Converter<T> getConverterFromConstructor(Class<T> clazz, Class<? super String> paramType) {
        try {
            final Constructor<T> declaredConstructor = clazz.getDeclaredConstructor(paramType);
            if (!declaredConstructor.isAccessible()) {
                declaredConstructor.setAccessible(true);
            }
            return new ConstructorConverter<>(declaredConstructor);
        } catch (NoSuchMethodException e) {
        }
        return null;
    }

    private static <T> Converter<T> getConverterFromStaticMethod(Class<T> clazz, String methodName,
            Class<? super String> paramType) {
        try {
            final Method method = clazz.getMethod(methodName, paramType);
            if (clazz != method.getReturnType()) {
                // doesn't meet requirements of the spec
                return null;
            }
            if (!method.isAccessible()) {
                method.setAccessible(true);
            }
            if (Modifier.isStatic(method.getModifiers())) {
                return new StaticMethodConverter<T>(clazz, method);
            }
        } catch (NoSuchMethodException e) {
        }
        return null;
    }

    static class StaticMethodConverter<T> implements Converter<T>, Serializable {

        private static final long serialVersionUID = 3350265927359848883L;

        private final Class<T> clazz;
        private final Method method;

        StaticMethodConverter(Class<T> clazz, Method method) {
            assert clazz == method.getReturnType();
            this.clazz = clazz;
            this.method = method;
        }

        @Override
        public T convert(String value) {
            try {
                return clazz.cast(method.invoke(null, value));
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new IllegalArgumentException(e);
            }
        }

        Object writeReplace() {
            return new Serialized(method.getDeclaringClass(), method.getName(), method.getParameterTypes()[0]);
        }

        static final class Serialized implements Serializable {
            private static final long serialVersionUID = -6334004040897615452L;

            private final Class<?> c;
            private final String m;
            private final Class<?> p;

            Serialized(final Class<?> c, final String m, final Class<?> p) {
                this.c = c;
                this.m = m;
                this.p = p;
            }

            Object readResolve() throws ObjectStreamException {
                if (!p.isAssignableFrom(String.class)) {
                    throw new InvalidObjectException("Invalid parameter type");
                }
                final Method method;
                try {
                    method = c.getMethod(m, p);
                } catch (NoSuchMethodException e) {
                    throw new InvalidObjectException("No matching method found");
                }
                if (c != method.getReturnType()) {
                    // doesn't meet requirements of the spec
                    throw new InvalidObjectException("Deserialized method has invalid return type");
                }
                if (!method.isAccessible()) {
                    throw new InvalidObjectException("Deserialized method is not accessible");
                }
                if (!Modifier.isStatic(method.getModifiers())) {
                    throw new InvalidObjectException("Non static method " + method);
                }
                return new StaticMethodConverter<>(method.getReturnType(), method);
            }
        }
    }

    static class ConstructorConverter<T> implements Converter<T>, Serializable {

        private static final long serialVersionUID = 3350265927359848883L;

        private final Constructor<T> ctor;

        public ConstructorConverter(final Constructor<T> ctor) {
            this.ctor = ctor;
        }

        @Override
        public T convert(String value) {
            try {
                return ctor.newInstance(value);
            } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
                throw new IllegalArgumentException(e);
            }
        }

        Object writeReplace() {
            return new Serialized(ctor.getDeclaringClass(), ctor.getParameterTypes()[0]);
        }

        static final class Serialized implements Serializable {
            private static final long serialVersionUID = -2903564775826815453L;

            private final Class<?> c;
            private final Class<?> p;

            Serialized(final Class<?> c, final Class<?> p) {
                this.c = c;
                this.p = p;
            }

            Object readResolve() throws ObjectStreamException {
                if (!p.isAssignableFrom(String.class)) {
                    throw new InvalidObjectException("Invalid parameter type");
                }
                final Constructor<?> ctor;
                try {
                    ctor = c.getConstructor(p);
                } catch (NoSuchMethodException e) {
                    throw new InvalidObjectException("No matching constructor found");
                }
                if (!ctor.isAccessible()) {
                    throw new InvalidObjectException("Deserialized constructor is not accessible");
                }
                return new ConstructorConverter<>(ctor);
            }
        }
    }
}
