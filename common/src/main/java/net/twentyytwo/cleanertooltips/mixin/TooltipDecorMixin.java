package net.twentyytwo.cleanertooltips.mixin;

import com.anthonyhilyard.legendarytooltips.tooltip.TooltipDecor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import static net.twentyytwo.cleanertooltips.CleanerTooltips.isLegendaryTooltipsLoaded;
import static net.twentyytwo.cleanertooltips.CleanerTooltips.legendaryTooltipsIncreasedHeight;

@Pseudo
@Mixin(TooltipDecor.class)
public class TooltipDecorMixin {

    @ModifyVariable(method = "drawSeparator", at = @At("HEAD"), index = 2, argsOnly = true)
    private static int onDrawSeparator(int value) {
        if (isLegendaryTooltipsLoaded) {
            return value - legendaryTooltipsIncreasedHeight;
        } else {
            return value;
        }
    }
}
