package net.twentyytwo.cleanertooltips.mixin;

import net.bettercombat.client.WeaponAttributeTooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.twentyytwo.cleanertooltips.util.CleanerTooltipsUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Pseudo
@Mixin(WeaponAttributeTooltip.class)
public class WeaponAttributeTooltipMixin {

    @Inject(method = "modifyTooltip", at = @At("HEAD"), cancellable = true)
    private static void onModifyTooltip(ItemStack itemStack, List<Component> lines, CallbackInfo ci) {
        if (CleanerTooltipsUtil.shouldAddTooltip(CleanerTooltipsUtil.getAttributeModifiers(itemStack))) {
            ci.cancel(); // needs key check
        }
    }
}
