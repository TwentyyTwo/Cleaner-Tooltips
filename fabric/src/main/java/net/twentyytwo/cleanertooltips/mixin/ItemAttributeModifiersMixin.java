package net.twentyytwo.cleanertooltips.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.twentyytwo.cleanertooltips.util.ItemStackHolder;
import net.twentyytwo.cleanertooltips.util.TooltipsUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import static net.twentyytwo.cleanertooltips.CleanerTooltips.MC;

@Mixin(ItemAttributeModifiers.Display.Default.class)
public abstract class ItemAttributeModifiersMixin implements ItemStackHolder {

    @Unique
    private ItemStack cleanertooltips$stack;

    @Override
    public void cleanerTooltips$setStack(ItemStack stack) {
        this.cleanertooltips$stack = stack;
    }

    // Fixes MC-271840
    @ModifyVariable(
            method = "apply",
            at = @At("STORE"),
            ordinal = 1
    )
    private double addAttackDamage(double e, @Local(argsOnly = true) AttributeModifier modifier) {
        if (MC != null && MC.player != null && modifier.is(Item.BASE_ATTACK_DAMAGE_ID)) {
            return modifier.amount() + TooltipsUtil.getBaseValue(Attributes.ATTACK_DAMAGE)
                    + TooltipsUtil.getSharpnessBonus(cleanertooltips$stack);
        }
        return e;
    }
}
