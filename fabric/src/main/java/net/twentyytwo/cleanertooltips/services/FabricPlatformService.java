package net.twentyytwo.cleanertooltips.services;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.twentyytwo.cleanertooltips.CleanerTooltips;

import static net.twentyytwo.cleanertooltips.CleanerTooltips.MC;

public class FabricPlatformService implements PlatformService {

    @Override
    public boolean isModLoaded(String modid) {
        return FabricLoader.getInstance().isModLoaded(modid);
    }

    @Override
    public boolean isKeyDown() {
        return InputConstants.isKeyDown(MC.getWindow(),
                KeyBindingHelper.getBoundKeyOf(CleanerTooltips.hideTooltip).getValue());
    }
}
