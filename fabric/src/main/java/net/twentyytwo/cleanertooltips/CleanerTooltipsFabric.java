package net.twentyytwo.cleanertooltips;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;

@Environment(value = EnvType.CLIENT)
public class CleanerTooltipsFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CleanerTooltips.init();
        KeyBindingHelper.registerKeyBinding(CleanerTooltips.hideTooltip);
    }
}