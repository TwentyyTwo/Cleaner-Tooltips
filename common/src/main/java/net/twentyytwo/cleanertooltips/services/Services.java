package net.twentyytwo.cleanertooltips.services;

import java.util.ServiceLoader;

public class Services {
    private static ModLoadingHelper instance;

    public static ModLoadingHelper getInstance() {
        if (instance == null) {
            ServiceLoader<ModLoadingHelper> loader = ServiceLoader.load(ModLoadingHelper.class);
            instance = loader.findFirst().orElseThrow(() -> new IllegalStateException("No implementation found!"));
        }
        return instance;
    }
}
