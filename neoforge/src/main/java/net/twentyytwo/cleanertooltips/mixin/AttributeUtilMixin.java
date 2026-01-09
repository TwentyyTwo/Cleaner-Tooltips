package net.twentyytwo.cleanertooltips.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
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
        private static double modifyAttackDamage(double base, @Local(argsOnly = true) ItemStack stack, @Local(name = "attr") Holder<Attribute> attr, @Local(name = "entityBase") double entityBase) {

        if (CleanerTooltips.config.sharpness && CleanerTooltips.MC.player != null  && attr.value().getBaseId() == Item.BASE_ATTACK_DAMAGE_ID) {
            double modifierValue = (base - entityBase) + CleanerTooltips.MC.player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);

            ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            for (var entry : enchantments.entrySet()) {
                if (entry.getKey().unwrapKey().isPresent() && entry.getKey().unwrapKey().get() == Enchantments.SHARPNESS  && entry.getIntValue() > 0) {
                    modifierValue += (0.5 * entry.getIntValue()) + 0.5;
                    break;
                }
            }
            return modifierValue;
        }
        return base;
    }
}
