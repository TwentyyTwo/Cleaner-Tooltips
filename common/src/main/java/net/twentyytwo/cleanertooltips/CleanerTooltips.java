package net.twentyytwo.cleanertooltips;

import com.google.common.collect.Multimap;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.twentyytwo.cleanertooltips.config.CleanerTooltipsConfig;
import net.twentyytwo.cleanertooltips.util.AttributeDisplayType;
import net.twentyytwo.cleanertooltips.util.CleanerTooltipsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CleanerTooltips {

    public static final String MOD_ID = "cleanertooltips";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final Minecraft MC = Minecraft.getInstance();

    public static KeyMapping hideTooltip;
    public static CleanerTooltipsConfig config;

    private static final int GAP = 3; // The gap between the icon and the value
    private static final int GROUP_GAP = 8; // The gap between attributes
    private static final ResourceLocation DURABILITY_ICON = new ResourceLocation("cleanertooltips", "textures/gui/attribute/durability.png");

    static {
        hideTooltip = new KeyMapping(
                "key.cleanertooltips.hide_tooltip",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_SHIFT,
                "key.categories.inventory"
        );
    }

    public static void init() {
        AutoConfig.register(CleanerTooltipsConfig.class, JanksonConfigSerializer::new);
        config = AutoConfig.getConfigHolder(CleanerTooltipsConfig.class).getConfig();
    }

    @Nullable
    private static ResourceLocation getIcon(Attribute attribute) {
        ResourceLocation attributeKey = BuiltInRegistries.ATTRIBUTE.getKey(attribute);

        if (attributeKey == null) return null;
        String texturePath = "textures/gui/attribute/" + attributeKey.getPath().replaceFirst("(generic|player)\\.", "") + ".png";
        ResourceLocation resourceLocation = new ResourceLocation("cleanertooltips", texturePath);
        if (MC.getResourceManager().getResource(resourceLocation).isEmpty())
            return null;
        return resourceLocation;
    }

    // Calculates the attribute value and returns it as a Mutable Component, which is used for width calculation and rendering purposes
    private static MutableComponent formatting(Attribute attribute, double value, double baseValue, ItemStack stack, AttributeDisplayType displayType) {
        if (config.sharpness && MC.level != null && attribute.equals(Attributes.ATTACK_DAMAGE))
            value += EnchantmentHelper.getDamageBonus(stack, MobType.UNDEFINED);

        switch (displayType) {
            case BOOLEAN -> {
                return Component.literal(value > (double) 0.0F ? "Enabled" : "Disabled").withStyle(ChatFormatting.WHITE);
            }
            case DIFFERENCE -> {
                return Component.literal((value > 0 ? "+" : "") + ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(value))
                        .withStyle(ChatFormatting.WHITE);
            }
            case MULTIPLIER -> {
                return Component.literal(ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format((value + baseValue) / baseValue) + "x")
                        .withStyle(ChatFormatting.WHITE);
            }
            case PERCENTAGE -> {
                return Component.literal((value > 0 ? "+" : "") + ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(value * 100) + "%")
                        .withStyle(ChatFormatting.WHITE);
            }
            default -> {
                return Component.literal(ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(value + baseValue)
                        .formatted(ChatFormatting.WHITE));
            }
        }
    }

    // Renders the icon and value for the respective attribute, and returns the total width that is then used as the x position for the next attribute
    private static int renderTooltip(GuiGraphics guiGraphics, TooltipEntry entry, int x, int y) {
        guiGraphics.blit(entry.icon(), x, y, 0, 0, 9, 9, 9, 9);
        guiGraphics.drawString(MC.font, entry.text(), x + 9 + GAP, y + 1, -1);

        x += entry.textWidth() + 9 + GAP + GROUP_GAP;
        return x;
    }

    private record TooltipEntry(MutableComponent text, int textWidth, ResourceLocation icon) {}

    /**
     * @param stack The {@code ItemStack} the {@code AttributeTooltip} should be added on to.
     * @param modifiers The {@code ItemAttributeModifiers} of the stack; should be obtained via {@link CleanerTooltipsUtil#getAttributeModifiers(ItemStack) getAttributeModifiers}.
     */
    public record AttributeTooltip(ItemStack stack, Multimap<Attribute, AttributeModifier> modifiers, List<TooltipEntry> cachedEntries) implements TooltipComponent, ClientTooltipComponent {

        public AttributeTooltip(ItemStack stack, Multimap<Attribute, AttributeModifier> modifiers) {
            this(stack, modifiers, new ArrayList<>());

            for (Map.Entry<Attribute, AttributeModifier> entry : modifiers.entries()) {
                double baseValue = MC.player != null && MC.player.getAttributes().hasAttribute(entry.getKey()) ? MC.player.getAttributeBaseValue(entry.getKey()) : 0;
                AttributeDisplayType displayType = CleanerTooltipsUtil.ATTRIBUTE_DISPLAY_MAP.getOrDefault(BuiltInRegistries.ATTRIBUTE.getKey(entry.getKey()), AttributeDisplayType.NUMBER);
                if (entry.getValue().getAmount() + baseValue != 0) {
                    if (displayType == AttributeDisplayType.DIFFERENCE && entry.getValue().getAmount() == 0) continue;
                    MutableComponent text = formatting(entry.getKey(), entry.getValue().getAmount(), baseValue, stack, displayType);
                    cachedEntries.add(new TooltipEntry(text, MC.font.width(text), getIcon(entry.getKey())));
                }
            }
        }

        @Override
        public int getHeight() {
            return 10;
        }

        @Override
        public int getWidth(@NotNull Font font) {
            int width = 0;
            boolean anyIconNull = false;
            for (TooltipEntry entry : cachedEntries) {
                if (entry.icon() == null) {
                    anyIconNull = true;
                    continue;
                }
                width += entry.textWidth() + 9 + GAP + GROUP_GAP;
            }

            if (anyIconNull && config.hiddenAttributesHint) width += font.width("[+]") + GROUP_GAP;

            // Only if POS_VALUES.INLINE is selected
            boolean displayDurability = config.durability && stack.getMaxDamage() > 0;
            if (displayDurability && config.durabilityPos == CleanerTooltipsConfig.posValues.INLINE) width += MC.font.width(durabilityFormatting(stack)) + 9 + GAP + GROUP_GAP;
            return width - GROUP_GAP;
        }

        @Override
        public void renderImage(@NotNull Font font, int x, int y, @NotNull GuiGraphics guiGraphics) {
            int groupX = x;
            boolean anyIconNull = false;
            for (TooltipEntry entry : cachedEntries) {
                if (entry.icon() == null) {
                    anyIconNull = true;
                    continue;
                }
                groupX = renderTooltip(guiGraphics, entry, groupX, y - 1);
            }

            if (anyIconNull && config.hiddenAttributesHint) {
                guiGraphics.drawString(font, Component.literal("[+]").withStyle(ChatFormatting.YELLOW), groupX, y, -1);
                groupX += font.width("[+]") + GROUP_GAP;
            }

            // Only if posValues.INLINE is selected
            boolean displayDurability = config.durability && stack.getMaxDamage() > 0;
            if (displayDurability && config.durabilityPos == CleanerTooltipsConfig.posValues.INLINE) renderDurabilityTooltip(guiGraphics, groupX, y - 1, stack);
        }
    }

    private static MutableComponent durabilityFormatting(ItemStack stack) {
        int maxDurability = stack.getMaxDamage();
        int curDurability = maxDurability - stack.getDamageValue();
        float diff = (float) curDurability / maxDurability;

        return Component.literal(String.valueOf(curDurability))
                .withStyle(!config.durabilityColor || curDurability == maxDurability
                        ? ChatFormatting.WHITE : diff >= 0.5f ? ChatFormatting.GREEN : diff >= 0.15f ? ChatFormatting.GOLD : ChatFormatting.RED)
                .append(Component.literal(" / ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(String.valueOf(maxDurability)).withStyle(ChatFormatting.DARK_GRAY));
    }

    private static void renderDurabilityTooltip(GuiGraphics guiGraphics, int x, int y, ItemStack stack) {
        guiGraphics.blit(DURABILITY_ICON , x, y, 0, 0, 9, 9, 9, 9);
        guiGraphics.drawString(MC.font, durabilityFormatting(stack), x + 9 + GAP, y + 1, -1);
    }

    /** @param stack The ItemStack whose durability is used for the tooltip. */
    public record DurabilityTooltip(ItemStack stack, MutableComponent text, int textWidth) implements TooltipComponent, ClientTooltipComponent{

        public DurabilityTooltip(ItemStack stack) {
            this(
                    stack,
                    durabilityFormatting(stack),
                    0
            );
        }

        public DurabilityTooltip {
            textWidth = MC.font.width(text) + 9 + GAP;
        }

        @Override
        public int getHeight() {
            return 10;
        }

        @Override
        public int getWidth(@NotNull Font font) {
            return textWidth;
        }

        @Override
        public void renderImage(@NotNull Font font, int x, int y, @NotNull GuiGraphics guiGraphics) {
            guiGraphics.blit(DURABILITY_ICON , x, y - 1, 0, 0, 9, 9, 9, 9);
            guiGraphics.drawString(MC.font, text, x + 9 + GAP, y + 1, -1);
        }
    }
}