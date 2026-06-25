package net.twentyytwo.cleanertooltips;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.server.packs.PackType;
import net.twentyytwo.cleanertooltips.CleanerTooltips.IconAttributeComponent;
import net.twentyytwo.cleanertooltips.CleanerTooltips.IconAttributeTooltip;
import net.twentyytwo.cleanertooltips.CleanerTooltips.IconDurabilityComponent;
import net.twentyytwo.cleanertooltips.CleanerTooltips.IconDurabilityTooltip;
import net.twentyytwo.cleanertooltips.config.CleanerTooltipsConfig.PosValues;
import net.twentyytwo.cleanertooltips.util.TooltipsUtil;
import net.twentyytwo.cleanertooltips.util.FabricAttributeManager;

import java.util.ArrayList;
import java.util.List;

import static net.twentyytwo.cleanertooltips.CleanerTooltips.config;

public class CleanerTooltipsFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        CleanerTooltips.init();
        KeyBindingHelper.registerKeyBinding(CleanerTooltips.hideTooltip);

        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES)
                .registerReloadListener(new FabricAttributeManager());
        ClientTickEvents.START_CLIENT_TICK.register(client -> TooltipsUtil.onTick());

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

    /**
     * Returns a list containing any missing components and/or shifts their position.
     * @param components    the list of {@link ClientTooltipComponent}.
     * @return              the modified list.
     */
    public static List<ClientTooltipComponent> getMissingComponents(
            List<ClientTooltipComponent> components) {
        PosValues position = config.durability.durabilityPos;

        if (!config.durability.durabilityEnabled || position == PosValues.INLINE) {
            return components;
        }

        List<ClientTooltipComponent> newList = new ArrayList<>(components);
        for (int i = 0; i < newList.size(); i++) {
            if (newList.get(i) instanceof IconAttributeTooltip tooltip) {
                if (tooltip.getStack().isDamageableItem()) {
                    int index = position == PosValues.BELOW ? i + 1 : newList.size();
                    newList.add(index, new IconDurabilityTooltip(tooltip.getStack()));
                }
                return newList;
            } else if (newList.get(i) instanceof IconDurabilityTooltip) {
                if (i != newList.size() - 1 && position == PosValues.BOTTOM) {
                    newList.add(newList.remove(i));
                }
                return newList;
            }
        }

        return components;
    }
}