package net.twentyytwo.cleanertooltips.services;

public interface PlatformService {

    /**
     * Check if a mod is loaded.
     * @param modid the id of the mod to check
     * @return      true if the given mod is loaded
     */
    boolean isModLoaded(String modid);

    /**
     * Check if the {@code hideTooltip} KeyMapping is held down.
     * @return  true if the key is held down
     */
    boolean isKeyDown();
}
