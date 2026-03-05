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
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.twentyytwo.cleanertooltips.compat.BetterCombatCompat;
import net.twentyytwo.cleanertooltips.config.CleanerTooltipsConfig;
import net.twentyytwo.cleanertooltips.services.Services;
import net.twentyytwo.cleanertooltips.util.AttributeDisplayType;
import net.twentyytwo.cleanertooltips.util.CleanerTooltipsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class CleanerTooltips {

    public static final String MOD_ID = "cleanertooltips";
    public static final Minecraft MC = Minecraft.getInstance();
    public static final KeyMapping hideTooltip = new KeyMapping(
            "key.cleanertooltips.hide_tooltip",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_SHIFT,
            KeyMapping.CATEGORY_INVENTORY
    );

    public static final int legendaryTooltipsIncreasedHeight = 1;
    public static final boolean isLegendaryTooltipsLoaded = Services.getInstance().isModLoaded("legendarytooltips");

    public static CleanerTooltipsConfig config;

    private static final int GAP = 3; // The gap between the icon and the value
    private static final int GROUP_GAP = 8; // The gap between attributes

    private static final ResourceLocation DURABILITY_ICON = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/attribute/durability.png");
    private static final ResourceLocation DIGGING_SPEED = ResourceLocation.fromNamespaceAndPath(CleanerTooltips.MOD_ID, "textures/gui/attribute/digging_speed.png");

    public static void init() {
        AutoConfig.register(CleanerTooltipsConfig.class, JanksonConfigSerializer::new);
        config = AutoConfig.getConfigHolder(CleanerTooltipsConfig.class).getConfig();
    }

    public static MutableComponent formatting(double value, double baseValue, AttributeDisplayType displayType) {
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

    private static MutableComponent durabilityFormatting(ItemStack stack) {
        int maxDurability = stack.getMaxDamage();
        int curDurability = maxDurability - stack.getDamageValue();
        float diff = (float) curDurability / maxDurability;

        Component totalDurability = Component.literal(" / ")
                .withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal(String.valueOf(maxDurability)).withStyle(ChatFormatting.DARK_GRAY));

        return Component.literal(String.valueOf(curDurability)).withStyle(!config.durabilityColor || curDurability == maxDurability
                        ? ChatFormatting.GRAY : diff >= 0.5f ? ChatFormatting.GREEN : diff >= 0.15f ? ChatFormatting.GOLD : ChatFormatting.RED)
                .append(config.showMaximumDurability ? totalDurability : Component.empty());
    }

    public record IconAttributeModifierTooltip(
            ItemStack stack,
            ItemAttributeModifiers modifiers,
            List<TooltipEntry> cachedEntries,
            MutableComponent durabilityComponent,
            float miningSpeed) implements TooltipComponent, ClientTooltipComponent {

        /**
         * A custom tooltip object rendering the attribute modifiers of an itemstack as icons.
         * @param stack     the item stack
         * @param modifiers the {@code ItemAttributeModifiers} of the itemstack
         */
        public IconAttributeModifierTooltip(ItemStack stack, ItemAttributeModifiers modifiers) {
            this(stack, modifiers, populateEntries(stack, modifiers), durabilityFormatting(stack), 0);

            if (BetterCombatCompat.shouldAdd(stack)) {
                cachedEntries.add(BetterCombatCompat.attackRangeEntry(stack, modifiers));
            }
        }

        public IconAttributeModifierTooltip {
            if (config.miningSpeed && stack.getItem() instanceof DiggerItem item) {
                miningSpeed = getMiningSpeed(item, stack);
            }
        }

        private static float getMiningSpeed(DiggerItem item, ItemStack stack) {
            float speed = item.getTier().getSpeed();

            ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            for (var enchantment : enchantments.entrySet()) {
                var enchantmentKey = enchantment.getKey().unwrapKey();
                if (enchantmentKey.isPresent() && enchantmentKey.get() == Enchantments.EFFICIENCY  && enchantment.getIntValue() > 0) {
                    speed += (float) (enchantment.getIntValue() * enchantment.getIntValue()) + 1;
                    break;
                }
            }
            return speed;
        }

        private static List<TooltipEntry> populateEntries(ItemStack stack, ItemAttributeModifiers modifiers) {
            List<TooltipEntry> tooltipEntries = new ArrayList<>();

            List<ItemAttributeModifiers.Entry> entries = modifiers.modifiers();
            AttributeMap playerAttributes = Objects.requireNonNull(MC.player).getAttributes();

            int size = entries.size();
            boolean[] isDuplicate = new boolean[size], renderAsPercentage = new boolean[size];

            double sharpnessBonus = CleanerTooltipsUtil.getSharpnessBonus(stack);

            for (int i = 0; i < size; i++) {
                if (isDuplicate[i] || (BetterCombatCompat.shouldAdd(stack) && Objects.equals(entries.get(i).attribute(), Attributes.ENTITY_INTERACTION_RANGE))) continue;

                ItemAttributeModifiers.Entry entry = entries.get(i);
                AttributeDisplayType displayType = AttributeDisplayType.get(entry.attribute());

                double value = entry.modifier().amount() + (entry.modifier().is(Item.BASE_ATTACK_DAMAGE_ID) ? sharpnessBonus : 0);
                double baseValue = displayType.hasBaseValue() && playerAttributes.hasAttribute(entry.attribute())
                        ? playerAttributes.getBaseValue(entry.attribute())
                        : 0;

                value = calculateDuplicates(entries, isDuplicate, renderAsPercentage, i, value, baseValue);

                if (renderAsPercentage[i]) {
                    displayType = AttributeDisplayType.PERCENTAGE;
                }

                if (value + baseValue != 0 && !(!displayType.hasBaseValue() && value == 0)) {
                    MutableComponent text = formatting(value, baseValue, displayType);
                    tooltipEntries.add(new TooltipEntry(text, MC.font.width(text), getIcon(entry.attribute())));
                }
            }

            return tooltipEntries;
        }

        private static double calculateDuplicates(List<ItemAttributeModifiers.Entry> entries, boolean[] isDuplicate, boolean[] renderAsPercentage, int i, double value, double baseValue) {
            ItemAttributeModifiers.Entry entry = entries.get(i);

            int size = entries.size();

            double totalAddValue = value + baseValue;
            double totalMultiBase = 1;
            double totalMultiplier = 1;

            for (int j = i + 1; j < size; j++) {
                if (isDuplicate[j]) {
                    continue;
                }

                ItemAttributeModifiers.Entry comparedEntry = entries.get(j);
                if (!entry.attribute().equals(comparedEntry.attribute())) {
                    continue;
                }

                double comparedValue = comparedEntry.modifier().amount();
                switch (comparedEntry.modifier().operation()) {
                    case ADD_VALUE -> {
                        totalAddValue += comparedValue;
                        isDuplicate[j] = true;
                    }
                    case ADD_MULTIPLIED_TOTAL -> {
                        totalMultiplier *= (1 + comparedValue);
                        isDuplicate[j] = true;
                    }
                    case ADD_MULTIPLIED_BASE -> {
                        if (Set.of(4, 5, 6, 7).contains(comparedEntry.slot().ordinal())) {
                            renderAsPercentage[j] = true;
                        } else {
                            totalMultiBase *= (1 + comparedValue);
                            isDuplicate[j] = true;
                        }
                    }
                }
            }

            return ((totalAddValue * totalMultiBase) * totalMultiplier) - baseValue;
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

        @Nullable
        private static ResourceLocation getIcon(Holder<Attribute> attribute) {
            ResourceLocation attributeKey = BuiltInRegistries.ATTRIBUTE.getKey(attribute.value());

            if (attributeKey == null) return null;
            String texturePath = "textures/gui/attribute/" + attributeKey.getPath().replaceFirst("(generic|player)\\.", "") + ".png";
            ResourceLocation resourceLocation =  ResourceLocation.fromNamespaceAndPath(CleanerTooltips.MOD_ID, texturePath);
            if (MC.getResourceManager().getResource(resourceLocation).isEmpty())
                return null;
            return resourceLocation;
        }

        @Override
        public int getHeight() {
            return 10;
        }

        @Override
        public int getWidth(@NotNull Font font) {
            int width = calculateAttributeWidth(cachedEntries, font);

            width += (miningSpeed > 0)
                    ? font.width(DecimalFormat.getInstance().format(miningSpeed)) + GROUP_GAP + GAP + 9
                    : 0;

            width += (config.durability && stack.getMaxDamage() > 0
                    && config.durabilityPos == CleanerTooltipsConfig.posValues.INLINE)
                    ? MC.font.width(durabilityComponent) + 9 + GAP + GROUP_GAP
                    : 0;

            return width - GROUP_GAP;
        }

        private static int calculateAttributeWidth(List<TooltipEntry> entries, Font font) {
            int width = 0;

            boolean anyIconMissing = false;
            for (TooltipEntry entry : entries) {
                if (entry.icon() == null) {
                    anyIconMissing = true;
                    continue;
                }
                width += entry.textWidth() + 9 + GAP + GROUP_GAP;
            }

            width += (anyIconMissing && config.hiddenAttributesHint)
                    ? font.width("[+]") + GROUP_GAP
                    : 0;

            return width;
        }

        @Override
        public void renderImage(@NotNull Font font, int x, int y, @NotNull GuiGraphics guiGraphics) {
            int groupX = renderAttributeModifiers(cachedEntries, font, guiGraphics, x, y);

            if (miningSpeed > 0) {
                groupX = renderMiningTooltip(guiGraphics, groupX, y - 1, miningSpeed);
            }

            if (config.durability && stack.getMaxDamage() > 0 && config.durabilityPos == CleanerTooltipsConfig.posValues.INLINE) {
                guiGraphics.blit(DURABILITY_ICON, groupX, y - 1, 0, 0, 9, 9, 9, 9);
                guiGraphics.drawString(MC.font, durabilityComponent, groupX + 9 + GAP, y, -1);
            }
        }

        private static int renderAttributeModifiers(List<TooltipEntry> entries, Font font, GuiGraphics guiGraphics, int x, int y) {
            int groupX = x;

            boolean anyIconMissing = false;
            for (TooltipEntry entry : entries) {
                if (entry.icon() == null) {
                    anyIconMissing = true;
                    continue;
                }
                groupX = renderAttributeIconPair(guiGraphics, entry, groupX, y - 1);
            }

            if (anyIconMissing && config.hiddenAttributesHint) {
                guiGraphics.drawString(font, Component.literal("[+]").withStyle(ChatFormatting.YELLOW), groupX, y, -1);
                groupX += font.width("[+]") + GROUP_GAP;
            }

            return groupX;
        }

        // Renders the icon and value for the respective attribute, and returns the total width that is then used as the x position for the next attribute
        private static int renderAttributeIconPair(GuiGraphics guiGraphics, TooltipEntry entry, int x, int y) {
            guiGraphics.blit(entry.icon(), x, y, 0, 0, 9, 9, 9, 9);
            guiGraphics.drawString(MC.font, entry.text(), x + 9 + GAP, y + 1, -1);

            x += entry.textWidth() + 9 + GAP + GROUP_GAP;
            return x;
        }

        private static int renderMiningTooltip(GuiGraphics guiGraphics, int x, int y, float miningSpeed) {
            String miningSpeedStr = DecimalFormat.getInstance().format(miningSpeed);
            guiGraphics.blit(DIGGING_SPEED, x, y, 0, 0, 9, 9, 9, 9);
            guiGraphics.drawString(MC.font, miningSpeedStr, x + 9 + GAP, y + 1, -1);
            return x + MC.font.width(miningSpeedStr) + GROUP_GAP + GAP + 9;
        }
    }

    public record IconDurabilityTooltip(ItemStack stack, MutableComponent text) implements TooltipComponent, ClientTooltipComponent {

        /**
         * A custom durability tooltip rendering the durability of an itemstack on the tooltip. <br>
         * Only used when the config option {@code INLINE} isn't selected, otherwise the durability tooltip is handled by the {@code AttributeTooltip} object.
         * @param stack the item stack
         */
        public IconDurabilityTooltip(ItemStack stack) {
            this(stack, durabilityFormatting(stack));
        }

        @Override
        public int getHeight() {
            return 10;
        }

        @Override
        public int getWidth(@NotNull Font font) {
            return MC.font.width(text) + 9 + GAP;
        }

        @Override
        public void renderImage(@NotNull Font font, int x, int y, @NotNull GuiGraphics guiGraphics) {
            guiGraphics.blit(DURABILITY_ICON , x, y - 1, 0, 0, 9, 9, 9, 9);
            guiGraphics.drawString(MC.font, text, x + 9 + GAP, y, -1);
        }
    }

}