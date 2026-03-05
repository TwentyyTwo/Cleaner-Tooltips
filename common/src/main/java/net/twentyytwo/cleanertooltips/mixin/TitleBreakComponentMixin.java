package net.twentyytwo.cleanertooltips.mixin;

import com.anthonyhilyard.iceberg.component.TitleBreakComponent;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import static net.twentyytwo.cleanertooltips.CleanerTooltips.isLegendaryTooltipsLoaded;
import static net.twentyytwo.cleanertooltips.CleanerTooltips.legendaryTooltipsIncreasedHeight;

@Pseudo
@Mixin(TitleBreakComponent.class)
public class TitleBreakComponentMixin {

    @ModifyReturnValue(method = "getHeight", at = @At("RETURN"))
    private int onGetHeight(int original) {
        if (isLegendaryTooltipsLoaded) {
            return original + legendaryTooltipsIncreasedHeight;
        } else {
            return original;
        }
    }
}
