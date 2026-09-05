package net.twentyytwo.cleanertooltips.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.twentyytwo.cleanertooltips.config.TooltipsConfig;
import net.twentyytwo.cleanertooltips.config.TooltipsConfig.RegexLocation;
import net.twentyytwo.cleanertooltips.services.Services;

import java.util.function.Predicate;

import static net.twentyytwo.cleanertooltips.CleanerTooltips.config;

public class AttributeHelper {
    public static final Predicate<Holder<Attribute>> ATTRIBUTE_FILTER = TooltipsConfig.BLACKLISTED_ATTRIBUTES::contains;

    public static final Predicate<AttributeModifier> MODIFIER_FILTER = m -> {
        for (RegexLocation loc : TooltipsConfig.BLACKLISTED_MODIFIERS) if (loc.matches(m.id())) return true;
        return false;
    };

    public static double getBaseValue(Holder<Attribute> attribute) {
        Player player = Minecraft.getInstance().player;
        return player != null && player.getAttributes().hasAttribute(attribute)
                ? player.getAttributeBaseValue(attribute) : 0;
    }

    /**
     * Returns whether the given {@code ItemStack} has at least {@code 1} valid attribute modifier to display.
     */
    public static boolean hasAttributes(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        boolean[] hasViableAttribute = {false};
        for (var slot : EquipmentSlotGroup.values()) {
            if (hasViableAttribute[0]) break; // short-circuit to prevent unnecessary calculations
            stack.forEachModifier(slot, (attribute, modifier, display) -> {
                if (hasViableAttribute[0] || AttributeManager.getTexture(attribute) == null
                        || ATTRIBUTE_FILTER.test(attribute) || MODIFIER_FILTER.test(modifier)) {
                    return;
                }

                var type = AttributeManager.getDisplayType(attribute);
                if (modifier.amount() + (type.hasBaseValue() ? getBaseValue(attribute) : 0) != 0) {
                    hasViableAttribute[0] = true;
                }
            });
        }

        return hasViableAttribute[0];
    }

    public static boolean isViableForIcons() {
        return Minecraft.getInstance().player != null && config.iconsEnabled && !Services.PLATFORM.isKeyDown();
    }

    public static boolean canAddIconAttributes(ItemStack stack) {
        return isViableForIcons() && (hasAttributes(stack) || TooltipsUtil.hasDiggingSpeed(stack));
    }
}
