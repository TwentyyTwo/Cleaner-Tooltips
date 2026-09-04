package net.twentyytwo.cleanertooltips.services;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.ModList;
import net.twentyytwo.cleanertooltips.CleanerTooltips;

public class NeoforgePlatformService implements PlatformService {

    @Override
    public boolean isModLoaded(String modid) {
        return ModList.get().isLoaded(modid);
    }

    @Override
    public boolean isKeyDown() {
        int key = CleanerTooltips.HIDE_TOOLTIP.getKey().getValue();
        return key != -1 && InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), key);
    }
}
