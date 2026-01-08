package net.twentyytwo.cleanertooltips.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.common.util.AttributeUtil;
import net.twentyytwo.cleanertooltips.CleanerTooltips;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(AttributeUtil.class)
public abstract class AttributeUtilMixin {

    // Fixes the attack damage not changing with sharpness
    @ModifyVariable(method = "applyTextFor",
            at = @At(value = "STORE"),
            name = "base")
        private static double modifyAttackDamage(double amt, @Local(argsOnly = true) ItemStack stack, @Local(name = "attr") Holder<Attribute> attr) {
        if (CleanerTooltips.config.sharpness && CleanerTooltips.MC.level != null && attr.equals(Attributes.ATTACK_DAMAGE)) {
            int sharpnessLevel = stack.getEnchantmentLevel(CleanerTooltips.MC.level.registryAccess().
                    lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(Enchantments.SHARPNESS));
            if (sharpnessLevel > 0) amt += (0.5 * sharpnessLevel) + 0.5;
        }
        return amt;
    }
}
