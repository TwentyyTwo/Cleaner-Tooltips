package net.twentyytwo.cleanertooltips;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.twentyytwo.cleanertooltips.compat.BetterCombatCompat;
import net.twentyytwo.cleanertooltips.util.AttributeDisplayType;
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
            if (hasBeenCombined[i] || (BetterCombatCompat.isModLoaded && Objects.equals(entries.get(i).attribute(), Attributes.ENTITY_INTERACTION_RANGE))) {
                continue;
            }

            ItemAttributeModifiers.Entry entry = entries.get(i);
            AttributeDisplayType displayType = AttributeDisplayType.get(entry);

            double value = entry.modifier().amount() + (entry.modifier().is(Item.BASE_ATTACK_DAMAGE_ID) ? sharpnessBonus : 0);
            double baseValue = displayType.hasBaseValue() && playerAttributes.hasAttribute(entry.attribute())
                    ? playerAttributes.getBaseValue(entry.attribute())
                    : 0;

            // Combines the values of attributes of the same type and EquipmentSlotGroup
            value = combineValues(entries, value, baseValue, i, hasBeenCombined);

            if (value + baseValue != 0) {
                Entry newEntry = new Entry(entry.attribute(),
                        new AttributeModifier(entry.modifier().id(), value, entry.modifier().operation()),
                        entry.slot(), displayType, baseValue);

                entryList.computeIfAbsent(entry.slot(), k -> new ArrayList<>()).add(newEntry);
            }
        }

        // Ensures that the most relevant EquipmentSlotGroup is always first
        if (!entryList.entrySet().iterator().next().getKey().equals(primaryGroup)) {
            entryList.putFirst(primaryGroup, entryList.get(primaryGroup));
        }

        return entryList;
    }

    private static double combineValues(List<ItemAttributeModifiers.Entry> entries, double value, double baseValue, int i, boolean[] hasBeenCombined) {
        ItemAttributeModifiers.Entry entry = entries.get(i);
        Operation entryOperation = entry.modifier().operation();

        int size = entries.size();

        double totalAddValue = value + baseValue;
        double totalMultiBase = 1;
        double totalMultiplier = 1;

        for (int j = i + 1; j < size; j++) {
            ItemAttributeModifiers.Entry entryToMerge = entries.get(j);
            if (!entry.attribute().equals(entryToMerge.attribute()) || !entry.slot().equals(entryToMerge.slot())) {
                continue;
            }

            double valueToMerge = entryToMerge.modifier().amount();

            // If both the entry and comparedEntry are equal to ADD_MULTIPLIED_BASE, simply add the values together.
            Operation entryToMergeOperation = entryToMerge.modifier().operation();
            if (entryOperation == Operation.ADD_MULTIPLIED_BASE
                    && entryOperation.equals(entryToMergeOperation)) {
                totalAddValue += valueToMerge;
                hasBeenCombined[j] = true;
                continue;
            }
            // If the entry is equal to ADD_MULTIPLIED_BASE, but the comparedEntry isn't, then skip.
            else if (entryOperation == Operation.ADD_MULTIPLIED_BASE) {
                continue;
            }

            switch (entryToMergeOperation) {
                case ADD_VALUE -> {
                    totalAddValue += valueToMerge;
                    hasBeenCombined[j] = true;
                }
                case ADD_MULTIPLIED_TOTAL -> {
                    totalMultiplier *= (1 + valueToMerge);
                    hasBeenCombined[j] = true;
                }
                case ADD_MULTIPLIED_BASE -> {
                    // Only add the valueToMerge if the EquipmentSlotGroup isn't equal to any armor piece.
                    if (!Set.of(4, 5, 6, 7).contains(entryToMerge.slot().ordinal())) {
                        totalMultiBase *= (1 + valueToMerge);
                        hasBeenCombined[j] = true;
                    }
                }
            }
        }

        return ((totalAddValue * totalMultiBase) * totalMultiplier) - baseValue;
    }

    private static EquipmentSlotGroup getPrimaryGroup(ItemStack stack) {
        if (stack.getItem() instanceof ArmorItem armorItem) {
            return EquipmentSlotGroup.bySlot(armorItem.getEquipmentSlot());
        }
        return EquipmentSlotGroup.MAINHAND;
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
                .forEach(entry -> this.modifiers().computeIfAbsent(key, k -> new ArrayList<>())
                        .add(new Entry(entry.attribute(), new AttributeModifier(entry.modifier().id(), 0, entry.modifier().operation()),
                                entry.slot(), entry.displayType(), entry.baseValue()))));
    }

    public record Entry(Holder<Attribute> attribute, AttributeModifier modifier, EquipmentSlotGroup slot, AttributeDisplayType displayType, double baseValue) {

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

            if (value > comparedValue) {
                return Comparison.HIGHER;
            } else if (value < comparedValue) {
                return Comparison.LOWER;
            } else {
                return Comparison.NONE;
            }
        }
    }
}
