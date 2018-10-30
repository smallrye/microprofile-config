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

package io.smallrye.config.inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import java.io.Serializable;
import java.util.*;

import static io.smallrye.config.SecuritySupport.getContextClassLoader;

/**
 * CDI producer for {@link Config} bean.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
@ApplicationScoped
public class ConfigProducer implements Serializable{

    @Produces
    Config getConfig(InjectionPoint injectionPoint) {
        // return the Config for the TCCL
        return ConfigProvider.getConfig(getContextClassLoader());
    }

    @Dependent
    @Produces @ConfigProperty
    String produceStringConfigProperty(InjectionPoint ip) {
        return ConfigProducerUtil.getValue(ip, String.class, getConfig(ip));
    }

    @Dependent
    @Produces @ConfigProperty
    Long getLongValue(InjectionPoint ip) {
        return ConfigProducerUtil.getValue(ip, Long.class, getConfig(ip));
    }

    @Dependent
    @Produces @ConfigProperty
    Integer getIntegerValue(InjectionPoint ip) {
        return ConfigProducerUtil.getValue(ip, Integer.class, getConfig(ip));
    }

    @Dependent
    @Produces @ConfigProperty
    Float produceFloatConfigProperty(InjectionPoint ip) {
        return ConfigProducerUtil.getValue(ip, Float.class, getConfig(ip));
    }

    @Dependent
    @Produces @ConfigProperty
    Double produceDoubleConfigProperty(InjectionPoint ip) {
        return ConfigProducerUtil.getValue(ip, Double.class, getConfig(ip));
    }

    @Dependent
    @Produces @ConfigProperty
    Boolean produceBooleanConfigProperty(InjectionPoint ip) {
        return ConfigProducerUtil.getValue(ip, Boolean.class, getConfig(ip));
    }

    @Dependent
    @Produces @ConfigProperty
    <T> Optional<T> produceOptionalConfigValue(InjectionPoint injectionPoint) {
        return ConfigProducerUtil.optionalConfigValue(injectionPoint, getConfig(injectionPoint));
    }

    @Dependent
    @Produces @ConfigProperty
    <T> Set<T> producesSetConfigPropery(InjectionPoint ip) {
        return ConfigProducerUtil.collectionConfigProperty(ip, getConfig(ip), new HashSet<>());
    }

    @Dependent
    @Produces @ConfigProperty
    <T> List<T> producesListConfigPropery(InjectionPoint ip) {
        return ConfigProducerUtil.collectionConfigProperty(ip, getConfig(ip), new ArrayList<T>());
    }

}
