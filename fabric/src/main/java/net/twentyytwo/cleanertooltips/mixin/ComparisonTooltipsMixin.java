package net.twentyytwo.cleanertooltips.mixin;

import com.anthonyhilyard.equipmentcompare.gui.ComparisonTooltips;
import com.anthonyhilyard.iceberg.component.TitleBreakComponent;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.twentyytwo.cleanertooltips.CleanerTooltipsFabric;
import net.twentyytwo.cleanertooltips.compat.LegendaryTooltipsCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

@Pseudo
@Mixin(ComparisonTooltips.class)
public class ComparisonTooltipsMixin {

    @ModifyVariable(method = "render", at = @At("STORE"), name = "itemStackTooltipLines")
    private static List<ClientTooltipComponent> onRender(List<ClientTooltipComponent> components, @Local(ordinal = 0, argsOnly = true) ItemStack stack) {
        return CleanerTooltipsFabric.getNewComponents(stack, components);
    }

    @ModifyVariable(method = "render", at = @At("STORE"), name = "equippedTooltipLines")
    private static List<ClientTooltipComponent> addMissingTitleBreak(List<ClientTooltipComponent> components) {
        // If the default attributes are hidden, Iceberg does not add a TitleBreakComponent, which
        // breaks the EquipmentCompare tooltip rendering if we try to add our ClientTooltipComponent.
        if (!LegendaryTooltipsCompat.hasTitleBreak(components)) {
            List<ClientTooltipComponent> newComponents = new ArrayList<>(components);
            for (int i = 0; i < newComponents.size(); i++) {
                ClientTooltipComponent component = newComponents.get(i);
                if (component instanceof ClientTextTooltip) {
                    newComponents.add(i + 1, new TitleBreakComponent());
                    return newComponents;
                }
            }
        }
        return components;
    }
}
