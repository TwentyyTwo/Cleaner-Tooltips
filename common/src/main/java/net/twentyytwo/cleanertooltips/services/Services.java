package net.twentyytwo.cleanertooltips.services;

import java.util.ServiceLoader;

public class Services {

    public static final PlatformService PLATFORM = load(PlatformService.class);

    public static <T> T load(Class<T> clazz) {
        return ServiceLoader.load(clazz)
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
    }
}
