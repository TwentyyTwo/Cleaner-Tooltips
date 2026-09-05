package net.twentyytwo.cleanertooltips.services;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.twentyytwo.cleanertooltips.CleanerTooltips;

public class FabricPlatformService implements PlatformService {

    @Override
    public boolean isModLoaded(String modid) {
        return FabricLoader.getInstance().isModLoaded(modid);
    }

    @Override
    public boolean isKeyDown() {
        int key = KeyBindingHelper.getBoundKeyOf(CleanerTooltips.HIDE_TOOLTIP).getValue();
        return key != -1 && InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), key);
    }
}
