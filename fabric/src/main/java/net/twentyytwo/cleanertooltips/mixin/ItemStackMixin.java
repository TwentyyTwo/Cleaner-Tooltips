package net.twentyytwo.cleanertooltips.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.twentyytwo.cleanertooltips.CleanerTooltips;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(ItemStack.class)
public class ItemStackMixin {

    @Inject( method = "addAttributeTooltips", at = @At("HEAD"), cancellable = true)
    private void hideDefaultAttributes(Consumer<Component> tooltipAdder, @Nullable Player player, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (!InputConstants.isKeyDown(mc.getWindow().getWindow(), ((KeyMappingAccessor) CleanerTooltips.hideTooltip).getKey().getValue())) {
            ci.cancel();
        }
    }
}
