package net.twentyytwo.cleanertooltips.util;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.twentyytwo.cleanertooltips.services.Services;

import java.text.DecimalFormat;
import java.util.Optional;

import static net.twentyytwo.cleanertooltips.CleanerTooltips.MC;
import static net.twentyytwo.cleanertooltips.CleanerTooltips.config;

/**
 * Collection of useful functions.
 */
public class CleanerTooltipsUtil {
    public static final ResourceLocation EFFICIENCY =
            ResourceLocation.withDefaultNamespace("enchantment.efficiency/mainhand");

    private static int tick = 0;
    private static boolean tickToggle = false;

    public static void onTick() {
        tick++;
        if (tick >= 20) {
            tickToggle = !tickToggle;
            tick = 0;
        }
    }

    public static boolean getTickToggle() {
        return tickToggle;
    }

    public static Optional<Holder.Reference<Attribute>> resolveAttribute(String s) {
        // parse automatically adds the minecraft namespace if missing
        var location = ResourceLocation.parse(s);
        return BuiltInRegistries.ATTRIBUTE.get(location);
    }

    public static ItemStack getEquippedStack(ItemStack stack) {
        assert MC.player != null;
        return MC.player.getItemBySlot(MC.player.getEquipmentSlotForItem(stack));
    }

    /**
     * Calculates the additional attack damage from the sharpness enchantment.
     * @param stack the item stack
     * @return      the additional attack damage
     */
    public static float getSharpnessBonus(ItemStack stack) {
        assert MC.player != null;
        float bonus = 0;
        if (config.general.sharpness) {
            var enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            for (var entry : enchantments.entrySet()) {
                Enchantment enchantment = entry.getKey().value();
                var effects = enchantment.getEffects(EnchantmentEffectComponents.DAMAGE);

                for (var effect : effects) {
                    if (effect.requirements().isEmpty()) {
                        bonus = effect.effect().process(entry.getIntValue(), MC.player.getRandom(), bonus);
                    }
                }
            }
        }
        return bonus;
    }

    public static MutableComponent getDiggingSpeedComponent(ItemStack stack) {
        return CommonComponents.space()
                .append(Component.translatable("text.cleanertooltips.mining_speed",
                        DecimalFormat.getInstance().format(getDiggingSpeed(stack))))
                .withStyle(ChatFormatting.DARK_GREEN);
    }

    public static float getDiggingSpeed(ItemStack stack) {
        Tool tool = stack.get(DataComponents.TOOL);
        if (tool == null || tool.rules().isEmpty()) {
            return 0.0f;
        }
        final float[] diggingSpeed = {0.0f};
        for (var rule : tool.rules()) rule.speed().ifPresent(f -> diggingSpeed[0] = f);

        var enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (var entry : enchantments.entrySet()) {
            Enchantment enchantment = entry.getKey().value();
            var effects = enchantment.getEffects(EnchantmentEffectComponents.ATTRIBUTES);

            for (var effect : effects) {
                diggingSpeed[0] += effect.amount().calculate(entry.getIntValue());
            }
        }

        return diggingSpeed[0];
    }

    public static double getBaseValue(Holder<Attribute> attribute) {
        return MC.player != null ? MC.player.getAttributeBaseValue(attribute) : 0;
    }

    public static boolean isViableForAttributes() {
        return MC.player != null && config.general.enabled && !Services.getInstance().isKeyDown();
    }

    public static boolean hasAttributes(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        boolean[] found = new boolean[]{false};
        for (EquipmentSlotGroup slot : EquipmentSlotGroup.values()) {
            if (found[0]) break;
            stack.forEachModifier(slot, (attribute, modifier) -> {
                if (found[0]) return;
                if (AttributeManager.getTexture(attribute) == null) return;
                if (modifier.amount() != 0) {
                    found[0] = true;
                } else if (AttributeManager.getDisplayType(attribute).hasBaseValue()
                        && modifier.amount() + getBaseValue(attribute) != 0) {
                    found[0] = true;
                }
            });
        }
        return found[0];
    }

    public static boolean canAddAttributeTooltip(ItemStack stack) {
        return isViableForAttributes() && hasAttributes(stack);
    }

    public static boolean canAddDurabilityTooltip(ItemStack stack) {
        return config.durability.durabilityEnabled && stack.isDamageableItem();
    }

    public static boolean separateOperations(EquipmentSlotGroup slotGroup) {
        return slotGroup != EquipmentSlotGroup.MAINHAND
                && slotGroup != EquipmentSlotGroup.OFFHAND
                && slotGroup != EquipmentSlotGroup.BODY;
    }
}
