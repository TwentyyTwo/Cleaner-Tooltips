package net.twentyytwo.cleanertooltips;

import com.anthonyhilyard.iceberg.component.TitleBreakComponent;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.twentyytwo.cleanertooltips.CleanerTooltips.IconAttributeModifierTooltip;
import net.twentyytwo.cleanertooltips.CleanerTooltips.IconDurabilityTooltip;
import net.twentyytwo.cleanertooltips.compat.LegendaryTooltipsCompat;
import net.twentyytwo.cleanertooltips.util.CleanerTooltipsUtil;
import net.twentyytwo.cleanertooltips.util.FabricAttributeManager;

import java.util.ArrayList;
import java.util.List;

import static net.twentyytwo.cleanertooltips.CleanerTooltips.config;

public class CleanerTooltipsFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        CleanerTooltips.init();
        KeyBindingHelper.registerKeyBinding(CleanerTooltips.hideTooltip);

        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new FabricAttributeManager());
        ClientTickEvents.START_CLIENT_TICK.register(client -> CleanerTooltipsUtil.onTick());
    }

    public static List<ClientTooltipComponent> getNewComponents(ItemStack stack, List<ClientTooltipComponent> components) {
        List<ClientTooltipComponent> newComponents = new ArrayList<>(components);
        if (stack != null && !stack.isEmpty()) {
            ItemAttributeModifiers modifiers = CleanerTooltipsUtil.getAttributeModifiers(stack);
            boolean shouldAddAttributes = CleanerTooltipsUtil.shouldAddAttributes() && CleanerTooltipsUtil.hasAttributes(modifiers);
            boolean shouldAddDurability = config.durability.durabilityEnabled && stack.getMaxDamage() > 0;

            if (LegendaryTooltipsCompat.isModLoaded && !LegendaryTooltipsCompat.hasTitleBreak(newComponents) && (shouldAddAttributes || shouldAddDurability)) {
                for (int i = 0; i < newComponents.size(); i++) {
                    if (newComponents.get(i) instanceof ClientTextTooltip) {
                        newComponents.add(i + 1, new TitleBreakComponent());
                        break;
                    }
                }
            }

            int nameIndex = CleanerTooltipsUtil.getIndexFabric(newComponents);
            if (shouldAddAttributes) {
                newComponents.add(nameIndex, new IconAttributeModifierTooltip(stack, modifiers));
            }

            if (shouldAddDurability) {
                switch (config.durability.durabilityPos) {
                    case INLINE -> {
                        if (!shouldAddAttributes) {
                            newComponents.add(nameIndex, new IconDurabilityTooltip(stack));
                        }
                    }
                    case BELOW -> newComponents.add(shouldAddAttributes ? nameIndex + 1 : nameIndex, new IconDurabilityTooltip(stack));
                    case BOTTOM -> newComponents.addLast(new IconDurabilityTooltip(stack));
                }
            }
        }
        return newComponents;
    }
}