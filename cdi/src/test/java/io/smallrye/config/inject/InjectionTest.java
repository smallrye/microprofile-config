package io.smallrye.config.inject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public abstract class InjectionTest {
    @BeforeAll
    protected static void beforeClass() throws Exception {
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        final URLClassLoader urlClassLoader = new URLClassLoader(new URL[] {
                new URL("memory", null, 0, "/", new InMemoryStreamHandler(InjectionTestConfigFactory.class.getName()))
        }, contextClassLoader);
        Thread.currentThread().setContextClassLoader(urlClassLoader);
    }

    @AfterAll
    protected static void afterClass() {
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(contextClassLoader.getParent());
    }

    public static class InMemoryStreamHandler extends URLStreamHandler {
        final byte[] contents;

        public InMemoryStreamHandler(final String contents) {
            this.contents = contents.getBytes();
        }

        @Override
        protected URLConnection openConnection(final URL u) {
            if (!u.getFile().endsWith("SmallRyeConfigFactory")) {
                return null;
            }

            return new URLConnection(u) {
                @Override
                public void connect() {
                }

                @Override
                public InputStream getInputStream() {
                    return new ByteArrayInputStream(contents);
                }
            };
        }
    }
}
