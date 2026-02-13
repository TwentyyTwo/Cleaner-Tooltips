package net.twentyytwo.cleanertooltips.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.twentyytwo.cleanertooltips.CleanerTooltips;
import net.twentyytwo.cleanertooltips.util.CleanerTooltipsUtil;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    // Hides the default attributes when the icon attributes are displayed
    @Inject( method = "addAttributeTooltips", at = @At("HEAD"), cancellable = true)
    private void hideDefaultAttributes(Consumer<Component> tooltipAdder, @Nullable Player player, CallbackInfo ci) {
        if (CleanerTooltipsUtil.shouldAddTooltip(CleanerTooltipsUtil.getAttributeModifiers((ItemStack) (Object) this))
                && !InputConstants.isKeyDown(CleanerTooltips.MC.getWindow().getWindow(), KeyBindingHelper.getBoundKeyOf(CleanerTooltips.hideTooltip).getValue())) {
            ci.cancel();
        }
    }

    // Fixes MC-271840
    @ModifyVariable(method = "addModifierTooltip", at = @At(value = "STORE"), ordinal = 1)
    private double addAttackDamage(double value, @Local(argsOnly = true) AttributeModifier modifier) {
        if (CleanerTooltips.MC.player != null && modifier.is(Item.BASE_ATTACK_DAMAGE_ID))
            return modifier.amount() + CleanerTooltips.MC.player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE) + CleanerTooltipsUtil.getSharpnessBonus((ItemStack)(Object)this);
        return value;
    }
}
