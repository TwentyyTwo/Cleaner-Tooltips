package net.twentyytwo.cleanertooltips.services;

import com.mojang.blaze3d.platform.InputConstants;
import net.neoforged.fml.ModList;
import net.twentyytwo.cleanertooltips.CleanerTooltips;

public class NeoforgeModLoadingHelper implements ModLoadingHelper {
    @Override
    public boolean isModLoaded(String modid) {
        return ModList.get().isLoaded(modid);
    }

    @Override
    public boolean isKeyDown() {
        return InputConstants.isKeyDown(CleanerTooltips.MC.getWindow().getWindow(), CleanerTooltips.hideTooltip.getKey().getValue());
    }
}
