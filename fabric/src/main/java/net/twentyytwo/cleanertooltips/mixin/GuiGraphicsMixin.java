package net.twentyytwo.cleanertooltips.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.twentyytwo.cleanertooltips.CleanerTooltips.IconAttributeTooltip;
import net.twentyytwo.cleanertooltips.CleanerTooltips.IconDurabilityTooltip;
import net.twentyytwo.cleanertooltips.config.CleanerTooltipsConfig.PosValues;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

import static net.twentyytwo.cleanertooltips.CleanerTooltips.config;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {

    // Because the IconDurabilityComponent isn't added if the IconAttributeComponent is, it
    // has to be added here. Also shifts the position of the durability component if necessary.
    @ModifyVariable(method = "renderTooltipInternal", at = @At("HEAD"), index = 2, argsOnly = true)
    private List<ClientTooltipComponent> onRenderTooltipInternalHead(
            List<ClientTooltipComponent> components) {
        var position = config.durability.durabilityPos;
        if (config.durability.durabilityEnabled && position != PosValues.INLINE) {

            List<ClientTooltipComponent> newComponents = new ArrayList<>(components);
            for (int i = 0; i < components.size(); i++) {
                if (components.get(i) instanceof IconAttributeTooltip tooltip) {
                    var stack = tooltip.getStack();
                    if (stack.isDamageableItem()) {
                        if (position == PosValues.BELOW) {
                            newComponents.add(i + 1, new IconDurabilityTooltip(stack));
                        } else if (position == PosValues.BOTTOM) {
                            newComponents.add(new IconDurabilityTooltip(stack));
                        }
                    }
                    return newComponents;
                } else if (components.get(i) instanceof IconDurabilityTooltip) {
                    if (i != components.size() - 1 && position == PosValues.BOTTOM) {
                        newComponents.add(newComponents.remove(i));
                    }
                    return newComponents;
                }
            }
        }
        return components;
    }
}
