package net.twentyytwo.cleanertooltips;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.ai.attributes.Attribute.Sentiment;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.twentyytwo.cleanertooltips.config.TooltipsConfig.Position;
import net.twentyytwo.cleanertooltips.config.TooltipsClothConfig;
import net.twentyytwo.cleanertooltips.config.TooltipsConfig;
import net.twentyytwo.cleanertooltips.util.AttributeDisplayType;
import net.twentyytwo.cleanertooltips.util.AttributeHelper;
import net.twentyytwo.cleanertooltips.util.AttributeManager;
import net.twentyytwo.cleanertooltips.util.TooltipsUtil;
import net.twentyytwo.cleanertooltips.util.ClientIconComponent;
import net.twentyytwo.cleanertooltips.util.Comparison;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.text.DecimalFormat;

public class CleanerTooltips {

    public static final String MOD_ID = "cleanertooltips";
    public static final KeyMapping HIDE_TOOLTIP = new KeyMapping("key.cleanertooltips.hide_tooltip",
                                                                 InputConstants.Type.KEYSYM,
                                                                 GLFW.GLFW_KEY_V,
                                                                 KeyMapping.CATEGORY_INVENTORY);

    public static TooltipsConfig config = TooltipsClothConfig.init();

    public static int GAP; // The gap between the icon and the value
    public static int GROUP_GAP; // The gap between attributes

    private static final ResourceLocation DURABILITY_ICON = location("textures/gui/attribute/durability.png");
    private static final ResourceLocation   DIGGING_SPEED = location("textures/gui/attribute/digging_speed.png");
    private static final ResourceLocation          HIGHER = location("textures/gui/attribute/higher.png");
    private static final ResourceLocation           LOWER = location("textures/gui/attribute/lower.png");

    public static void init() {
        TooltipsClothConfig.saveConfig();
    }

    public static ResourceLocation location(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static MutableComponent formatting(double value, double baseValue,
                                              AttributeDisplayType displayType, Sentiment sentiment) {
        return switch (displayType) {
            case BOOLEAN -> Component.literal(value > 0.0 ? "Enabled" : "Disabled")
                    .withStyle(ChatFormatting.WHITE);
            case DIFFERENCE -> Component.literal((value > 0 ? "+" : "") + format(value))
                    .withStyle(getColorWithSentiment(value, sentiment));
            case MULTIPLIER -> Component.literal(format((value + baseValue) / baseValue) + "x")
                    .withStyle(ChatFormatting.WHITE);
            case PERCENTAGE -> Component.literal((value > 0 ? "+" : "") + format(value * 100)
                    .formatted(getColorWithSentiment(value, sentiment)) + "%");
            case null, default -> Component.literal(format(value + baseValue))
                    .withStyle(getColorWithSentiment(value + baseValue, sentiment));
        };
    }

    private static ChatFormatting getColorWithSentiment(double value, Sentiment sentiment) {
        return sentiment == Sentiment.NEGATIVE ? (value > 0 ? ChatFormatting.RED : ChatFormatting.WHITE)
                                               : (value < 0 ? ChatFormatting.RED : ChatFormatting.WHITE);
    }

    private static String format(double value) {
        return ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(value);
    }

    private static MutableComponent durabilityFormatting(ItemStack stack) {
        int maxDurability = stack.getMaxDamage();
        int curDurability = maxDurability - stack.getDamageValue();
        int percentage = Math.round(((float) curDurability / maxDurability) * 100);

        int durabilityColor = getDurabilityColor(stack, percentage);

        switch (config.durabilityStyle) {
            case PERCENTAGE -> {
                return Component.literal(String.format("%d%%", percentage)).withColor(durabilityColor);
            }
            case null, default -> {
                var remain = Component.literal(String.valueOf(curDurability)).withColor(durabilityColor);
                if (!config.durabilityMaximum) return remain;

                return remain.append(Component.literal(String.format(" / %s", maxDurability))
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }

    private static int getDurabilityColor(ItemStack stack, int percentage) {
        if (!config.durabilityColor || !stack.isDamaged()) {
            return 0xAAAAAA;
        }

        switch (config.colorMode) {
            case LINEAR -> {
                if (TooltipsConfig.SORTED_STOPS.isEmpty()) return 0xAAAAAA;

                if (percentage <= TooltipsConfig.SORTED_STOPS.firstKey()) {
                    return TooltipsConfig.SORTED_STOPS.firstEntry().getValue();
                } else if (percentage >= TooltipsConfig.SORTED_STOPS.lastKey()) {
                    return TooltipsConfig.SORTED_STOPS.lastEntry().getValue();
                }

                var lower = TooltipsConfig.SORTED_STOPS.floorEntry(percentage);
                var upper = TooltipsConfig.SORTED_STOPS.ceilingEntry(percentage);

                float delta = (float) (percentage - lower.getKey()) / (upper.getKey() - lower.getKey());

                return ARGB.lerp(delta, lower.getValue(), upper.getValue());
            }
            case NATIVE -> {
                return stack.getBarColor();
            }
            case null, default -> {
                for (var e : TooltipsConfig.SORTED_STOPS.entrySet()) {
                    if (percentage <= e.getKey()) return e.getValue();
                }
                return 0xAAAAAA;
            }
        }
    }

    public record IconAttributeComponent(ItemStack stack) implements TooltipComponent {
    }

    public record IconAttributeTooltip(ItemStack stack,
                                       Multimap<String, AttributeFormattingData> formattingDataMap,
                                       MutableComponent durabilityComponent,
                                       boolean anyTextureMissing) implements ClientIconComponent {

        public static IconAttributeTooltip fromComponent(IconAttributeComponent component) {
            return fromStack(component.stack());
        }

        public static IconAttributeTooltip fromStack(ItemStack stack) {
            CombinedAttributeModifiers modifiers = CombinedAttributeModifiers.fromStack(stack);
            CombinedAttributeModifiers comparedModifiers = getComparedModifiers(stack);

            final boolean[] anyTextureMissing = {false};

            if (!config.onlyCompareMutual) {
                modifiers = CombinedAttributeModifiers.combine(modifiers, comparedModifiers, false);
            }

            ImmutableMultimap.Builder<String, AttributeFormattingData> builder = ImmutableListMultimap.builder();
            modifiers.modifiers().forEach((slot, entry) -> {
                Comparison comparison = Comparison.NONE;
                if (comparedModifiers.modifiers().containsKey(slot)) {
                    comparison = comparedModifiers.modifiers().get(slot).stream()
                            .filter(entry::isComparableWith)
                            .findFirst()
                            .map(entry::getComparison)
                            .orElse(entry.getComparison());
                }

                ResourceLocation texture = AttributeManager.getTexture(entry.attribute());
                if (texture != null) {
                    builder.put(slot, new AttributeFormattingData(entry, texture, comparison));
                } else {
                    anyTextureMissing[0] = true;
                }
            });

            AttributeFormattingData miningData = getMiningSpeedData(stack);
            if (miningData != null) builder.put("mainhand", miningData);

            return new IconAttributeTooltip(stack, builder.build(), durabilityFormatting(stack), anyTextureMissing[0]);
        }

        private static CombinedAttributeModifiers getComparedModifiers(ItemStack stack) {
            if (!config.comparisonEnabled) {
                return CombinedAttributeModifiers.EMPTY;
            }

            var comparedStack = TooltipsUtil.getEquippedStack(stack);
            if (comparedStack.isEmpty() || comparedStack.equals(stack)
                    || !AttributeHelper.hasAttributes(comparedStack)) {
                return CombinedAttributeModifiers.EMPTY;
            }

            return CombinedAttributeModifiers.fromStack(comparedStack);
        }

        @Nullable
        private static AttributeFormattingData getMiningSpeedData(ItemStack stack) {
            if (config.miningSpeed) {
                float speed = TooltipsUtil.getDiggingSpeed(stack);
                if (speed <= 0.0f) return null;

                var component = Component.literal(DecimalFormat.getInstance().format(speed));
                Comparison comparison = getMiningSpeedComparison(stack, speed);

                return new AttributeFormattingData(component, DIGGING_SPEED, comparison);
            }
            return null;
        }

        private static Comparison getMiningSpeedComparison(ItemStack stack, float speed) {
            if (config.comparisonEnabled) {
                var comparedStack = TooltipsUtil.getEquippedStack(stack);

                if (!comparedStack.isEmpty() && !comparedStack.equals(stack)
                        && stack.getItem().getClass().equals(comparedStack.getItem().getClass())) {
                    float comparedSpeed = TooltipsUtil.getDiggingSpeed(comparedStack);
                    if (comparedSpeed <= 0.0f) return Comparison.NONE;
                    return Comparison.getComparison(speed, comparedSpeed, true);
                }
            }

            return Comparison.NONE;
        }

        @Override
        public int getHeight(@NotNull Font font) {
            return config.groupDisplay == TooltipsConfig.GroupDisplay.ROWS
                    ? Math.max(10, formattingDataMap.asMap().size() * 10) : 10;
        }

        @Override
        public int getWidth(@NotNull Font font) {
            int width = 0;

            if (TooltipsUtil.canAddDurabilityTooltip(stack) && config.durabilityPos == Position.INLINE) {
                width += Minecraft.getInstance().font.width(durabilityComponent) + GROUP_GAP + GAP + 9;
            }

            width = calculateAttributeWidth(font, width);

            return width - GROUP_GAP;
        }

        private int calculateAttributeWidth(Font font, int width) {
            return switch (config.groupDisplay) {
                case ROWS -> getWidthRows(font, width);
                case INLINE -> getWidthInline(font, width);
            };
        }

        private int getWidthRows(Font font, int width) {
            int firstRowWidth = width;
            int biggestRowWidth = 0;

            boolean firstIteration = true;

            var dataMap = formattingDataMap.asMap();
            for (var collection : dataMap.values()) {
                int rowWidth = 0;
                for (var formattingData : collection) {
                    rowWidth += formattingData.textWidth() + 9 + GAP + GROUP_GAP;
                }

                if (firstIteration) {
                    firstIteration = false;
                    firstRowWidth += rowWidth;
                }
                biggestRowWidth = Math.max(rowWidth, biggestRowWidth);
            }

            if (this.anyTextureMissing && config.hintEnabled) {
                firstRowWidth += font.width("[+]") + GROUP_GAP;
            }

            int slotSize = dataMap.size() > 1 ? (9 + GROUP_GAP) : 0;
            return Math.max(firstRowWidth, biggestRowWidth) + slotSize;
        }

        private int getWidthInline(Font font, int width) {
            for (var formattingData : formattingDataMap.values()) {
                width += formattingData.textWidth() + 9 + GAP + GROUP_GAP;
            }

            if (this.anyTextureMissing && config.hintEnabled) {
                width += font.width("[+]") + GROUP_GAP;
            }

            int slotSize = formattingDataMap.asMap().size();
            return width + (slotSize > 1 ? slotSize * (9 + GROUP_GAP) : 0);
        }

        @Override
        public void renderImage(@NotNull Font font, int x, int y, int width, int height,
                                @NotNull GuiGraphics guiGraphics) {
            int groupX = renderAttributeModifiers(font, guiGraphics, x, y);

            if (TooltipsUtil.canAddDurabilityTooltip(stack)
                    && config.durabilityPos == Position.INLINE) {
                guiGraphics.blit(RenderType::guiTextured, DURABILITY_ICON, groupX, y - 1, 0, 0, 9, 9, 9, 9);
                guiGraphics.drawString(Minecraft.getInstance().font, durabilityComponent, groupX + 9 + GAP, y, -1);
            }
        }

        private int renderAttributeModifiers(Font font, GuiGraphics guiGraphics, int x, int y) {
            return switch (config.groupDisplay) {
                case ROWS -> renderRows(font, guiGraphics, x, y);
                case INLINE -> renderInline(font, guiGraphics, x, y);
            };
        }

        private int renderRows(Font font, GuiGraphics guiGraphics, int x, int y) {
            int groupX = x;
            int groupY = y - 1;
            int firstGroupX = x;

            boolean firstIteration = true;

            var dataMap = formattingDataMap.asMap();
            for (var entry : dataMap.entrySet()) {
                if (dataMap.size() > 1) {
                    var icon = getSlotIcon(entry.getKey());
                    groupX = renderSlotGroupIcon(guiGraphics, icon, groupX, groupY);
                }

                for (var formattingData : entry.getValue()) {
                    groupX = renderAttributeIconPair(guiGraphics, formattingData, groupX, groupY);
                }

                if (firstIteration) {
                    firstIteration = false;
                    firstGroupX = groupX;
                }
                groupX = x;
                groupY += 10;
            }

            return this.anyTextureMissing
                    ? renderHiddenHint(font, guiGraphics, firstGroupX, y)
                    : firstGroupX;
        }

        private int renderInline(Font font, GuiGraphics guiGraphics, int x, int y) {
            var dataMap = formattingDataMap.asMap();
            for (var entry : dataMap.entrySet()) {
                if (dataMap.size() > 1) {
                    var icon = getSlotIcon(entry.getKey());
                    x = renderSlotGroupIcon(guiGraphics, icon, x, y - 1);
                }

                for (var formattingData : entry.getValue()) {
                    x = renderAttributeIconPair(guiGraphics, formattingData, x, y - 1);
                }
            }

            return this.anyTextureMissing ? renderHiddenHint(font, guiGraphics, x, y) : x;
        }

        private int renderSlotGroupIcon(GuiGraphics guiGraphics,
                                        ResourceLocation icon,
                                        int x, int y) {
            guiGraphics.blit(RenderType::guiTextured, icon, x, y, 0, 0, 9, 9, 9, 9);
            return x + 9 + GROUP_GAP;
        }

        private int renderAttributeIconPair(GuiGraphics guiGraphics,
                                            AttributeFormattingData entry,
                                            int x, int y) {
            guiGraphics.blit(RenderType::guiTextured, entry.icon(), x, y, 0, 0, 9, 9, 9, 9);
            renderComparisonArrow(guiGraphics, entry.comparison(), x, y);
            entry.applyComparison();
            guiGraphics.drawString(Minecraft.getInstance().font, entry.text(), x + 9 + GAP, y + 1, -1);

            return x + entry.textWidth() + 9 + GAP + GROUP_GAP;
        }

        private int renderHiddenHint(Font font, GuiGraphics guiGraphics, int x, int y) {
            if (config.hintEnabled) {
                var component = Component.literal("[+]").withStyle(ChatFormatting.YELLOW);
                guiGraphics.drawString(font, component, x, y, -1);
                x += font.width("[+]") + GROUP_GAP;
            }
            return x;
        }

        private void renderComparisonArrow(GuiGraphics guiGraphics, Comparison comparison,
                                           int x, int y) {
            if (config.comparisonArrow && !comparison.equals(Comparison.NONE)) {
                ResourceLocation arrow = comparison.equals(Comparison.HIGHER) ? HIGHER : LOWER;
                int height = TooltipsUtil.getTickToggle() ? y : y - 1;
                guiGraphics.blit(RenderType::guiTextured, arrow, x + 7, height, 0, 0, 3, 3, 3, 3);
            }
        }

        private ResourceLocation getSlotIcon(String slotGroup) {
            String texturePath = "textures/gui/slot/" + slotGroup + ".png";
            ResourceLocation resourceLocation = location(texturePath);
            return Minecraft.getInstance().getResourceManager().getResource(resourceLocation).isEmpty()
                    ? location("textures/gui/slot/any.png")
                    : resourceLocation;
        }
    }

    public record IconDurabilityComponent(ItemStack stack) implements TooltipComponent {
    }

    /**
     * A custom durability tooltip rendering the durability of an itemstack on the tooltip. <p>
     *
     * Only used when the config option {@code INLINE} isn't selected, otherwise the durability
     * tooltip is handled by the {@link IconAttributeTooltip} object.
     */
    public record IconDurabilityTooltip(MutableComponent text) implements ClientIconComponent {

        public IconDurabilityTooltip(IconDurabilityComponent component) {
            this(component.stack());
        }

        public IconDurabilityTooltip(ItemStack stack) {
            this(durabilityFormatting(stack));
        }

        @Override
        public int getHeight(@NotNull Font font) {
            return 10;
        }

        @Override
        public int getWidth(@NotNull Font font) {
            return Minecraft.getInstance().font.width(text) + 9 + GAP;
        }

        @Override
        public void renderImage(@NotNull Font font, int x, int y, int width, int height,
                                @NotNull GuiGraphics guiGraphics) {
            guiGraphics.blit(RenderType::guiTextured, DURABILITY_ICON, x, y - 1, 0, 0, 9, 9, 9, 9);
            guiGraphics.drawString(Minecraft.getInstance().font, text, x + 9 + GAP, y, -1);
        }
    }

}