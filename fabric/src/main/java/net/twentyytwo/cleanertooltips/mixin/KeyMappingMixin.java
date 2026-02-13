package net.twentyytwo.cleanertooltips.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.KeyMapping;
import net.twentyytwo.cleanertooltips.CleanerTooltips;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

@Mixin(KeyMapping.class)
public class KeyMappingMixin {

    @WrapOperation(method = "resetMapping", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private static Object onResetMapping(Map<Object, Object> instance, Object k, Object v, Operation<Object> original) {
        if (!v.equals(CleanerTooltips.hideTooltip)) {
            return original.call(instance, k, v);
        } else {
            return null;
        }
    }
}
