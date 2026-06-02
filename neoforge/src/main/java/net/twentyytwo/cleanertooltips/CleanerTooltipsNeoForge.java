package net.twentyytwo.cleanertooltips;

import com.mojang.datafixers.util.Either;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.GatherSkippedAttributeTooltipsEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import net.twentyytwo.cleanertooltips.CleanerTooltips.IconAttributeModifierTooltip;
import net.twentyytwo.cleanertooltips.CleanerTooltips.IconDurabilityTooltip;
import net.twentyytwo.cleanertooltips.config.CleanerTooltipsConfig;
import net.twentyytwo.cleanertooltips.util.CleanerTooltipsUtil;
import net.twentyytwo.cleanertooltips.util.AttributeManager;

import java.util.List;
import java.util.function.Supplier;

import static net.twentyytwo.cleanertooltips.CleanerTooltips.config;

@Mod(value = CleanerTooltips.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = CleanerTooltips.MOD_ID, value = Dist.CLIENT)
public class CleanerTooltipsNeoForge {
    private static ItemStack stackBackup = ItemStack.EMPTY;

    public CleanerTooltipsNeoForge(ModContainer container) {
        CleanerTooltips.init();
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (Supplier<IConfigScreenFactory>) () ->
                (modContainer, parent) ->
                        AutoConfig.getConfigScreen(CleanerTooltipsConfig.class, parent).get());
    }

    @SubscribeEvent()
    public static void registerKeybind(RegisterKeyMappingsEvent event) {
        event.register(CleanerTooltips.hideTooltip);
    }

    @SubscribeEvent()
    public static void onTick(ClientTickEvent.Pre event) {
        CleanerTooltipsUtil.onTick();
    }

    @SubscribeEvent()
    public static void onResourceReload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new AttributeManager());
    }

    @SubscribeEvent()
    public static void registerTooltips(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(IconAttributeModifierTooltip.class, payload -> payload);
        event.register(IconDurabilityTooltip.class, payload -> payload);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void addTooltip(RenderTooltipEvent.GatherComponents event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) stack = stackBackup; // Backup if the gui container doesn't provide the stack

        if (stack != null && !stack.isEmpty()) {
            List<Either<FormattedText, TooltipComponent>> tooltipElements = event.getTooltipElements();

            boolean shouldAddAttributes = CleanerTooltipsUtil.shouldAddAttributes()
                    && CleanerTooltipsUtil.hasAttributes(stack);
            boolean shouldAddDurability = config.durability.durabilityEnabled
                    && stack.isDamageableItem();

            int insertIndex = CleanerTooltipsUtil.getIndexNeoforge(tooltipElements);
            if (shouldAddAttributes) {
                tooltipElements.add(insertIndex, Either.right(IconAttributeModifierTooltip.get(stack)));
                insertIndex++;
            }

            if (shouldAddDurability) {
                switch (config.durability.durabilityPos) {
                    case INLINE -> {
                        if (!shouldAddAttributes) {
                            tooltipElements.add(insertIndex, Either.right(new IconDurabilityTooltip(stack)));
                        }
                    }
                    case BELOW -> tooltipElements.add(insertIndex, Either.right(new IconDurabilityTooltip(stack)));
                    case BOTTOM -> tooltipElements.addLast(Either.right(new IconDurabilityTooltip(stack)));
                }
            }
        }
    }

    @SubscribeEvent()
    public static void hideDefaultAttributes(GatherSkippedAttributeTooltipsEvent event) {
        stackBackup = event.getStack();
        event.setSkipAll(CleanerTooltipsUtil.shouldAddAttributes()
                && CleanerTooltipsUtil.hasAttributes(stackBackup));

        // Don't display mining efficiency if mining speed is displayed
        if (config.general.miningSpeed) {
            event.skipId(CleanerTooltipsUtil.EFFICIENCY);
        }
    }
}