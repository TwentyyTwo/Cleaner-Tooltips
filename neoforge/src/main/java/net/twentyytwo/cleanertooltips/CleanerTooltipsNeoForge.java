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

import net.twentyytwo.cleanertooltips.CleanerTooltips.IconAttributeComponent;
import net.twentyytwo.cleanertooltips.CleanerTooltips.IconAttributeTooltip;
import net.twentyytwo.cleanertooltips.CleanerTooltips.IconDurabilityComponent;
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
        event.register(IconAttributeComponent.class, IconAttributeTooltip::new);
        event.register(IconDurabilityComponent.class, IconDurabilityTooltip::new);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void addTooltip(RenderTooltipEvent.GatherComponents event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) stack = stackBackup; // Backup if the gui container doesn't provide the stack

        if (stack != null && !stack.isEmpty()) {
            List<Either<FormattedText, TooltipComponent>> tooltipElements = event.getTooltipElements();

            boolean shouldAddAttributes = CleanerTooltipsUtil.canAddAttributeTooltip(stack);
            boolean shouldAddDurability = CleanerTooltipsUtil.canAddDurabilityTooltip(stack);

            if (!(shouldAddAttributes || shouldAddDurability)) return;

            int insertIndex = getIndex(tooltipElements);
            if (shouldAddAttributes) {
                tooltipElements.add(insertIndex, Either.right(new IconAttributeComponent(stack)));
                insertIndex++;
            }

            if (shouldAddDurability) {
                switch (config.durability.durabilityPos) {
                    case INLINE -> {
                        if (!shouldAddAttributes) {
                            tooltipElements.add(insertIndex, Either.right(new IconDurabilityComponent(stack)));
                        }
                    }
                    case BELOW -> tooltipElements.add(insertIndex, Either.right(new IconDurabilityComponent(stack)));
                    case BOTTOM -> tooltipElements.addLast(Either.right(new IconDurabilityComponent(stack)));
                }
            }
        }
    }

    private static int getIndex(List<Either<FormattedText, TooltipComponent>> elements) {
        int index = 1;
        for (int i = 0; i < elements.size(); i++) {
            // Check for the first FormattedText element, and get the index after
            if (elements.get(i).left().isPresent()) {
                index = i + 1;
                break;
            }
        }

        // If that element's a TooltipComponent, increment by one
        while (index < elements.size() && elements.get(index).right().isPresent()) {
            index++;
        }

        return index;
    }

    @SubscribeEvent()
    public static void hideDefaultAttributes(GatherSkippedAttributeTooltipsEvent event) {
        stackBackup = event.getStack();
        event.setSkipAll(CleanerTooltipsUtil.canAddAttributeTooltip(stackBackup));

        // Don't display mining efficiency if mining speed is displayed
        if (config.general.miningSpeed) {
            event.skipId(CleanerTooltipsUtil.EFFICIENCY);
        }
    }
}