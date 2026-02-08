package net.twentyytwo.cleanertooltips;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.datafixers.util.Either;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.GatherSkippedAttributeTooltipsEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.twentyytwo.cleanertooltips.CleanerTooltips.*;

import net.twentyytwo.cleanertooltips.config.CleanerTooltipsConfig;
import net.twentyytwo.cleanertooltips.util.CleanerTooltipsUtil;

import java.util.List;
import java.util.function.Supplier;

@Mod(value = CleanerTooltips.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = CleanerTooltips.MOD_ID, value = Dist.CLIENT)
public class CleanerTooltipsNeoForge {
    public CleanerTooltipsNeoForge(ModContainer container) {
        CleanerTooltips.init();
        container.registerExtensionPoint(IConfigScreenFactory.class, (Supplier<IConfigScreenFactory>) () ->
                (modContainer, parent) -> AutoConfig.getConfigScreen(CleanerTooltipsConfig.class, parent).get());
    }

    @SubscribeEvent()
    public static void registerKeybind(RegisterKeyMappingsEvent event) {
        event.register(CleanerTooltips.hideTooltip);
    }

    @SubscribeEvent()
    public static void registerTooltips(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(AttributeTooltip.class, payload -> payload);
        event.register(DurabilityTooltip.class, payload -> payload);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void addTooltip(RenderTooltipEvent.GatherComponents event) {
        ItemStack stack = event.getItemStack();
        ItemAttributeModifiers modifiers = CleanerTooltipsUtil.getAttributeModifiers(stack);

        List<Either<FormattedText, TooltipComponent>> tooltipElements = event.getTooltipElements();
        int insertIndex = CleanerTooltipsUtil.getInsertIndex(stack, tooltipElements);

        boolean shouldAdd = CleanerTooltipsUtil.shouldAddTooltip(modifiers)
                && !InputConstants.isKeyDown(CleanerTooltips.MC.getWindow().getWindow(), CleanerTooltips.hideTooltip.getKey().getValue());
        if (shouldAdd) tooltipElements.add(insertIndex, Either.right(new AttributeTooltip(stack, modifiers)));

        if (CleanerTooltips.config.durability && stack.getMaxDamage() > 0) {
            switch (CleanerTooltips.config.durabilityPos) {
                case INLINE -> {
                    if (shouldAdd) return;
                    tooltipElements.add(insertIndex, Either.right(new DurabilityTooltip(stack)));
                }
                case BELOW -> tooltipElements.add(shouldAdd ? insertIndex + 1 : insertIndex, Either.right(new DurabilityTooltip(stack)));
                case BOTTOM -> tooltipElements.addLast(Either.right(new DurabilityTooltip(stack)));
            }
        }
    }

    @SubscribeEvent()
    public static void hideDefaultAttributes(GatherSkippedAttributeTooltipsEvent event) {
        event.setSkipAll(CleanerTooltipsUtil.shouldAddTooltip(CleanerTooltipsUtil.getAttributeModifiers(event.getStack())));
    }
}