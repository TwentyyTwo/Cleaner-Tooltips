package net.twentyytwo.cleanertooltips;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.GatherSkippedAttributeTooltipsEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.BooleanAttribute;
import net.neoforged.neoforge.common.PercentageAttribute;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.List;

@Mod(value = CleanerTooltips.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = CleanerTooltips.MOD_ID, value = Dist.CLIENT)
public class CleanerTooltips {

    public static final String MOD_ID = "cleanertooltips";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CleanerTooltips(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    private static final Minecraft mc = Minecraft.getInstance();

    private static final int GAP = 3; // The gap between the icon and the value
    private static final int GROUP_GAP = 8; // The gap between attributes

    @SubscribeEvent()
    public static void registerAttributeTooltip(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(AttributeTooltip.class, payload -> payload);
    }

    private static ResourceLocation getIcon(Holder<Attribute> attribute) {
        ResourceLocation attributeKey = BuiltInRegistries.ATTRIBUTE.getKey(attribute.value());

        if (attributeKey == null) return ResourceLocation.fromNamespaceAndPath("cleanertooltips", "textures/gui/attribute/default.png");
        String texturePath = "textures/gui/attribute/" + attributeKey.getPath().replaceFirst("^generic\\.", "") + ".png";
        ResourceLocation resourceLocation =  ResourceLocation.fromNamespaceAndPath("cleanertooltips", texturePath);
        if (mc.getResourceManager().getResource(resourceLocation).isEmpty())
            return ResourceLocation.fromNamespaceAndPath("cleanertooltips", "textures/gui/attribute/default.png");
        return resourceLocation;
    }

    // EventPriority is set to highest to guarantee the AttributeTooltip is always added below the name
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void addTooltip(RenderTooltipEvent.GatherComponents event) {
        ItemStack stack = event.getItemStack();

        if (!Screen.hasShiftDown() && Config.MOD_ENABLED.getAsBoolean() && !stack.getAttributeModifiers().modifiers().isEmpty() && mc.player != null) {
            List<Either<FormattedText, TooltipComponent>> tooltip = event.getTooltipElements();
            Component itemName = stack.getHoverName();

            int nameIndex = -1;
            for (int i = 0; i < tooltip.size(); i++) {
                var component = tooltip.get(i);
                if (component.left().isPresent() && component.left().get().getString().equals(itemName.getString())) {
                    nameIndex = i;
                    break;
                }
            }
            tooltip.add((nameIndex >= 0) ? nameIndex + 1 : 1, Either.right(new AttributeTooltip(stack)));
        }
    }

    @SubscribeEvent()
    public static void hideAttributes(GatherSkippedAttributeTooltipsEvent event) {
        event.setSkipAll(!Screen.hasShiftDown() && Config.MOD_ENABLED.getAsBoolean());
    }

    private static MutableComponent formatting(ItemAttributeModifiers.Entry entry, double baseValue, ItemStack stack) {
        double value = entry.modifier().amount();

        if (Config.ADD_SHARPNESS.getAsBoolean()) {
            HolderLookup.Provider registries = mc.level.registryAccess();
            Holder.Reference<Enchantment> sharpnessHolder = registries.lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath("minecraft", "sharpness")));
            int sharpnessLevel = EnchantmentHelper.getTagEnchantmentLevel(sharpnessHolder, stack);
            if (sharpnessLevel > 0 && entry.modifier().id().toShortLanguageKey().replaceFirst("^base_", "").equals("attack_damage"))
                value += (0.5 * sharpnessLevel) + 0.5;
        }

        Class<?> attributeClass = entry.attribute().getDelegate().value().getClass();
        if (PercentageAttribute.class.isAssignableFrom(attributeClass)) {
            return Component.literal(ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(value * 100)
                    .formatted(ChatFormatting.WHITE) + "%");
        } else if (BooleanAttribute.class.isAssignableFrom(attributeClass)) {
            return Component.literal(ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(value != 0 ? "Enabled" : "Disabled")
                    .formatted(ChatFormatting.WHITE));
        } else {
            return Component.literal(ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(value + baseValue)
                    .formatted(ChatFormatting.WHITE));
        }
    }

    private static int renderTooltip(GuiGraphics guiGraphics, ItemAttributeModifiers.Entry entry, int x, int y, ItemStack stack) {
        ResourceLocation icon = getIcon(entry.attribute());
        guiGraphics.blit(icon, x, y, 0, 0, 9, 9, 9, 9);

        double baseValue = mc.player.getAttributeBaseValue(entry.attribute());
        MutableComponent valueStr = formatting(entry, baseValue, stack);

        guiGraphics.drawString(mc.font, valueStr, x + 9 + GAP, y + 1, -1);
        x += mc.font.width(valueStr) + 9 + GAP + GROUP_GAP;
        return x;
    }

    // The attributes to be added to the tooltip
    public record AttributeTooltip(ItemStack stack) implements TooltipComponent, ClientTooltipComponent {

        @Override
        public int getHeight() {
            return 10;
        }

        @Override
        public int getWidth(@NotNull Font font) {
            int width = 0;
            ItemAttributeModifiers modifiers = stack.getAttributeModifiers();
            for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
                double baseValue = mc.player.getAttributeBaseValue(entry.attribute());
                if (entry.modifier().amount() + baseValue == 0) continue;
                width += mc.font.width(formatting(entry, baseValue, stack)) + 9 + GAP + GROUP_GAP;
            }
            return width - GROUP_GAP;
        }

        @Override
        public void renderImage(@NotNull Font font, int x, int y, @NotNull GuiGraphics guiGraphics) {
            PoseStack pose = guiGraphics.pose();
            pose.pushPose();

            ItemAttributeModifiers modifiers = stack.getAttributeModifiers();

            int groupX = x;
            for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
                double baseValue = mc.player.getAttributeBaseValue(entry.attribute());
                if (entry.modifier().amount() + baseValue == 0) continue;
                groupX = renderTooltip(guiGraphics, entry, groupX, y - 1, stack);
            }
            pose.popPose();
        }
    }
}