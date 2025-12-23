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
    public static void registerKeybind(RegisterKeyMappingsEvent event) { event.register(CleanerTooltips.hideTooltip); }

    @SubscribeEvent()
    public static void registerTooltips(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(AttributeTooltip.class, payload -> payload);
        event.register(DurabilityTooltip.class, payload -> payload);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void addTooltip(RenderTooltipEvent.GatherComponents event) {
        ItemStack stack = event.getItemStack();
        ItemAttributeModifiers modifiers = CleanerTooltipsUtil.getAttributeModifiers(stack);

        if (CleanerTooltipsUtil.shouldAddTooltip(modifiers)) {
            List<Either<FormattedText, TooltipComponent>> tooltipElements = event.getTooltipElements();

            int nameIndex = CleanerTooltipsUtil.getInsertIndex(stack, tooltipElements);
            tooltipElements.add(nameIndex, Either.right(new AttributeTooltip(stack, modifiers)));

            if (CleanerTooltips.config.durability && CleanerTooltips.config.durabilityPos != CleanerTooltipsConfig.posValues.INLINE && stack.getMaxDamage() > 0) {
                switch (CleanerTooltips.config.durabilityPos) {
                    case BELOW -> tooltipElements.add(nameIndex + 1, Either.right(new DurabilityTooltip(stack)));
                    case BOTTOM -> tooltipElements.addLast(Either.right(new DurabilityTooltip(stack)));
                }
            }
        }
    }

    @SubscribeEvent()
    public static void hideDefaultAttributes(GatherSkippedAttributeTooltipsEvent event) {
        event.setSkipAll(!InputConstants.isKeyDown(CleanerTooltips.MC.getWindow().getWindow(), CleanerTooltips.hideTooltip.getKey().getValue()) && CleanerTooltips.config.enabled);
    }
}