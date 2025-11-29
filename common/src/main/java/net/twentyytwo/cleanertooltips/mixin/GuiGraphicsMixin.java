package net.twentyytwo.cleanertooltips.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.datafixers.util.Either;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.twentyytwo.cleanertooltips.CleanerTooltips;
import net.twentyytwo.cleanertooltips.IStackHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {

    @Unique
    List<Either<FormattedText, TooltipComponent>> cleanerTooltips$elements;

    @Inject( method = "renderTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;II)V", at = @At(value = "HEAD") )
    private void getElements(Font font, List<Component> tooltipLines, Optional<TooltipComponent> visualTooltipComponent, int mouseX, int mouseY, CallbackInfo ci) {
        this.cleanerTooltips$elements = ((List<? extends FormattedText>) tooltipLines).stream()
                .map((Function<FormattedText, Either<FormattedText, TooltipComponent>>) Either::left)
                .collect(Collectors.toCollection(ArrayList::new));
        visualTooltipComponent.ifPresent(component -> this.cleanerTooltips$elements.add(1, Either.right(component)));
    }

    @Shadow
    protected abstract void renderTooltipInternal(Font font, List<ClientTooltipComponent> components, int mouseX, int mouseY, ClientTooltipPositioner tooltipPositioner);

    @Redirect(
            method = "renderTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;II)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderTooltipInternal(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;)V"))
    private void makeMutable(GuiGraphics instance, Font font, List<ClientTooltipComponent> components, int mouseX, int mouseY, ClientTooltipPositioner tooltipPositioner) {
        List<ClientTooltipComponent> mutableComponents = new ArrayList<>(components);
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof IStackHolder holder) {
            ItemStack stack = holder.cleanerTooltips$getStack();

            if (!stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).modifiers().isEmpty() && mc.player != null &&
            !InputConstants.isKeyDown(mc.getWindow().getWindow(), ((KeyMappingAccessor) CleanerTooltips.hideTooltip).getKey().getValue()) && CleanerTooltips.config.enabled) {

                // Ensures the tooltip is always below the name, which ensures better compatibility between other mods
                Component itemName = stack.getHoverName();
                int nameIndex = -1;
                for (int i = 0; i < this.cleanerTooltips$elements.size(); i++) {
                    var component = this.cleanerTooltips$elements.get(i);
                    if (component.left().isPresent() && component.left().get().getString().equals(itemName.getString())) {
                        nameIndex = i;
                        break;
                    }
                }
                mutableComponents.add((nameIndex >= 0) ? nameIndex + 1 : 1, new CleanerTooltips.AttributeTooltip(stack));

                if (CleanerTooltips.config.durability && stack.getMaxDamage() > 0) {
                    switch (CleanerTooltips.config.durabilityPos) {
                        case BELOW -> mutableComponents.add((nameIndex >= 0) ? nameIndex + 2 : 2, new CleanerTooltips.DurabilityTooltip(stack));
                        case BOTTOM -> mutableComponents.addLast(new CleanerTooltips.DurabilityTooltip(stack));
                    }
                }
            }
        }
        this.renderTooltipInternal(font, mutableComponents, mouseX, mouseY, tooltipPositioner);
    }
}