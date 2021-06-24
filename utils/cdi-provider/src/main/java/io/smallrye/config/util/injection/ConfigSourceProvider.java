package io.smallrye.config.util.injection;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

/**
 * Making the Config sources available via CDI
 * 
 * @author <a href="mailto:phillip.kruger@redhat.com">Phillip Kruger</a>
 */
@Dependent
public class ConfigSourceProvider {
    @Inject
    private Provider<Config> configProvider;

    private final Map<String, ConfigSource> configSourceMap = new HashMap<String, ConfigSource>() {
        @Override
        public Collection<ConfigSource> values() {
            return StreamSupport.stream(configProvider.get().getConfigSources().spliterator(), false)
                    .collect(Collectors.toList());
        }
    };

    @PostConstruct
    public void init() {
        if (this.configSourceMap.isEmpty()) {
            for (ConfigSource configSource : configProvider.get().getConfigSources()) {
                this.configSourceMap.put(configSource.getName(), configSource);
            }
        }
    }

    @Produces
    @ConfigSourceMap
    public Map<String, ConfigSource> produceConfigSourceMap() {
        return this.configSourceMap;
    }

    @Produces
    @Name("")
    public ConfigSource produceConfigSource(final InjectionPoint injectionPoint) {
        Set<Annotation> qualifiers = injectionPoint.getQualifiers();
        String name = getName(qualifiers);
        return configSourceMap.get(name);
    }

    private String getName(Set<Annotation> qualifiers) {
        for (Annotation qualifier : qualifiers) {
            if (qualifier.annotationType().equals(Name.class)) {
                Name name = (Name) qualifier;
                return name.value();
            }
        }
        return "";
    }
}
