package io.smallrye.config;

import static io.smallrye.config.Converters.STRING_CONVERTER;
import static io.smallrye.config.Converters.newCollectionConverter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import jakarta.annotation.Priority;

@Priority(Priorities.LIBRARY + 600)
public class ProfileConfigSourceInterceptor implements ConfigSourceInterceptor {

    private static final long serialVersionUID = -6305289277993917313L;
    private static final Comparator<ConfigValue> CONFIG_SOURCE_COMPARATOR = (o1, o2) -> {
        int res = Integer.compare(o2.getConfigSourceOrdinal(), o1.getConfigSourceOrdinal());
        if (res != 0) {
            return res;
        }

        if (o1.getConfigSourceName() != null && o2.getConfigSourceName() != null) {
            return o2.getConfigSourceName().compareTo(o1.getConfigSourceName());
        } else {
            return res;
        }
    };

    private final String[] profiles;

    public ProfileConfigSourceInterceptor(final String profile) {
        this(profile != null ? convertProfile(profile) : new ArrayList<>());
    }

    public ProfileConfigSourceInterceptor(final List<String> profiles) {
        List<String> reverseProfiles = new ArrayList<>(profiles);
        Collections.reverse(reverseProfiles);
        this.profiles = reverseProfiles.toArray(new String[0]);
    }

    public ProfileConfigSourceInterceptor(final ConfigSourceInterceptorContext context) {
        this(convertProfile(context));
    }

    @Override
    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
        if (profiles.length > 0) {
            final String normalizeName = normalizeName(name);
            final ConfigValue profileValue = getProfileValue(context, normalizeName);
            if (profileValue != null) {
                try {
                    final ConfigValue originalValue = context.proceed(normalizeName);
                    if (originalValue != null && CONFIG_SOURCE_COMPARATOR.compare(profileValue, originalValue) > 0) {
                        return originalValue;
                    }
                } catch (final NoSuchElementException e) {
                    // We couldn't find the main property so we fallback to the profile property because it exists.
                }
                return profileValue.withName(normalizeName);
            }
        }

        return context.proceed(name);
    }

    public ConfigValue getProfileValue(final ConfigSourceInterceptorContext context, final String normalizeName) {
        for (String profile : profiles) {
            final ConfigValue profileValue = context.proceed("%" + profile + "." + normalizeName);
            if (profileValue != null) {
                return profileValue;
            }
        }

        return null;
    }

    @Override
    public Iterator<String> iterateNames(final ConfigSourceInterceptorContext context) {
        final Set<String> names = new HashSet<>();
        final Iterator<String> namesIterator = context.iterateNames();
        while (namesIterator.hasNext()) {
            names.add(normalizeName(namesIterator.next()));
        }
        return names.iterator();
    }

    @Override
    public Iterator<ConfigValue> iterateValues(final ConfigSourceInterceptorContext context) {
        final Set<ConfigValue> values = new HashSet<>();
        final Iterator<ConfigValue> valuesIterator = context.iterateValues();
        while (valuesIterator.hasNext()) {
            final ConfigValue value = valuesIterator.next();
            values.add(value.withName(normalizeName(value.getName())));
        }
        return values.iterator();
    }

    public String[] getProfiles() {
        return profiles;
    }

    private String normalizeName(final String name) {
        for (String profile : profiles) {
            if (name.startsWith("%" + profile + ".")) {
                return name.substring(profile.length() + 2);
            }
        }

        return name;
    }

    public static List<String> convertProfile(final String profile) {
        return newCollectionConverter(STRING_CONVERTER, ArrayList::new).convert(profile);
    }

    private static List<String> convertProfile(final ConfigSourceInterceptorContext context) {
        final List<String> profiles = new ArrayList<>();
        final ConfigValue profile = context.proceed(SmallRyeConfig.SMALLRYE_CONFIG_PROFILE);
        if (profile != null) {
            final ConfigValue parentProfile = context.proceed(SmallRyeConfig.SMALLRYE_CONFIG_PROFILE_PARENT);
            if (parentProfile != null) {
                profiles.add(parentProfile.getValue());
            }
            profiles.addAll(convertProfile(profile.getValue()));
        }
        return profiles;
    }
}
