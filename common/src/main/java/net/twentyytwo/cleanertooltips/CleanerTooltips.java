package net.twentyytwo.cleanertooltips;

import com.mojang.blaze3d.platform.InputConstants;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.twentyytwo.cleanertooltips.config.CleanerTooltipsConfig;
import net.twentyytwo.cleanertooltips.util.AttributeDisplayType;
import net.twentyytwo.cleanertooltips.util.CleanerTooltipsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class CleanerTooltips {

    public static final String MOD_ID = "cleanertooltips";
    public static final Minecraft MC = Minecraft.getInstance();
    public static final KeyMapping hideTooltip = new KeyMapping(
            "key.cleanertooltips.hide_tooltip",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_SHIFT,
            KeyMapping.CATEGORY_INVENTORY
    );

    public static KeyMapping hideTooltip;
    public static CleanerTooltipsConfig config;

    private static final int GAP = 3; // The gap between the icon and the value
    private static final int GROUP_GAP = 8; // The gap between attributes
    private static final ResourceLocation DURABILITY_ICON = ResourceLocation.fromNamespaceAndPath("cleanertooltips", "textures/gui/attribute/durability.png");
    private static final Registry<Attribute> attributeRegistry = BuiltInRegistries.ATTRIBUTE;

    public static void init() {
        AutoConfig.register(CleanerTooltipsConfig.class, JanksonConfigSerializer::new);
        config = AutoConfig.getConfigHolder(CleanerTooltipsConfig.class).getConfig();
    }

    @Nullable
    private static ResourceLocation getIcon(Holder<Attribute> attribute) {
        ResourceLocation attributeKey = attributeRegistry.getKey(attribute.value());

        if (attributeKey == null) return null;
        String texturePath = "textures/gui/attribute/" + attributeKey.getPath().replaceFirst("(generic|player)\\.", "") + ".png";
        ResourceLocation resourceLocation =  ResourceLocation.fromNamespaceAndPath("cleanertooltips", texturePath);
        if (MC.getResourceManager().getResource(resourceLocation).isEmpty())
            return null;
        return resourceLocation;
    }

    private static MutableComponent formatting(double value, double baseValue, AttributeDisplayType displayType) {

        switch (displayType) {
            case BOOLEAN -> {
                return Component.literal(value > (double) 0.0F ? "Enabled" : "Disabled").withStyle(ChatFormatting.WHITE);
            }
            case DIFFERENCE -> {
                return Component.literal((value > 0 ? "+" : "") + ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(value))
                                .withStyle(value < 0 ? ChatFormatting.RED : ChatFormatting.WHITE);
            }
            case MULTIPLIER -> {
                return Component.literal(ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format((value + baseValue) / baseValue) + "x")
                                .withStyle(ChatFormatting.WHITE);
            }
            case PERCENTAGE -> {
                return Component.literal((value > 0 ? "+" : "") + ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(value * 100)
                                .formatted(value < 0 ? ChatFormatting.RED : ChatFormatting.WHITE) + "%");
            }
            default -> {
                return Component.literal(ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(value + baseValue))
                                .withStyle((value + baseValue) < 0 ? ChatFormatting.RED : ChatFormatting.WHITE);
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

    /**
     * An object storing all the necessary information for calculating and rendering a custom tooltip.<br>
     * Useful to avoid running the same code multiple times.
     * @param text      a mutable component of the attribute modifier value
     * @param textWidth the width of the {@code text}
     * @param icon      the texture of the corresponding attribute. can be {@code null}
     */
    private record TooltipEntry(MutableComponent text, int textWidth, ResourceLocation icon) {}

    public record AttributeTooltip(ItemStack stack, ItemAttributeModifiers modifiers, List<TooltipEntry> cachedEntries) implements TooltipComponent, ClientTooltipComponent {

        /**
         * A custom tooltip object rendering the attribute modifiers of an itemstack as icons.
         * @param stack     the item stack
         * @param modifiers the {@code ItemAttributeModifiers} of the itemstack
         */
        public AttributeTooltip(ItemStack stack, ItemAttributeModifiers modifiers) {
            this(stack, modifiers, new ArrayList<>());

            // Handle sharpness
            int sharpnessBonus = 0;
            if (config.sharpness && MC.player != null) {
                ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
                for (var enchantment : enchantments.entrySet()) {
                    if (enchantment.getKey().unwrapKey().isPresent() && enchantment.getKey().unwrapKey().get() == Enchantments.SHARPNESS  && enchantment.getIntValue() > 0) {
                        sharpnessBonus += (int) ((0.5 * enchantment.getIntValue()) + 0.5);
                        break;
                    }
                }
            }

            List<ItemAttributeModifiers.Entry> entries = modifiers.modifiers();
            AttributeMap playerAttributes = MC.player.getAttributes();
            int size = entries.size();
            boolean[] isDuplicate = new boolean[size], renderAsPercentage = new boolean[size];

            for (int i = 0; i < size; i++) {
                if (isDuplicate[i]) continue;

                ItemAttributeModifiers.Entry entry = entries.get(i);
                AttributeDisplayType displayType = CleanerTooltipsUtil.ATTRIBUTE_DISPLAY_MAP
                        .getOrDefault(attributeRegistry.getKey(entry.attribute().value()), AttributeDisplayType.NUMBER);

                double value = entry.modifier().amount();
                double baseValue = (displayType == AttributeDisplayType.MULTIPLIER || displayType == AttributeDisplayType.NUMBER)
                        && playerAttributes.hasAttribute(entry.attribute())
                        ?  playerAttributes.getBaseValue(entry.attribute()) : 0;
                double totalValue = value + baseValue;

                if (entry.modifier().is(Item.BASE_ATTACK_DAMAGE_ID)) value += sharpnessBonus;

                // Checks if the current attribute exists multiple times, if it does, they get added together.
                for (int j = i + 1; j < size; j++) {
                    if (isDuplicate[j]) continue;
                    ItemAttributeModifiers.Entry comparedEntry = entries.get(j);
                    if (!entry.attribute().equals(comparedEntry.attribute())) continue;

                    double comparedValue = comparedEntry.modifier().amount();
                    switch (comparedEntry.modifier().operation()) {
                        case ADD_VALUE -> {
                            value += comparedValue;
                            isDuplicate[j] = true;
                        }
                        case ADD_MULTIPLIED_TOTAL -> {
                            value += totalValue * comparedValue;
                            isDuplicate[j] = true;
                        }
                        case ADD_MULTIPLIED_BASE -> renderAsPercentage[j] = true;
                    }
                }

                if (renderAsPercentage[i]) displayType = AttributeDisplayType.PERCENTAGE;
                if (totalValue != 0 && !((displayType == AttributeDisplayType.DIFFERENCE || displayType == AttributeDisplayType.PERCENTAGE) && value == 0)) {
                    MutableComponent text = formatting(value, baseValue, displayType);
                    cachedEntries.add(new TooltipEntry(text, MC.font.width(text), getIcon(entry.attribute())));
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

            // Only if posValues.INLINE is selected
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

        Component totalDurability = Component.literal(" / ").withStyle(ChatFormatting.DARK_GRAY).append(
                Component.literal(String.valueOf(maxDurability)).withStyle(ChatFormatting.DARK_GRAY));

        return Component.literal(String.valueOf(curDurability))
                .withStyle(!config.durabilityColor || curDurability == maxDurability
                        ? ChatFormatting.GRAY : diff >= 0.5f ? ChatFormatting.GREEN : diff >= 0.15f ? ChatFormatting.GOLD : ChatFormatting.RED)
                .append(CleanerTooltips.config.showMaximumDurability ? totalDurability : Component.empty());
    }

    private static void renderDurabilityTooltip(GuiGraphics guiGraphics, int x, int y, ItemStack stack) {
        guiGraphics.blit(DURABILITY_ICON, x, y, 0, 0, 9, 9, 9, 9);
        guiGraphics.drawString(MC.font, durabilityFormatting(stack), x + 9 + GAP, y + 1, -1);
    }

    public record DurabilityTooltip(ItemStack stack, MutableComponent text, int textWidth) implements TooltipComponent, ClientTooltipComponent{

        /**
         * A custom durability tooltip rendering the durability of an itemstack on the tooltip. <br>
         * Only used when the config option {@code INLINE} isn't selected, otherwise the durability tooltip is handled by the {@code AttributeTooltip} object.
         * @param stack the item stack
         */
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
            guiGraphics.drawString(MC.font, text, x + 9 + GAP, y, -1);
        }
    }
}