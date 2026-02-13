package net.twentyytwo.cleanertooltips.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.AttributeUtil;
import net.twentyytwo.cleanertooltips.CleanerTooltips;
import net.twentyytwo.cleanertooltips.util.CleanerTooltipsUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Objects;

@Mixin(AttributeUtil.class)
public abstract class AttributeUtilMixin {

    // Fixes MC-271840
    @ModifyVariable(method = "applyTextFor",
            at = @At(value = "STORE"),
            name = "base")
    private static double modifyAttackDamage(double base, @Local(argsOnly = true) ItemStack stack, @Local(name = "attr") Holder<Attribute> attr, @Local(name = "entityBase") double entityBase) {
        if (CleanerTooltips.MC.player != null && Objects.equals(attr.value().getBaseId(), Item.BASE_ATTACK_DAMAGE_ID))
            return (base - entityBase) + CleanerTooltips.MC.player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE) + CleanerTooltipsUtil.getSharpnessBonus(stack);
        return base;
    }
}
