package net.twentyytwo.cleanertooltips;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
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
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.twentyytwo.cleanertooltips.config.CleanerTooltipsConfig;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class CleanerTooltips {

    public static final String MOD_ID = "cleanertooltips";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final Minecraft mc = Minecraft.getInstance();

    public static KeyMapping hideTooltip;

    private static final int GAP = 3; // The gap between the icon and the value
    private static final int GROUP_GAP = 8; // The gap between attributes

    public static CleanerTooltipsConfig config;

    public enum AttributeType {
        /**Displays the attribute as a percentage. For example: a knockback resistance of 0.1 becomes "10%".*/
        PERCENTAGE,
        /**Displays the attribute either as Enabled or Disabled.*/
        BOOLEAN,
        /**Displays the attribute as a flat number. For example: an armor value of 6 becomes "6"*/
        NUMBER
    }

    private static final Map<Holder<Attribute>, AttributeType> ATTRIBUTE_TYPE_MAP = new HashMap<>();
    static {
        ATTRIBUTE_TYPE_MAP.put(Attributes.KNOCKBACK_RESISTANCE, AttributeType.PERCENTAGE);
        ATTRIBUTE_TYPE_MAP.put(Attributes.MOVEMENT_SPEED, AttributeType.PERCENTAGE);
        ATTRIBUTE_TYPE_MAP.put(Attributes.SWEEPING_DAMAGE_RATIO, AttributeType.PERCENTAGE);

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

    // Returns the resource location for the given attribute
    private static ResourceLocation getIcon(Holder<Attribute> attribute) {
        ResourceLocation attributeKey = BuiltInRegistries.ATTRIBUTE.getKey(attribute.value());

        if (attributeKey == null) return ResourceLocation.fromNamespaceAndPath("cleanertooltips", "textures/gui/attribute/default.png");
        String texturePath = "textures/gui/attribute/" + attributeKey.getPath().replaceFirst("(generic|player)\\.", "") + ".png";
        ResourceLocation resourceLocation =  ResourceLocation.fromNamespaceAndPath("cleanertooltips", texturePath);
        if (mc.getResourceManager().getResource(resourceLocation).isEmpty())
            return ResourceLocation.fromNamespaceAndPath("cleanertooltips", "textures/gui/attribute/default.png");
        return resourceLocation;
    }

    // Calculates the attribute value and returns it as a Mutable Component, which is used for width calculation and rendering purposes
    private static MutableComponent formatting(ItemAttributeModifiers.Entry entry, double baseValue, ItemStack stack) {
        double value = entry.modifier().amount();

        if (config.sharpness && mc.level != null && entry.attribute() == Attributes.ATTACK_DAMAGE) {
            HolderLookup.Provider registries = mc.level.registryAccess();
            Holder.Reference<Enchantment> sharpnessHolder = registries.lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath("minecraft", "sharpness")));
            int sharpnessLevel = EnchantmentHelper.getItemEnchantmentLevel(sharpnessHolder, stack);
            if (sharpnessLevel > 0) value += (0.5 * sharpnessLevel) + 0.5;
        }

        switch (ATTRIBUTE_TYPE_MAP.getOrDefault(entry.attribute(), AttributeType.NUMBER)) {
            case PERCENTAGE -> {
                return Component.literal(ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(value * 100)
                        .formatted(ChatFormatting.WHITE) + "%");
            }
            case BOOLEAN -> {
                return Component.literal(ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(value != 0 ? "Enabled" : "Disabled")
                        .formatted(ChatFormatting.WHITE));
            }
            default -> {
                return Component.literal(ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(value + baseValue)
                        .formatted(ChatFormatting.WHITE));
            }
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

    /** @param stack The ItemStack whose values are used for the tooltip. */
    public record AttributeTooltip(ItemStack stack, ItemAttributeModifiers modifiers) implements TooltipComponent, ClientTooltipComponent {

        @Override
        public int getHeight() {
            return 10;
        }

        @Override
        public int getWidth(@NotNull Font font) {
            int width = 0;

            for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
                double baseValue = mc.player != null ? mc.player.getAttributeBaseValue(entry.attribute()) : 0;
                if (entry.modifier().amount() + baseValue == 0) continue;
                width += mc.font.width(formatting(entry, baseValue, stack)) + 9 + GAP + GROUP_GAP;
            }
            // Only if POS_VALUES.INLINE is selected
            boolean displayDurability = config.durability && stack.getMaxDamage() > 0;
            if (displayDurability && config.durabilityPos == CleanerTooltipsConfig.posValues.INLINE) width += mc.font.width(durabilityFormatting(stack)) + 9 + GAP + GROUP_GAP;
            return width - GROUP_GAP;
        }

        @Override
        public void renderImage(@NotNull Font font, int x, int y, @NotNull GuiGraphics guiGraphics) {
            PoseStack pose = guiGraphics.pose();
            pose.pushPose();

            int groupX = x;
            for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
                double baseValue = mc.player != null ? mc.player.getAttributeBaseValue(entry.attribute()) : 0;
                if (entry.modifier().amount() + baseValue == 0) continue;
                groupX = renderTooltip(guiGraphics, entry, groupX, y - 1, stack);
            }
            // Only if POS_VALUES.INLINE is selected
            boolean displayDurability = config.durability && stack.getMaxDamage() > 0;
            if (displayDurability && config.durabilityPos == CleanerTooltipsConfig.posValues.INLINE) renderDurabilityTooltip(guiGraphics, groupX, y - 1, stack);
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

    /** @param stack The ItemStack whose durability is used for the tooltip. */
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