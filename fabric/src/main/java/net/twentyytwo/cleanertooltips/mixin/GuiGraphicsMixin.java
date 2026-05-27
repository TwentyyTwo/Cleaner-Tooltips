package net.twentyytwo.cleanertooltips.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.world.item.ItemStack;
import net.twentyytwo.cleanertooltips.CleanerTooltipsFabric;
import net.twentyytwo.cleanertooltips.util.StackHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {

    @ModifyVariable(method = "renderTooltipInternal", at = @At("HEAD"), index = 2, argsOnly = true)
    private List<ClientTooltipComponent> onRenderTooltipInternalHead(
            List<ClientTooltipComponent> components) {
        ItemStack stack = StackHolder.getInstance().getItemStack();
        return CleanerTooltipsFabric.getNewComponents(stack, components);
    }

    @Inject(method = "renderTooltipInternal", at = @At("RETURN"))
    private void onRenderTooltipInternalTail(Font font, List<ClientTooltipComponent> components,
                                             int mouseX, int mouseY,
                                             ClientTooltipPositioner tooltipPositioner,
                                             CallbackInfo ci) {
        // Reset the ItemStack at the tail to ensure that additional calls
        // to ItemStack#getTooltipLines do not override the ItemStack
        StackHolder.getInstance().resetStack();
    }
}
