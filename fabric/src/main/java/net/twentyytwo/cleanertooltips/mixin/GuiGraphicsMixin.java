package net.twentyytwo.cleanertooltips.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.twentyytwo.cleanertooltips.CleanerTooltips.IconAttributeModifierTooltip;
import net.twentyytwo.cleanertooltips.CleanerTooltips.IconDurabilityTooltip;
import net.twentyytwo.cleanertooltips.util.StackHolder;
import net.twentyytwo.cleanertooltips.util.CleanerTooltipsUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import static net.twentyytwo.cleanertooltips.CleanerTooltips.config;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {

    @ModifyVariable(method = "renderTooltipInternal", at = @At("HEAD"), index = 2, argsOnly = true)
    private List<ClientTooltipComponent> onRenderTooltipInternalHead(List<ClientTooltipComponent> components) {
        ItemStack stack = StackHolder.getInstance().getItemStack();
        if (stack != null && !stack.isEmpty()) {
            ItemAttributeModifiers modifiers = CleanerTooltipsUtil.getAttributeModifiers(stack);

            int nameIndex = CleanerTooltipsUtil.getIndexFabric(components);
            boolean shouldAdd = CleanerTooltipsUtil.shouldAddTooltip(modifiers);

            if (shouldAdd) {
                components.add(nameIndex, new IconAttributeModifierTooltip(stack, modifiers));
            }

            if (config.durability.durabilityEnabled && stack.getMaxDamage() > 0) {
                switch (config.durability.durabilityPos) {
                    case INLINE -> {
                        if (!shouldAdd) {
                            components.add(nameIndex, new IconDurabilityTooltip(stack));
                        }
                    }
                    case BELOW -> components.add(shouldAdd ? nameIndex + 1 : nameIndex, new IconDurabilityTooltip(stack));
                    case BOTTOM -> components.addLast(new IconDurabilityTooltip(stack));
                }
            }
        }
        return components;
    }

    @Inject(method = "renderTooltipInternal", at = @At("RETURN"))
    private void onRenderTooltipInternalTail(Font font, List<ClientTooltipComponent> components, int mouseX, int mouseY, ClientTooltipPositioner tooltipPositioner, CallbackInfo ci) {
        // Reset the ItemStack at the tail to ensure that additional calls to ItemStack#getTooltipLines do not override the ItemStack
        StackHolder.getInstance().resetStack();
    }
}
