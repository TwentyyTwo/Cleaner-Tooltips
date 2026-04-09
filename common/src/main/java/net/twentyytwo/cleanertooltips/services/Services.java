package net.twentyytwo.cleanertooltips.services;

import java.util.ServiceLoader;

public class Services {
    private static PlatformService instance;

    public static PlatformService getInstance() {
        if (instance == null) {
            ServiceLoader<PlatformService> loader = ServiceLoader.load(PlatformService.class);
            instance = loader.findFirst().orElseThrow(() -> new IllegalStateException("No implementation found!"));
        }
        return instance;
    }
}
