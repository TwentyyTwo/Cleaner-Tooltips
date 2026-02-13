package net.twentyytwo.cleanertooltips.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;
import java.util.Optional;

@Mixin(GuiGraphics.class)
public class GuiGraphicsMixin {

    @Unique
    private static boolean IsKeyNotDown() {
        return !InputConstants.isKeyDown(CleanerTooltips.MC.getWindow().getWindow(), KeyBindingHelper.getBoundKeyOf(CleanerTooltips.hideTooltip).getValue());
    }

    @Inject(method = "renderTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;renderTooltipInternal(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;)V"),
    locals = LocalCapture.CAPTURE_FAILSOFT)
    private void addTooltip(Font font, List<Component> tooltipLines, Optional<TooltipComponent> visualTooltipComponent, int mouseX, int mouseY, CallbackInfo ci, List<ClientTooltipComponent> list) {
        if (CleanerTooltips.MC.screen instanceof IItemStackHolder holder) {
            ItemStack stack = holder.cleanerTooltips$getStack();
            ItemAttributeModifiers modifiers = CleanerTooltipsUtil.getAttributeModifiers(stack);

            int nameIndex = CleanerTooltipsUtil.getReplaceIndex(list);

            boolean shouldAdd = CleanerTooltipsUtil.shouldAddTooltip(modifiers) && IsKeyNotDown();
            if (shouldAdd) list.set(nameIndex, new CleanerTooltips.AttributeTooltip(stack, modifiers));

            if (CleanerTooltips.config.durability && stack.getMaxDamage() > 0) {
                switch (CleanerTooltips.config.durabilityPos) {
                    case INLINE -> {
                        if (shouldAdd) return;
                        list.set(nameIndex, new CleanerTooltips.DurabilityTooltip(stack));
                    }
                    case BELOW -> {
                        if (shouldAdd) list.add(nameIndex + 1, new CleanerTooltips.DurabilityTooltip(stack));
                        else list.set(nameIndex, new CleanerTooltips.DurabilityTooltip(stack));
                    }
                    case BOTTOM -> list.addLast(new CleanerTooltips.DurabilityTooltip(stack));
                }
            }
        }
    }

    @Inject(method = "renderTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;II)V", at = @At("HEAD"))
    private void onRenderTooltipHead(Font font, List<Component> tooltipLines, Optional<TooltipComponent> visualTooltipComponent, int mouseX, int mouseY, CallbackInfo ci) {
        if (CleanerTooltips.MC.screen instanceof IItemStackHolder holder) {
            ItemStack stack = holder.cleanerTooltips$getStack();
            if ((CleanerTooltipsUtil.shouldAddTooltip(CleanerTooltipsUtil.getAttributeModifiers(stack)) && IsKeyNotDown())
                    || (CleanerTooltips.config.durability && stack.getMaxDamage() > 0)
                    && CleanerTooltips.config.durabilityPos != CleanerTooltipsConfig.posValues.BOTTOM) tooltipLines.add(1, Component.empty());
        }
    }
}
