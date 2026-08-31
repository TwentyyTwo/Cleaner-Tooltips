package net.twentyytwo.cleanertooltips.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.xylonity.tooltipoverhaul.client.util.TextUtils;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.twentyytwo.cleanertooltips.CleanerTooltips.IconAttributeTooltip;
import net.twentyytwo.cleanertooltips.CleanerTooltips.IconDurabilityTooltip;
import net.twentyytwo.cleanertooltips.util.AttributeHelper;
import net.twentyytwo.cleanertooltips.util.TooltipsUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

import static net.twentyytwo.cleanertooltips.CleanerTooltips.config;

@Pseudo
@Mixin(TextUtils.class)
public abstract class TextUtilsMixin {

    @ModifyReturnValue(method = "getTooltipComponentsFrom", at = @At("RETURN"))
    private static List<ClientTooltipComponent> addMissingComponent(
            List<ClientTooltipComponent> original, @Local(argsOnly = true) ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            boolean shouldAddAttributes = AttributeHelper.canAddIconAttributes(stack);
            boolean shouldAddDurability = TooltipsUtil.canAddDurabilityTooltip(stack);

            if (!(shouldAddAttributes || shouldAddDurability)) return original;

            int index = original.isEmpty() ? 0 : 1;
            if (shouldAddAttributes) {
                original.add(index, IconAttributeTooltip.fromStack(stack));
                index++;
            }

            if (shouldAddDurability) {
                switch (config.durabilityPos) {
                    case INLINE -> {
                        if (!shouldAddAttributes) {
                            original.add(index, new IconDurabilityTooltip(stack));
                        }
                    }
                    case BELOW -> original.add(index, new IconDurabilityTooltip(stack));
                    case BOTTOM -> original.addLast(new IconDurabilityTooltip(stack));
                }
            }
        }
        return original;
    }
}
