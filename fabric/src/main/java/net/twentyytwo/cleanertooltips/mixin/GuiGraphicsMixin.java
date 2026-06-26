package net.twentyytwo.cleanertooltips.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.twentyytwo.cleanertooltips.CleanerTooltipsFabric;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {

    // Because the IconDurabilityComponent isn't added if the IconAttributeComponent is, it
    // has to be added here. Also shifts the position of the durability component if necessary.
    @ModifyVariable(method = "renderTooltip", at = @At("HEAD"), index = 2, argsOnly = true)
    private List<ClientTooltipComponent> onRenderTooltipInternalHead(
            List<ClientTooltipComponent> components) {
        return CleanerTooltipsFabric.getMissingComponents(components);
    }
}
