package net.twentyytwo.cleanertooltips.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.xylonity.tooltipoverhaul.client.util.TextUtils;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.twentyytwo.cleanertooltips.CleanerTooltipsFabric;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Pseudo
@Mixin(TextUtils.class)
public abstract class TextUtilsMixin {

    @WrapMethod(method = "lambda$getTooltipComponentsFrom$0")
    private static void checkForComponents(List<Component> originalComponentLines,
                                           List<ClientTooltipComponent> componentList,
                                           TooltipComponent component, Operation<Void> original) {
        componentList.add(componentList.isEmpty() ? 0 : 1, ClientTooltipComponent.create(component));
    }

    @ModifyReturnValue(method = "getTooltipComponentsFrom", at = @At("RETURN"))
    private static List<ClientTooltipComponent> change(List<ClientTooltipComponent> original) {
        return CleanerTooltipsFabric.getMissingComponents(original);
    }
}
