package net.twentyytwo.cleanertooltips.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.twentyytwo.cleanertooltips.CleanerTooltips.IconAttributeModifierTooltip;
import net.twentyytwo.cleanertooltips.CleanerTooltips.IconDurabilityTooltip;
import net.twentyytwo.cleanertooltips.api.IItemStackHolder;
import net.twentyytwo.cleanertooltips.config.CleanerTooltipsConfig;
import net.twentyytwo.cleanertooltips.util.CleanerTooltipsUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;
import java.util.Optional;

import static net.twentyytwo.cleanertooltips.CleanerTooltips.MC;
import static net.twentyytwo.cleanertooltips.CleanerTooltips.config;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {

    @Inject(method = "renderTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;renderTooltipInternal(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;)V"),
    locals = LocalCapture.CAPTURE_FAILSOFT)
    private void addTooltip(Font font, List<Component> tooltipLines, Optional<TooltipComponent> visualTooltipComponent, int mouseX, int mouseY, CallbackInfo ci, List<ClientTooltipComponent> list) {
        if (MC.screen instanceof IItemStackHolder holder) {
            ItemStack stack = holder.cleanerTooltips$getStack();
            ItemAttributeModifiers modifiers = CleanerTooltipsUtil.getAttributeModifiers(stack);

            int nameIndex = CleanerTooltipsUtil.getReplaceIndex(list);

            boolean shouldAdd = CleanerTooltipsUtil.shouldAddTooltip(modifiers);
            if (shouldAdd) {
                list.set(nameIndex, new IconAttributeModifierTooltip(stack, modifiers));
            }

            if (config.durability && stack.getMaxDamage() > 0) {
                switch (config.durabilityPos) {
                    case INLINE -> {
                        if (shouldAdd) {
                            return;
                        }
                        list.set(nameIndex, new IconDurabilityTooltip(stack));
                    }
                    case BELOW -> {
                        if (shouldAdd) {
                            list.add(nameIndex + 1, new IconDurabilityTooltip(stack));
                        } else {
                            list.set(nameIndex, new IconDurabilityTooltip(stack));
                        }
                    }
                    case BOTTOM -> list.addLast(new IconDurabilityTooltip(stack));
                }
            }
        }
    }

    @Inject(method = "renderTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;II)V", at = @At("HEAD"))
    private void onRenderTooltipHead(Font font, List<Component> tooltipLines, Optional<TooltipComponent> visualTooltipComponent, int mouseX, int mouseY, CallbackInfo ci) {
        if (MC.screen instanceof IItemStackHolder holder) {
            ItemStack stack = holder.cleanerTooltips$getStack();
            if ((CleanerTooltipsUtil.shouldAddTooltip(CleanerTooltipsUtil.getAttributeModifiers(stack)))
                    || (config.durability && stack.getMaxDamage() > 0)
                    && config.durabilityPos != CleanerTooltipsConfig.posValues.BOTTOM) {
                tooltipLines.add(1, Component.empty());
            }
        }
    }
}
