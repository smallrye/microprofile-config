/*
 * Copyright 2018 Red Hat, Inc.
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

package io.smallrye.config.test.collections;

import java.util.List;
import java.util.Set;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
public class CollectionBean {

    @Inject
    @ConfigProperty(name = "myPets", defaultValue = "horse,monkey")
    private String[] arrayPets;

    @Inject
    @ConfigProperty(name = "myPets", defaultValue = "cat,lama")
    private List<String> listPets;

    @Inject
    @ConfigProperty(name = "myPets", defaultValue = "dog,mouse")
    private Set<String> setPets;

    @Inject
    @ConfigProperty(name = "test.converter.stringList", defaultValue = "")
    private Provider<List<String>> listWithEmptyDefault;

    @Inject
    @ConfigProperty(name = "test.converter.stringList2")
    private Provider<List<String>> listWithNoDefault;

    public String[] getArrayPets() {
        return arrayPets;
    }

    public List<String> getListPets() {
        return listPets;
    }

    public Set<String> getSetPets() {
        return setPets;
    }

    public Provider<List<String>> getListWithEmptyDefault() {
        return listWithEmptyDefault;
    }

    public Provider<List<String>> getListWithNoDefault() {
        return listWithNoDefault;
    }
}
