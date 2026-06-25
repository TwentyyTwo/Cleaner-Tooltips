package net.twentyytwo.cleanertooltips.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.AttributeTooltipContext;
import net.neoforged.neoforge.common.util.AttributeUtil;
import net.twentyytwo.cleanertooltips.util.CleanerTooltipsUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

import static net.twentyytwo.cleanertooltips.CleanerTooltips.MC;
import static net.twentyytwo.cleanertooltips.CleanerTooltips.config;

@Mixin(AttributeUtil.class)
public abstract class AttributeUtilMixin {

    @Inject(method = "applyModifierTooltips", at = @At("TAIL"))
    private static void addMiningSpeedTooltip(ItemStack stack, Consumer<Component> tooltip,
                                              AttributeTooltipContext ctx, CallbackInfo ci) {
        if (config.general.miningSpeed && stack != null && !stack.isEmpty()) {
            float speed = CleanerTooltipsUtil.getDiggingSpeed(stack);
            if (speed > 0.0f) {
                tooltip.accept(CleanerTooltipsUtil.getDiggingSpeedComponent(speed));
            }
        }
    }

    // Fixes MC-271840
    @ModifyVariable(method = "applyTextFor",
            at = @At(value = "STORE"),
            name = "base")
    private static double modifyAttackDamage(double base,
                                             @Local(argsOnly = true) ItemStack stack,
                                             @Local(name = "attr") Holder<Attribute> attr,
                                             @Local(name = "entityBase") double entityBase) {
        return MC != null && MC.player != null && attr.value().getBaseId() == Item.BASE_ATTACK_DAMAGE_ID
                ? base - entityBase + MC.player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE)
                + CleanerTooltipsUtil.getSharpnessBonus(stack)
                : base;
    }
}
