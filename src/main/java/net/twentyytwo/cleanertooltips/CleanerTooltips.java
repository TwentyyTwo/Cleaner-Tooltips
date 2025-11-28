package net.twentyytwo.cleanertooltips;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
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
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.BooleanAttribute;
import net.neoforged.neoforge.common.PercentageAttribute;
import net.neoforged.neoforge.common.util.Lazy;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

import java.util.List;

@Mod(value = CleanerTooltips.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = CleanerTooltips.MOD_ID, value = Dist.CLIENT)
public class CleanerTooltips {

    public static final String MOD_ID = "cleanertooltips";
    @SuppressWarnings("unused")
    public static final Logger LOGGER = LogUtils.getLogger();

    public CleanerTooltips(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    private static final Minecraft mc = Minecraft.getInstance();

    private static final int GAP = 3; // The gap between the icon and the value
    private static final int GROUP_GAP = 8; // The gap between attributes

    public static final Lazy<KeyMapping> HIDE_TOOLTIP = Lazy.of(() -> new KeyMapping(
            "key.cleanertooltips.hide_tooltip",
            KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_SHIFT,
            "key.categories.inventory"
    ));

    @SubscribeEvent()
    public static void registerKeybind(RegisterKeyMappingsEvent event) { event.register(HIDE_TOOLTIP.get()); }

    @SubscribeEvent()
    public static void registerTooltips(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(AttributeTooltip.class, payload -> payload);
        event.register(DurabilityTooltip.class, payload -> payload);
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

        if (!InputConstants.isKeyDown(mc.getWindow().getWindow(), HIDE_TOOLTIP.get().getKey().getValue()) &&
                !stack.getAttributeModifiers().modifiers().isEmpty() &&
                mc.player != null &&
                Config.MOD_ENABLED.getAsBoolean()) {
            List<Either<FormattedText, TooltipComponent>> tooltip = event.getTooltipElements();
            Component itemName = stack.getHoverName();

            // Ensures the tooltip is always below the name, which ensures better compatibility between other mods
            int nameIndex = -1;
            for (int i = 0; i < tooltip.size(); i++) {
                var component = tooltip.get(i);
                if (component.left().isPresent() && component.left().get().getString().equals(itemName.getString())) {
                    nameIndex = i;
                    break;
                }
            }
            tooltip.add((nameIndex >= 0) ? nameIndex + 1 : 1, Either.right(new AttributeTooltip(stack)));

            if (Config.DURABILITY.getAsBoolean() && stack.getMaxDamage() > 0) {
                switch (Config.DURABILITY_POS.get()) {
                    case BELOW -> tooltip.add((nameIndex >= 0) ? nameIndex + 2 : 2, Either.right(new DurabilityTooltip(stack)));
                    case BOTTOM -> tooltip.addLast(Either.right(new DurabilityTooltip(stack)));
                }
            }
        }
    }

    @SubscribeEvent()
    public static void hideAttributes(GatherSkippedAttributeTooltipsEvent event) {
        event.setSkipAll(!InputConstants.isKeyDown(mc.getWindow().getWindow(), HIDE_TOOLTIP.get().getKey().getValue()) && Config.MOD_ENABLED.getAsBoolean());
    }

    // Calculates the attribute value and returns it as a Mutable Component, which is used for width calculation and rendering purposes
    private static MutableComponent formatting(ItemAttributeModifiers.Entry entry, double baseValue, ItemStack stack) {
        double value = entry.modifier().amount();

        if (Config.ADD_SHARPNESS.getAsBoolean() && mc.level != null) {
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

    // Renders the icon and value for the respective attribute, and returns the total width that is then used as the x position for the next attribute
    private static int renderTooltip(GuiGraphics guiGraphics, ItemAttributeModifiers.Entry entry, int x, int y, ItemStack stack) {
        ResourceLocation icon = getIcon(entry.attribute());
        guiGraphics.blit(icon, x, y, 0, 0, 9, 9, 9, 9);

        double baseValue = mc.player != null ? mc.player.getAttributeBaseValue(entry.attribute()) : 0;
        MutableComponent valueStr = formatting(entry, baseValue, stack);

        guiGraphics.drawString(mc.font, valueStr, x + 9 + GAP, y + 1, -1);
        x += mc.font.width(valueStr) + 9 + GAP + GROUP_GAP;
        return x;
    }

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
                double baseValue = mc.player != null ? mc.player.getAttributeBaseValue(entry.attribute()) : 0;
                if (entry.modifier().amount() + baseValue == 0) continue;
                width += mc.font.width(formatting(entry, baseValue, stack)) + 9 + GAP + GROUP_GAP;
            }
            // Only if POS_VALUES.INLINE is selected
            boolean displayDurability = Config.DURABILITY.getAsBoolean() && stack.getMaxDamage() > 0;
            if (displayDurability && Config.DURABILITY_POS.get() == Config.POS_VALUES.INLINE) width += mc.font.width(durabilityFormatting(stack)) + 9 + GAP + GROUP_GAP;
            return width - GROUP_GAP;
        }

        @Override
        public void renderImage(@NotNull Font font, int x, int y, @NotNull GuiGraphics guiGraphics) {
            PoseStack pose = guiGraphics.pose();
            pose.pushPose();

            ItemAttributeModifiers modifiers = stack.getAttributeModifiers();

            int groupX = x;
            for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
                double baseValue = mc.player != null ? mc.player.getAttributeBaseValue(entry.attribute()) : 0;
                if (entry.modifier().amount() + baseValue == 0) continue;
                groupX = renderTooltip(guiGraphics, entry, groupX, y - 1, stack);
            }
            // Only if POS_VALUES.INLINE is selected
            boolean displayDurability = Config.DURABILITY.getAsBoolean() && stack.getMaxDamage() > 0;
            if (displayDurability && Config.DURABILITY_POS.get() == Config.POS_VALUES.INLINE) renderDurabilityTooltip(guiGraphics, groupX, y - 1, stack);
            pose.popPose();
        }
    }

    private static MutableComponent durabilityFormatting(ItemStack stack) {
        int maxDurability = stack.getMaxDamage();
        int curDurability = maxDurability - stack.getDamageValue();

        return Component.empty()
                .append(Component.literal(String.valueOf(curDurability)).withStyle(ChatFormatting.WHITE))
                .append(Component.literal("/").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(String.valueOf(maxDurability)).withStyle(ChatFormatting.DARK_GRAY));
    }

    private static void renderDurabilityTooltip(GuiGraphics guiGraphics, int x, int y, ItemStack stack) {
        ResourceLocation durability = ResourceLocation.fromNamespaceAndPath("cleanertooltips", "textures/gui/attribute/durability.png");
        guiGraphics.blit(durability , x, y, 0, 0, 9, 9, 9, 9);

        guiGraphics.drawString(mc.font, durabilityFormatting(stack), x + 9 + GAP, y + 1, -1);

    }

    public record DurabilityTooltip(ItemStack stack) implements TooltipComponent, ClientTooltipComponent{

        @Override
        public int getHeight() {
            return 10;
        }

        @Override
        public int getWidth(@NotNull Font font) {
            int width = 0;
            width += mc.font.width(durabilityFormatting(stack)) + 9 + GAP;
            return width;
        }

        @Override
        public void renderImage(@NotNull Font font, int x, int y, @NotNull GuiGraphics guiGraphics) {
            PoseStack pose = guiGraphics.pose();
            pose.pushPose();

            renderDurabilityTooltip(guiGraphics, x, y - 1, stack);
            pose.popPose();
        }
    }
}