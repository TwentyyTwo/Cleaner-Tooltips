package net.twentyytwo.cleanertooltips;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.twentyytwo.cleanertooltips.util.CleanerTooltipsUtil;

import java.util.ArrayList;
import java.util.List;

import static net.twentyytwo.cleanertooltips.CleanerTooltips.config;

public class CleanerTooltipsFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        CleanerTooltips.init();
        KeyBindingHelper.registerKeyBinding(CleanerTooltips.hideTooltip);
    }

    public static List<ClientTooltipComponent> getNewComponents(ItemStack stack, List<ClientTooltipComponent> components) {
        List<ClientTooltipComponent> newComponents = new ArrayList<>(components);
        if (stack != null && !stack.isEmpty()) {
            ItemAttributeModifiers modifiers = CleanerTooltipsUtil.getAttributeModifiers(stack);

            int nameIndex = CleanerTooltipsUtil.getIndexFabric(components);
            boolean shouldAdd = CleanerTooltipsUtil.shouldAddTooltip(modifiers);

            if (shouldAdd) {
                newComponents.add(nameIndex, new CleanerTooltips.IconAttributeModifierTooltip(stack, modifiers));
            }

            if (config.durability.durabilityEnabled && stack.getMaxDamage() > 0) {
                switch (config.durability.durabilityPos) {
                    case INLINE -> {
                        if (!shouldAdd) {
                            newComponents.add(nameIndex, new CleanerTooltips.IconDurabilityTooltip(stack));
                        }
                    }
                    case BELOW -> newComponents.add(shouldAdd ? nameIndex + 1 : nameIndex, new CleanerTooltips.IconDurabilityTooltip(stack));
                    case BOTTOM -> newComponents.addLast(new CleanerTooltips.IconDurabilityTooltip(stack));
                }
            }
        }
        return newComponents;
    }
}