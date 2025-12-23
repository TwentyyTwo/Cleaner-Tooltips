package net.twentyytwo.cleanertooltips.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.twentyytwo.cleanertooltips.CleanerTooltips;
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

@Mixin(GuiGraphics.class)
public class GuiGraphicsMixin {

    @Inject(method = "renderTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;renderTooltipInternal(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;)V"),
    locals = LocalCapture.CAPTURE_FAILSOFT)
    private void addTooltip(Font font, List<Component> tooltipLines, Optional<TooltipComponent> visualTooltipComponent, int mouseX, int mouseY, CallbackInfo ci, List<ClientTooltipComponent> list) {
        int i = 0;
        for (ClientTooltipComponent tooltipComponent : list) System.out.println(i++ + " clientComponent " + tooltipComponent);
        if (CleanerTooltips.MC.screen instanceof IItemStackHolder holder) {
            ItemStack stack = holder.cleanerTooltips$getStack();
            ItemAttributeModifiers modifiers = CleanerTooltipsUtil.getAttributeModifiers(stack);

            if (CleanerTooltipsUtil.shouldAddTooltip(modifiers)) {

                // does not work for legendary tooltips
                int nameIndex = CleanerTooltipsUtil.getReplaceIndex(list);
                list.set(nameIndex, new CleanerTooltips.AttributeTooltip(stack, modifiers));

                if (CleanerTooltips.config.durability && CleanerTooltips.config.durabilityPos != CleanerTooltipsConfig.posValues.INLINE && stack.getMaxDamage() > 0) {
                    switch (CleanerTooltips.config.durabilityPos) {
                        case BELOW -> list.add(nameIndex + 1, new CleanerTooltips.DurabilityTooltip(stack));
                        case BOTTOM -> list.addLast(new CleanerTooltips.DurabilityTooltip(stack));
                    }
                }
            }
        }
    }

    @Inject(method = "renderTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;II)V", at = @At("HEAD"))
    private void onRenderTooltipHead(Font font, List<Component> tooltipLines, Optional<TooltipComponent> visualTooltipComponent, int mouseX, int mouseY, CallbackInfo ci) {
        if (CleanerTooltips.MC.screen instanceof IItemStackHolder holder) {
            ItemStack stack = holder.cleanerTooltips$getStack();
            if (CleanerTooltipsUtil.shouldAddTooltip(CleanerTooltipsUtil.getAttributeModifiers(stack))) tooltipLines.add(1, Component.empty());
        }
    }
}
