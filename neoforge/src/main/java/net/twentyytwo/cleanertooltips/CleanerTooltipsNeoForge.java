package net.twentyytwo.cleanertooltips;

import com.mojang.blaze3d.platform.InputConstants;
import me.shedaniel.autoconfig.AutoConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.GatherSkippedAttributeTooltipsEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.twentyytwo.cleanertooltips.CleanerTooltips.*;

import net.twentyytwo.cleanertooltips.config.CleanerTooltipsConfig;

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

    @SubscribeEvent
    public static void hideDefaultAttributes(GatherSkippedAttributeTooltipsEvent event) {
        event.setSkipAll(!InputConstants.isKeyDown(CleanerTooltips.mc.getWindow().getWindow(), CleanerTooltips.hideTooltip.getKey().getValue()));
    }
}