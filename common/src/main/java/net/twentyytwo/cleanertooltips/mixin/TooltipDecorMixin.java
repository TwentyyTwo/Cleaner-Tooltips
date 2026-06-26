package net.twentyytwo.cleanertooltips.mixin;

import com.anthonyhilyard.legendarytooltips.tooltip.TooltipDecor;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.twentyytwo.cleanertooltips.util.ClientIconComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Pseudo
@Mixin(TooltipDecor.class)
public abstract class TooltipDecorMixin {

    @Definition(id = "components", local = @Local(type = List.class, argsOnly = true))
    @Definition(id = "get", method = "Ljava/util/List;get(I)Ljava/lang/Object;")
    @Definition(id = "ClientTextTooltip", type = ClientTextTooltip.class)
    @Expression("components.get(?) instanceof ClientTextTooltip")
    @ModifyExpressionValue(
            method = "drawBorder",
            at = @At("MIXINEXTRAS:EXPRESSION")
    )
    private static boolean addIconCondition(boolean original,
            @Local(argsOnly = true) List<ClientTooltipComponent> components,
            @Local(name = "i") int i) {
        return original || (components.get(i) instanceof ClientIconComponent);
    }
}
