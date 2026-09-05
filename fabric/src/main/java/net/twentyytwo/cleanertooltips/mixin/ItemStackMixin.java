package net.twentyytwo.cleanertooltips.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TooltipDisplay;
import net.twentyytwo.cleanertooltips.CleanerTooltips;
import net.twentyytwo.cleanertooltips.util.AttributeHelper;
import net.twentyytwo.cleanertooltips.util.TooltipsUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.function.Consumer;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Unique
    private static ItemStack cleanerTooltipsStack = ItemStack.EMPTY;

    // Adds either an attribute or durability icon component to the return value
    @ModifyReturnValue(method = "getTooltipImage", at = @At("RETURN"))
    private Optional<TooltipComponent> addCustomComponent(Optional<TooltipComponent> original) {
        ItemStack stack = (ItemStack) (Object) this;
        if (stack != null && !stack.isEmpty()) {
            if (AttributeHelper.canAddIconAttributes(stack)) {
                return Optional.of(new CleanerTooltips.IconAttributeComponent(stack));
            } else if (TooltipsUtil.canAddDurabilityTooltip(stack)) {
                return Optional.of(new CleanerTooltips.IconDurabilityComponent(stack));
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
        return !AttributeHelper.isViableForIcons();
    }

    // Add the mining speed to the end of the attributes
    @Inject(method = "addAttributeTooltips", at = @At("TAIL"))
    private void addMiningSpeedTooltip(Consumer<Component> tooltipAdder, TooltipDisplay tooltipDisplay, Player player, CallbackInfo ci) {
        ItemStack thisStack = (ItemStack) (Object) this;
        if (CleanerTooltips.config.miningSpeed && thisStack != null && !thisStack.isEmpty()) {
            float speed = TooltipsUtil.getDiggingSpeed(thisStack);
            if (speed > 0.0f) {
                tooltipAdder.accept(TooltipsUtil.getDiggingSpeedComponent(speed));
            }
        }
    }

    @Inject(
            method = "addAttributeTooltips",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;forEachModifier(Lnet/minecraft/world/entity/EquipmentSlotGroup;Lorg/apache/commons/lang3/function/TriConsumer;)V")
    )
    private void setStack(Consumer<Component> tooltipAdder,
                          TooltipDisplay tooltipDisplay,
                          Player player, CallbackInfo ci) {
        // Because you cannot access the current "this" instance inside
        // a lambda mixin, we'll get it here when the lambda is invoked.
        cleanerTooltipsStack = (ItemStack) (Object) this;
    }

    @ModifyArg(
            method = "method_57370",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/component/ItemAttributeModifiers$Display;apply(Ljava/util/function/Consumer;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/Holder;Lnet/minecraft/world/entity/ai/attributes/AttributeModifier;)V"),
            index = 3
    )
    private static AttributeModifier addAttackDamage(AttributeModifier modifier) {
        if (modifier.is(Item.BASE_ATTACK_DAMAGE_ID)) {
            double amount = modifier.amount() + TooltipsUtil.getSharpnessBonus(cleanerTooltipsStack);
            return new AttributeModifier(modifier.id(), amount, modifier.operation());
        }
        return modifier;
    }
}
