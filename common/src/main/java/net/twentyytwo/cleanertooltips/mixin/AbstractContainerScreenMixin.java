package net.twentyytwo.cleanertooltips.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.twentyytwo.cleanertooltips.IStackHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenMixin<T extends AbstractContainerMenu> {

    @Final
    @Shadow
    protected T menu;

    @Shadow
    protected Slot hoveredSlot;

    @Inject( method = "renderTooltip", at = @At(value = "HEAD") )
    private void getItemStack(GuiGraphics guiGraphics, int x, int y, CallbackInfo ci) {
        if (menu.getCarried().isEmpty() && hoveredSlot != null && hoveredSlot.hasItem())
            ((IStackHolder) this).cleanerTooltips$setStack(hoveredSlot.getItem());
    }
}
