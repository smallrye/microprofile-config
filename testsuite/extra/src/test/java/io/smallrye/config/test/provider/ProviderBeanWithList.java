package io.smallrye.config.test.provider;

import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

public class ProviderBeanWithList {

    @Inject
    @ConfigProperty(name = "objectIds", defaultValue = "")
    Provider<List<String>> objectdIds;

    @Inject
    @ConfigProperty(name = "numbers", defaultValue = "")
    Provider<List<Integer>> numbers;
}
