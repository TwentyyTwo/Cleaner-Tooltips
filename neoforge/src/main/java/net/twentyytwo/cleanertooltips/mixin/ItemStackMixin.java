package net.twentyytwo.cleanertooltips.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.twentyytwo.cleanertooltips.CleanerTooltips.IconAttributeComponent;
import net.twentyytwo.cleanertooltips.CleanerTooltips.IconDurabilityComponent;
import net.twentyytwo.cleanertooltips.util.CleanerTooltipsUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @ModifyReturnValue(method = "getTooltipImage", at = @At("RETURN"))
    private Optional<TooltipComponent> addCustomComponent(Optional<TooltipComponent> original) {
        ItemStack stack = (ItemStack) (Object) this;
        if (stack != null && !stack.isEmpty()) {
            if (CleanerTooltipsUtil.canAddAttributeTooltip(stack)) {
                return Optional.of(new IconAttributeComponent(stack));
            } else if (CleanerTooltipsUtil.canAddDurabilityTooltip(stack)) {
                return Optional.of(new IconDurabilityComponent(stack));
            }
        }
        return original;
    }
}
