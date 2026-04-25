package net.twentyytwo.cleanertooltips.mixin;

import com.anthonyhilyard.iceberg.component.TitleBreakComponent;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.twentyytwo.cleanertooltips.compat.LegendaryTooltipsCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(TitleBreakComponent.class)
public abstract class TitleBreakComponentMixin {

    @ModifyReturnValue(method = "getHeight", at = @At("RETURN"))
    private int onGetHeight(int value) {
        return LegendaryTooltipsCompat.isModLoaded ? (value + LegendaryTooltipsCompat.increasedHeight) : value;
    }
}
