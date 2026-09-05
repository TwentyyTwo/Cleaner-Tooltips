package net.twentyytwo.cleanertooltips;

import com.mojang.datafixers.util.Either;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import net.neoforged.neoforge.event.GatherSkippedAttributeTooltipsEvent;
import net.twentyytwo.cleanertooltips.CleanerTooltips.IconAttributeComponent;
import net.twentyytwo.cleanertooltips.CleanerTooltips.IconAttributeTooltip;
import net.twentyytwo.cleanertooltips.CleanerTooltips.IconDurabilityComponent;
import net.twentyytwo.cleanertooltips.CleanerTooltips.IconDurabilityTooltip;
import net.twentyytwo.cleanertooltips.config.TooltipsConfig.Position;
import net.twentyytwo.cleanertooltips.config.TooltipsClothConfig;
import net.twentyytwo.cleanertooltips.util.AttributeHelper;
import net.twentyytwo.cleanertooltips.util.TooltipsUtil;
import net.twentyytwo.cleanertooltips.util.AttributeManager;

import java.util.List;
import java.util.function.Supplier;

import static net.twentyytwo.cleanertooltips.CleanerTooltips.config;

@Mod(value = CleanerTooltips.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = CleanerTooltips.MOD_ID, value = Dist.CLIENT)
public class CleanerTooltipsNeoForge {

    public CleanerTooltipsNeoForge(ModContainer container) {
        CleanerTooltips.init();
        container.registerExtensionPoint(IConfigScreenFactory.class, (Supplier<IConfigScreenFactory>)
                () -> (client, parent) -> TooltipsClothConfig.getConfigScreen(parent));
    }

    @SubscribeEvent()
    public static void registerKeybind(RegisterKeyMappingsEvent event) {
        event.register(CleanerTooltips.HIDE_TOOLTIP);
    }

    @SubscribeEvent()
    public static void onTick(ClientTickEvent.Pre event) {
        TooltipsUtil.onTick();
    }

    @SubscribeEvent()
    public static void onResourceReload(AddClientReloadListenersEvent event) {
        event.addListener(AttributeManager.LOCATION, new AttributeManager());
    }

    @SubscribeEvent()
    public static void registerTooltips(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(IconAttributeComponent.class, IconAttributeTooltip::fromComponent);
        event.register(IconDurabilityComponent.class, IconDurabilityTooltip::new);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void getMissingComponents(RenderTooltipEvent.GatherComponents event) {
        Position position = config.durabilityPos;
        if (!config.durabilityEnabled || position == Position.INLINE) return;

        var elements = event.getTooltipElements();
        for (int i = 0; i < elements.size(); i++) {
            var op = elements.get(i).right();
            if (op.isEmpty()) continue;
            if (op.get() instanceof IconAttributeComponent(ItemStack stack)) {
                if (TooltipsUtil.isDamageable(stack)) {
                    int index = position == Position.BELOW ? i + 1 : elements.size();
                    elements.add(index, Either.right(new IconDurabilityComponent(stack)));
                }
                return;
            } else if (op.get() instanceof IconDurabilityComponent) {
                if (i != elements.size() - 1 && position == Position.BOTTOM) {
                    elements.add(elements.remove(i));
                }
                return;
            }
        }
    }

    @SubscribeEvent()
    public static void hideDefaultAttributes(GatherSkippedAttributeTooltipsEvent event) {
        event.setSkipAll(AttributeHelper.isViableForIcons());
    }

    @SubscribeEvent()
    public static void registerCommand(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("get_modifier_ids").executes(ctx -> {
            List<Component> modifierComponents = TooltipsUtil.getMainhandModifierComponents();

            modifierComponents.forEach(c -> ctx.getSource().sendSuccess(() -> c, false));
            return modifierComponents.size();
        }));
    }
}