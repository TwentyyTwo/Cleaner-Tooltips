package net.twentyytwo.cleanertooltips;

import com.mojang.datafixers.util.Either;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import net.neoforged.neoforge.event.GatherSkippedAttributeTooltipsEvent;
import net.twentyytwo.cleanertooltips.CleanerTooltips.IconAttributeComponent;
import net.twentyytwo.cleanertooltips.CleanerTooltips.IconAttributeTooltip;
import net.twentyytwo.cleanertooltips.CleanerTooltips.IconDurabilityComponent;
import net.twentyytwo.cleanertooltips.CleanerTooltips.IconDurabilityTooltip;
import net.twentyytwo.cleanertooltips.config.CleanerTooltipsConfig;
import net.twentyytwo.cleanertooltips.config.CleanerTooltipsConfig.PosValues;
import net.twentyytwo.cleanertooltips.util.CleanerTooltipsUtil;
import net.twentyytwo.cleanertooltips.util.AttributeManager;

import java.util.function.Supplier;

import static net.twentyytwo.cleanertooltips.CleanerTooltips.MC;
import static net.twentyytwo.cleanertooltips.CleanerTooltips.config;

@Mod(value = CleanerTooltips.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = CleanerTooltips.MOD_ID, value = Dist.CLIENT)
public class CleanerTooltipsNeoForge {

    public CleanerTooltipsNeoForge(ModContainer container) {
        CleanerTooltips.init();
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (Supplier<IConfigScreenFactory>) () ->
                (modContainer, parent) ->
                        AutoConfig.getConfigScreen(CleanerTooltipsConfig.class, parent).get());
    }

    @SubscribeEvent()
    public static void onClientSetup(FMLClientSetupEvent event) {
        MC = Minecraft.getInstance();
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
    public static void onResourceReload(AddClientReloadListenersEvent event) {
        event.addListener(AttributeManager.LOCATION, new AttributeManager());
    }

    @SubscribeEvent()
    public static void registerTooltips(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(IconAttributeComponent.class, IconAttributeTooltip::new);
        event.register(IconDurabilityComponent.class, IconDurabilityTooltip::new);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void getMissingComponents(RenderTooltipEvent.GatherComponents event) {
        PosValues position = config.durability.durabilityPos;
        if (!config.durability.durabilityEnabled || position == PosValues.INLINE) return;

        var elements = event.getTooltipElements();
        for (int i = 0; i < elements.size(); i++) {
            var op = elements.get(i).right();
            if (op.isEmpty()) continue;
            if (op.get() instanceof IconAttributeComponent(ItemStack stack)) {
                if (stack.isDamageableItem()) {
                    int index = position == PosValues.BELOW ? i + 1 : elements.size();
                    elements.add(index, Either.right(new IconDurabilityComponent(stack)));
                }
                return;
            } else if (op.get() instanceof IconDurabilityComponent) {
                if (i != elements.size() - 1 && position == PosValues.BOTTOM) {
                    elements.add(elements.remove(i));
                }
                return;
            }
        }
    }

    @SubscribeEvent()
    public static void hideDefaultAttributes(GatherSkippedAttributeTooltipsEvent event) {
        event.setSkipAll(CleanerTooltipsUtil.canAddAttributeTooltip(event.getStack()));

        // Don't display mining efficiency if mining speed is displayed
        if (config.general.miningSpeed) event.skipId(CleanerTooltipsUtil.EFFICIENCY);
    }
}