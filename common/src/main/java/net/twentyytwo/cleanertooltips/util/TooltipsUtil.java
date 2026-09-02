package net.twentyytwo.cleanertooltips.util;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.twentyytwo.cleanertooltips.CleanerTooltips;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Collection of useful functions.
 */
public class TooltipsUtil {
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

    public static ItemStack getEquippedStack(ItemStack stack) {
        var player = Minecraft.getInstance().player;
        return player != null ? player.getItemBySlot(player.getEquipmentSlotForItem(stack)) : ItemStack.EMPTY;
    }

    public static Optional<Holder.Reference<Attribute>> getAttributeFromString(String s) {
        return BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(s));
    }

    public static List<Component> getMainhandModifierComponents() {
        var player = Minecraft.getInstance().player;
        ItemStack stack = player != null ? player.getMainHandItem() : ItemStack.EMPTY;

        List<Component> modifierComponents = new ArrayList<>();
        Arrays.stream(EquipmentSlotGroup.values()).forEach(slot -> stack.forEachModifier(slot, (a, m) ->
                modifierComponents.add(ComponentUtils.copyOnClickText(m.id().toString()))));

        return modifierComponents;
    }

    /**
     * Calculates the additional attack damage from the sharpness enchantment.
     * @param stack the item stack
     * @return      the additional attack damage
     */
    public static float getSharpnessBonus(ItemStack stack) {
        float bonus = 0;
        if (CleanerTooltips.config.sharpnessFix) {
            var enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            for (var entry : enchantments.entrySet()) {
                Enchantment enchantment = entry.getKey().value();
                var effects = enchantment.getEffects(EnchantmentEffectComponents.DAMAGE);

                for (var effect : effects) {
                    if (effect.requirements().isEmpty()) {
                        bonus = effect.effect().process(entry.getIntValue(), RandomSource.create(), bonus);
                    }
                }
            }
        }
        return bonus;
    }

    public static MutableComponent getDiggingSpeedComponent(float speed) {
        return CommonComponents.space()
                .append(Component.translatable("text.cleanertooltips.mining_speed",
                        DecimalFormat.getInstance().format(speed)))
                .withStyle(ChatFormatting.DARK_GREEN);
    }

    public static float getDiggingSpeed(ItemStack stack) {
        Tool tool = stack.get(DataComponents.TOOL);
        if (tool == null || tool.rules().isEmpty()) {
            return 0.0f;
        }
        final float[] diggingSpeed = {0.0f};
        for (var rule : tool.rules()) {
            var key = rule.blocks().unwrapKey();
            if (key.isPresent() && key.get().location().getPath().equals("sword_efficient")) {
                return 0.0f;
            }

            rule.speed().ifPresent(f -> diggingSpeed[0] = f);
        }

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

    public static boolean hasDiggingSpeed(ItemStack stack) {
        if (!CleanerTooltips.config.miningSpeed) return false;

        Tool tool = stack.get(DataComponents.TOOL);
        if (tool == null || tool.rules().isEmpty()) return false;

        for (var rule : tool.rules()) {
            var key = rule.blocks().unwrapKey();
            if (key.isPresent() && key.get().location().getPath().equals("sword_efficient")) {
                return false;
            }

            if (rule.speed().isPresent()) return true;
        }

        return false;
    }

    /**
     * Compares two maps with each other for equality. Returns {@code true}
     * if both maps represent the same mappings and in the same order.
     * @param m1    a map
     * @param m2    a map to be compared with {@code m1} for equality
     * @return      {@code true} if both maps are equal to each other
     * @see         java.util.AbstractMap#equals(Object)
     * @see         java.util.AbstractMap.SimpleEntry#equals(Object)
     */
    public static boolean equalsOrdered(Map<?, ?> m1, Map<?, ?> m2) {
        if (m1 == m2) {
            return true;
        }

        if (m1 == null || m2 == null || m1.size() != m2.size()) {
            return false;
        }

        var iterator1 = m1.entrySet().iterator();
        var iterator2 = m2.entrySet().iterator();

        while (iterator1.hasNext() && iterator2.hasNext()) {
            Map.Entry<?, ?> entry1 = iterator1.next();
            Map.Entry<?, ?> entry2 = iterator2.next();

            if (!entry1.equals(entry2)) {
                return false;
            }
        }

        return !iterator1.hasNext() && !iterator2.hasNext();
    }

    public static boolean isDamageable(ItemStack stack) {
        return !CleanerTooltips.config.hideWhenRepaired ? stack.isDamageableItem() : stack.isDamaged();
    }

    public static boolean canAddDurabilityTooltip(ItemStack stack) {
        return CleanerTooltips.config.durabilityEnabled && isDamageable(stack);
    }
}
