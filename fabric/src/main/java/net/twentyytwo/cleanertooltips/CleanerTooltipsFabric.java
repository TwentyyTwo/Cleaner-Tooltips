package net.twentyytwo.cleanertooltips;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.twentyytwo.cleanertooltips.services.ModLoadingHelper;

import static net.twentyytwo.cleanertooltips.CleanerTooltips.MC;

public class CleanerTooltipsFabric implements ClientModInitializer, ModLoadingHelper {

    @Override
    public void onInitializeClient() {
        CleanerTooltips.init();
        KeyBindingHelper.registerKeyBinding(CleanerTooltips.hideTooltip);
    }

    @Override
    public boolean isModLoaded(String modid) {
        return FabricLoader.getInstance().isModLoaded(modid);
    }

    @Override
    public boolean isKeyDown() {
        return InputConstants.isKeyDown(MC.getWindow().getWindow(), KeyBindingHelper.getBoundKeyOf(CleanerTooltips.hideTooltip).getValue());
    }
}