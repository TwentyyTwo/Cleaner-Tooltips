package net.twentyytwo.cleanertooltips.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.twentyytwo.cleanertooltips.util.StackHolder;
import net.twentyytwo.cleanertooltips.util.CleanerTooltipsUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Consumer;

import static net.twentyytwo.cleanertooltips.CleanerTooltips.MC;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Inject(method = "getTooltipLines", at = @At("RETURN"))
    private void onGetTooltipLines(Item.TooltipContext tooltipContext, Player player, TooltipFlag tooltipFlag, CallbackInfoReturnable<List<Component>> cir) {
        // this method will always get called when a tooltip is rendered, this
        // ensures compatibility with mods like JEI.
        StackHolder.getInstance().setItemStack((ItemStack) (Object) this);
    }

    @WrapWithCondition(method = "getTooltipLines", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;addAttributeTooltips(Ljava/util/function/Consumer;Lnet/minecraft/world/entity/player/Player;)V"))
    private boolean hideDefaultAttributes(ItemStack instance, Consumer<Component> equipmentslotgroup, Player player) {
        return !(CleanerTooltipsUtil.shouldAddAttributes() && CleanerTooltipsUtil.hasAttributes(instance));
    }

    // Fixes MC-271840
    @ModifyVariable(method = "addModifierTooltip", at = @At(value = "STORE"), ordinal = 1)
    private double addAttackDamage(double value, @Local(argsOnly = true) AttributeModifier modifier) {
        return MC.player != null && modifier.is(Item.BASE_ATTACK_DAMAGE_ID)
                ? modifier.amount() + MC.player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE) + CleanerTooltipsUtil.getSharpnessBonus((ItemStack) (Object) this)
                : value;
    }
}
