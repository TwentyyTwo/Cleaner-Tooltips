package net.twentyytwo.cleanertooltips;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.twentyytwo.cleanertooltips.compat.BetterCombatCompat;
import net.twentyytwo.cleanertooltips.util.AttributeDisplayType;
import net.twentyytwo.cleanertooltips.util.AttributeManager;
import net.twentyytwo.cleanertooltips.util.CleanerTooltipsUtil;
import net.twentyytwo.cleanertooltips.util.Comparison;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static net.twentyytwo.cleanertooltips.CleanerTooltips.MC;

/**
 * This object contains a map {@code modifiers}, that maps each {@code EquipmentSlotGroup} to a list of entries.
 * Each {@code Entry} contains the same data as an {@link ItemAttributeModifiers.Entry}, with the addition
 * of the attributes {@link AttributeDisplayType}, and the base player value of the attribute.<p>
 *
 * Multiple attributes will be combined into a single {@code Entry}, if they have the same {@code Holder<Attribute>} and
 * {@link EquipmentSlotGroup}. However, any attributes whose operation is {@code ADD_MULTIPLIED_BASE}, and slotGroup is
 * any type of armor, will remain a separate {@code Entry}, to reflect that these attributes affect the entire armor.<p>
 *
 * The base player value will only be calculated if it is required by the {@code AttributeDisplayType}, otherwise
 * it defaults to {@code  0}. Lastly, if both the value and the base player value equate to {@code 0}, the {@code Entry}
 * will be omitted.
 */
public record CombinedAttributeModifiers(LinkedHashMap<EquipmentSlotGroup, List<Entry>> modifiers) {

    public CombinedAttributeModifiers(ItemStack stack, ItemAttributeModifiers attributeModifiers) {
        this(getEntries(stack, attributeModifiers));
    }

    private static LinkedHashMap<EquipmentSlotGroup, List<Entry>> getEntries(ItemStack stack, ItemAttributeModifiers modifiers) {
        LinkedHashMap<EquipmentSlotGroup, List<Entry>> entryList = new LinkedHashMap<>();

        List<ItemAttributeModifiers.Entry> entries = modifiers.modifiers();
        AttributeMap playerAttributes = Objects.requireNonNull(MC.player).getAttributes();
        EquipmentSlotGroup primaryGroup = getPrimaryGroup(stack);

        int size = entries.size();
        boolean[] hasBeenCombined = new boolean[size];

        double sharpnessBonus = CleanerTooltipsUtil.getSharpnessBonus(stack);

        for (int i = 0; i < size; i++) {
            if (!(hasBeenCombined[i] || (BetterCombatCompat.isModLoaded && Objects.equals(entries.get(i).attribute(), Attributes.ENTITY_INTERACTION_RANGE)))) {
                ItemAttributeModifiers.Entry entry = entries.get(i);

                boolean isOnlyWhenUsing = Set.of(1, 2, 9).contains(entries.get(i).slot().ordinal());
                AttributeDisplayType displayType = AttributeManager.getDisplayType(entry, isOnlyWhenUsing);

                double baseValue = displayType.hasBaseValue() && playerAttributes.hasAttribute(entry.attribute())
                        ? playerAttributes.getBaseValue(entry.attribute()) : 0;
                double value = getTotalValue(entries, baseValue, i, isOnlyWhenUsing, hasBeenCombined)
                        + (entry.modifier().is(Item.BASE_ATTACK_DAMAGE_ID) ? sharpnessBonus : 0);

                if (value + baseValue != 0) {
                    AttributeModifier modifier = new AttributeModifier(entry.modifier().id(), value, entry.modifier().operation());
                    Entry newEntry = new Entry(entry.attribute(), modifier, entry.slot(), displayType, baseValue);

                    entryList.computeIfAbsent(entry.slot(), k -> new ArrayList<>()).add(newEntry);
                }
            }
        }

        // Ensures that the most relevant EquipmentSlotGroup is always first
        if (!entryList.entrySet().iterator().next().getKey().equals(primaryGroup)) {
            entryList.putFirst(primaryGroup, entryList.get(primaryGroup));
        }

        return entryList;
    }

    private static double getTotalValue(List<ItemAttributeModifiers.Entry> entries, double baseValue, int index, boolean isOnlyWhenUsing, boolean[] hasBeenCombined) {
        ItemAttributeModifiers.Entry baseEntry = entries.get(index);

        double totalAddValue = baseValue;
        double totalBaseMultiplier = 1;
        double totalMultiplier = 1;

        for (int j = index; j < entries.size(); j++) {
            ItemAttributeModifiers.Entry entry = entries.get(j);
            if (isCombinable(baseEntry, entry, isOnlyWhenUsing)) {
                double amount = entry.modifier().amount();
                hasBeenCombined[j] = true;

                // if the modifier of the baseEntry applies to items other than itself, meaning other slotGroups
                // than "mainhand", "offhand" and "body", keep the modifier operations separate.
                // Example: modifiers with the slotGroup "chest" effect your total armor value, while "mainhand"
                // only ever effects the item the modifier is on.
                if (!isOnlyWhenUsing) {
                    totalAddValue += amount;
                    continue;
                }

                switch (entry.modifier().operation()) {
                    case ADD_VALUE -> totalAddValue += amount;
                    case ADD_MULTIPLIED_BASE -> totalBaseMultiplier += amount;
                    case ADD_MULTIPLIED_TOTAL -> totalMultiplier *= (1 + amount);
                }
            }
        }

        // deduct the baseValue here in case this value should be displayed as a difference.
        return ((totalAddValue * totalBaseMultiplier) * totalMultiplier) - baseValue;
    }

    private static boolean isCombinable(ItemAttributeModifiers.Entry baseEntry, ItemAttributeModifiers.Entry compareEntry, boolean isOnlyWhenUsing) {
        boolean baseCheck = baseEntry.attribute().equals(compareEntry.attribute()) && baseEntry.slot().equals(compareEntry.slot());
        return isOnlyWhenUsing ? baseCheck : baseCheck && baseEntry.modifier().operation().equals(compareEntry.modifier().operation());
    }

    private static EquipmentSlotGroup getPrimaryGroup(ItemStack stack) {
        return stack.getItem() instanceof ArmorItem armorItem
                ? EquipmentSlotGroup.bySlot(armorItem.getEquipmentSlot())
                : EquipmentSlotGroup.MAINHAND;
    }

    /**
     * For each {@link EquipmentSlotGroup} in {@code other}, any entry with an attribute not present in
     * this object will get added to this objects {@link #modifiers()}, with a value of {@code 0}.
     *
     * @param other the object whose {@code modifiers} are merged into this object
     */
    public void merge(CombinedAttributeModifiers other) {
        Map<EquipmentSlotGroup, Set<Holder<Attribute>>> existingAttributes = this.modifiers().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().stream()
                        .map(Entry::attribute)
                        .collect(Collectors.toSet())));

        other.modifiers().forEach((key, value) -> value.stream()
                .filter(entry -> !existingAttributes.getOrDefault(key, Collections.emptySet()).contains(entry.attribute()))
                .forEach(entry -> this.modifiers().computeIfAbsent(key, k -> new ArrayList<>()).add(entry.getPlaceholder())));
    }

    public record Entry(Holder<Attribute> attribute, AttributeModifier modifier, EquipmentSlotGroup slot, AttributeDisplayType displayType, double baseValue) {

        public Entry getPlaceholder() {
            return new Entry(this.attribute(), new AttributeModifier(this.modifier().id(), 0, this.modifier().operation()), this.slot(), this.displayType(), this.baseValue());
        }

        public boolean isComparable(Entry comparedEntry, boolean isOnlyWhenUsing) {
            boolean baseCheck = this.attribute().equals(comparedEntry.attribute()) && this.slot().equals(comparedEntry.slot());
            return isOnlyWhenUsing ? baseCheck : baseCheck && this.modifier().operation().equals(comparedEntry.modifier().operation());
        }

        public Comparison getComparison(Entry comparedEntry) {
            return getComparison(comparedEntry.modifier().amount(), comparedEntry.baseValue());
        }

        public Comparison getComparison(double otherValue, double otherBaseValue) {
            double value = this.modifier().amount();
            double comparedValue = otherValue;

            if (this.displayType().hasBaseValue()) {
                value += this.baseValue();
                comparedValue += otherBaseValue;
            }

            return Comparison.getComparison(value, comparedValue);
        }
    }
}
