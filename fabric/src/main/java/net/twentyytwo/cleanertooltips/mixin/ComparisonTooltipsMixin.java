package net.twentyytwo.cleanertooltips.mixin;

import com.anthonyhilyard.equipmentcompare.gui.ComparisonTooltips;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.twentyytwo.cleanertooltips.CleanerTooltipsFabric;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;

@Pseudo
@Mixin(ComparisonTooltips.class)
public class ComparisonTooltipsMixin {

    @ModifyVariable(method = "render", at = @At("STORE"), name = "itemStackTooltipLines")
    private static List<ClientTooltipComponent> onRender(List<ClientTooltipComponent> components, @Local(ordinal = 0, argsOnly = true) ItemStack stack) {
        return CleanerTooltipsFabric.getNewComponents(stack, components);
    }
}
