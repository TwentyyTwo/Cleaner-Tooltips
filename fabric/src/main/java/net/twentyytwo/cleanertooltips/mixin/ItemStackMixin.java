package net.twentyytwo.cleanertooltips.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
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
        if (CleanerTooltipsUtil.shouldAddTooltip(CleanerTooltipsUtil.getAttributeModifiers((ItemStack) (Object) this))) {
            ci.cancel();
        }
    }

    // Fixes MC-271840
    @ModifyVariable(method = "addModifierTooltip", at = @At(value = "STORE"), ordinal = 1)
    private double addAttackDamage(double value, @Local(argsOnly = true) AttributeModifier modifier) {
        if (CleanerTooltips.config.sharpness && CleanerTooltips.MC.player != null && modifier.is(Item.BASE_ATTACK_DAMAGE_ID)) {
            double modifierValue = modifier.amount() + CleanerTooltips.MC.player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);

            ItemEnchantments enchantments = ((ItemStack)(Object)this).getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            for (var entry : enchantments.entrySet()) {
                if (entry.getKey().unwrapKey().get() == Enchantments.SHARPNESS  && entry.getIntValue() > 0) {
                    modifierValue += (0.5 * entry.getIntValue()) + 0.5;
                    break;
                }
            }
            return modifierValue;
        }
        return value;
    }
}
