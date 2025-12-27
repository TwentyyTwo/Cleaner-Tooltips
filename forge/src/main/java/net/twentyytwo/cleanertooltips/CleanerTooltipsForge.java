package net.twentyytwo.cleanertooltips;

import com.google.common.collect.Multimap;
import com.mojang.datafixers.util.Either;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.common.Mod;
import net.twentyytwo.cleanertooltips.CleanerTooltips.*;
import net.twentyytwo.cleanertooltips.config.CleanerTooltipsConfig;
import net.twentyytwo.cleanertooltips.util.CleanerTooltipsUtil;

import java.util.List;

@Mod(CleanerTooltips.MOD_ID)
public class CleanerTooltipsForge {
    public CleanerTooltipsForge(ModContainer container) {
        CleanerTooltips.init();
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

    @SubscribeEvent()
    public static void addTooltip(RenderTooltipEvent.GatherComponents event) {
        ItemStack stack = event.getItemStack();
        Multimap<Attribute, AttributeModifier> modifiers = CleanerTooltipsUtil.getAttributeModifiers(stack);

        if (CleanerTooltipsUtil.shouldAddTooltip(modifiers)) {
            List<Either<FormattedText, TooltipComponent>> tooltipElements = event.getTooltipElements();

            int nameIndex = CleanerTooltipsUtil.getInsertIndex(stack, tooltipElements);
            tooltipElements.add(nameIndex, Either.right(new AttributeTooltip(stack, modifiers)));

            if (CleanerTooltips.config.durability && CleanerTooltips.config.durabilityPos != CleanerTooltipsConfig.posValues.INLINE && stack.getMaxDamage() > 0) {
                switch (CleanerTooltips.config.durabilityPos) {
                    case BELOW -> tooltipElements.add(nameIndex + 1, Either.right(new DurabilityTooltip(stack)));
                    case BOTTOM -> tooltipElements.add(Either.right(new DurabilityTooltip(stack)));
                }
            }
        }
    }
}
