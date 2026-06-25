package net.twentyytwo.cleanertooltips.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TooltipDisplay;
import net.twentyytwo.cleanertooltips.CleanerTooltips.IconAttributeComponent;
import net.twentyytwo.cleanertooltips.CleanerTooltips.IconDurabilityComponent;
import net.twentyytwo.cleanertooltips.util.TooltipsUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.function.Consumer;

import static net.twentyytwo.cleanertooltips.CleanerTooltips.MC;
import static net.twentyytwo.cleanertooltips.CleanerTooltips.config;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    // Adds either an attribute or durability icon component to the return value
    @ModifyReturnValue(method = "getTooltipImage", at = @At("RETURN"))
    private Optional<TooltipComponent> addCustomComponent(Optional<TooltipComponent> original) {
        ItemStack stack = (ItemStack) (Object) this;
        if (stack != null && !stack.isEmpty()) {
            if (TooltipsUtil.canAddAttributeTooltip(stack)) {
                return Optional.of(new IconAttributeComponent(stack));
            } else if (TooltipsUtil.canAddDurabilityTooltip(stack)) {
                return Optional.of(new IconDurabilityComponent(stack));
            }
        }
        return original;
    }

    // Hide the default attribute tooltip if the icon attributes are displayed
    @WrapWithCondition(
            method = "addDetailsToTooltip",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;addAttributeTooltips(Ljava/util/function/Consumer;Lnet/minecraft/world/item/component/TooltipDisplay;Lnet/minecraft/world/entity/player/Player;)V")
    )
    private boolean hideDefaultAttributes(ItemStack instance, Consumer<Component> tooltipAdder,
                                          TooltipDisplay tooltipDisplay, Player player) {
        return !TooltipsUtil.canAddAttributeTooltip(instance);
    }

    // Add the mining speed to the end of the attributes
    @Inject(method = "addAttributeTooltips", at = @At("TAIL"))
    private void addMiningSpeedTooltip(Consumer<Component> tooltipAdder, TooltipDisplay tooltipDisplay, Player player, CallbackInfo ci) {
        ItemStack thisStack = (ItemStack) (Object) this;
        if (config.general.miningSpeed && thisStack != null && !thisStack.isEmpty()) {
            float speed = TooltipsUtil.getDiggingSpeed(thisStack);
            if (speed > 0.0f) {
                tooltipAdder.accept(TooltipsUtil.getDiggingSpeedComponent(speed));
            }
        }
    }

    // Hide the mining efficiency attribute tooltip in favor of mining speed
    @WrapWithCondition(method = "method_57370",
                       at = @At(value = "INVOKE",
                                target = "Lnet/minecraft/world/item/ItemStack;addModifierTooltip(Ljava/util/function/Consumer;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/Holder;Lnet/minecraft/world/entity/ai/attributes/AttributeModifier;)V"))
    private boolean hideEfficiencyTooltip(ItemStack instance, Consumer<Component> tooltipAdder, Player player,
                                          Holder<Attribute> attribute, AttributeModifier modifier) {
        return !(config.general.miningSpeed && modifier.is(TooltipsUtil.EFFICIENCY));
    }

    // Fixes MC-271840
    @ModifyVariable(method = "addModifierTooltip", at = @At(value = "STORE"), ordinal = 1)
    private double addAttackDamage(double value, @Local(argsOnly = true) AttributeModifier modifier) {
        return MC != null && MC.player != null && modifier.is(Item.BASE_ATTACK_DAMAGE_ID)
                ? modifier.amount() + MC.player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE)
                + TooltipsUtil.getSharpnessBonus((ItemStack) (Object) this)
                : value;
    }
}
