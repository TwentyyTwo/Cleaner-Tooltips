package net.twentyytwo.cleanertooltips.mixin;

import com.anthonyhilyard.legendarytooltips.tooltip.TooltipDecor;
import net.twentyytwo.cleanertooltips.compat.LegendaryTooltipsHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Mixin(TooltipDecor.class)
public abstract class TooltipDecorMixin {

    @ModifyVariable(method = "drawSeparator", at = @At("HEAD"), index = 2, argsOnly = true)
    private static int onDrawSeparator(int value) {
        return LegendaryTooltipsHandler.isModLoaded
                ? (value - LegendaryTooltipsHandler.increasedHeight)
                : value;
    }
}
