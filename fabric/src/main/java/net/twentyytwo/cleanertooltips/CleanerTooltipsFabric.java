package net.twentyytwo.cleanertooltips;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.packs.PackType;
import net.twentyytwo.cleanertooltips.CleanerTooltips.IconAttributeComponent;
import net.twentyytwo.cleanertooltips.CleanerTooltips.IconAttributeTooltip;
import net.twentyytwo.cleanertooltips.CleanerTooltips.IconDurabilityComponent;
import net.twentyytwo.cleanertooltips.CleanerTooltips.IconDurabilityTooltip;
import net.twentyytwo.cleanertooltips.util.CleanerTooltipsUtil;
import net.twentyytwo.cleanertooltips.util.FabricAttributeManager;

public class CleanerTooltipsFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        CleanerTooltips.init();
        KeyBindingHelper.registerKeyBinding(CleanerTooltips.hideTooltip);

        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES)
                .registerReloadListener(new FabricAttributeManager());
        ClientTickEvents.START_CLIENT_TICK.register(client -> CleanerTooltipsUtil.onTick());

        // Register a mapping of IconAttributeComponent to IconAttributeModifierTooltip
        // or IconDurabilityComponent to IconDurabilityTooltip
        TooltipComponentCallback.EVENT.register(data -> {
            if (data instanceof IconAttributeComponent component) {
                return new IconAttributeTooltip(component);
            } else if (data instanceof IconDurabilityComponent component) {
                return new IconDurabilityTooltip(component);
            }
            return null;
        });
    }
}