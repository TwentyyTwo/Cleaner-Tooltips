package net.twentyytwo.cleanertooltips;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
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

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import static net.twentyytwo.cleanertooltips.util.CleanerTooltipsUtil.getBaseValue;

/**
 * A record containing data to simplify working with attribute modifiers.
 *
 * @param modifiers the map of attribute modifiers
 * @see ItemAttributeModifiers
 */
public record CombinedAttributeModifiers(ListMultimap<EquipmentSlotGroup, Entry> modifiers) {
    public static final CombinedAttributeModifiers EMPTY = new CombinedAttributeModifiers(ImmutableListMultimap.of());

    public static CombinedAttributeModifiers fromStack(ItemStack stack) {
        Builder builder = builder().orderValues();
        double sharpnessBonus = CleanerTooltipsUtil.getSharpnessBonus(stack);

        EquipmentSlotGroup[] values = EquipmentSlotGroup.values();
        for (int i = 0; i < values.length; i++) {
            EquipmentSlotGroup slot = values[(i + getPrimaryGroup(stack).ordinal()) % values.length]; // start at the "primary" group, i.e. the group we want rendered first
            Builder.MergeBuilder mergeBuilder = builder.mergeBuilder();

            boolean keepOperationsSeparate = CleanerTooltipsUtil.shouldSeparateOperations(slot);
            stack.forEachModifier(slot, mergeBuilder::add);

            mergeBuilder.build(keepOperationsSeparate, true, sharpnessBonus).entries().forEach(entry -> {
                AttributeDisplayType displayType = AttributeManager.getDisplayType(entry.getKey(), entry.getValue(), slot, keepOperationsSeparate);
                builder.add(entry.getKey(), entry.getValue(), displayType, slot);
            });
        }
        return builder.build();
    }

    private static EquipmentSlotGroup getPrimaryGroup(ItemStack stack) {
        return stack.getItem() instanceof ArmorItem armorItem
                ? EquipmentSlotGroup.bySlot(armorItem.getEquipmentSlot())
                : EquipmentSlotGroup.MAINHAND;
    }

    public CombinedAttributeModifiers merge(CombinedAttributeModifiers other, boolean copyValues) {
        if (other.modifiers().isEmpty() || this.modifiers.keySet().stream().noneMatch(slot -> other.modifiers().keySet().contains(slot))) return this;
        Builder builder = builder().orderValues().addAll(this.modifiers);
        other.modifiers().entries().forEach(entry -> {
            boolean keepOperationsSeparate = CleanerTooltipsUtil.shouldSeparateOperations(entry.getKey());
            Entry entryValue = entry.getValue();
            if (!this.modifiers.containsKey(entry.getKey()) || this.modifiers.get(entry.getKey()).stream()
                    .noneMatch(thisEntry -> entryValue.matchesAttribute(thisEntry.attribute()) && (!keepOperationsSeparate || entryValue.matchesOperation(thisEntry.modifier())))) {
                builder.add(copyValues ? entryValue : entryValue.getWithoutAmount(), entry.getKey());
            }
        });
        return builder.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        ImmutableListMultimap.Builder<EquipmentSlotGroup, Entry> entries = ImmutableListMultimap.builder();

        public MergeBuilder mergeBuilder() {
            return new MergeBuilder();
        }

        Builder() {}

        public Builder orderValues() {
            this.entries.orderValuesBy(Entry.comparator);
            return this;
        }

        public Builder add(Entry entry, EquipmentSlotGroup slotGroup) {
            this.entries.put(slotGroup, entry);
            return this;
        }

        public Builder add(Holder<Attribute> attribute, AttributeModifier modifier, AttributeDisplayType displayType, EquipmentSlotGroup slotGroup) {
            this.entries.put(slotGroup, new Entry(attribute, modifier, displayType));
            return this;
        }

        public Builder add(Holder<Attribute> attribute, AttributeModifier modifier, AttributeDisplayType displayType) {
            this.entries.put(EquipmentSlotGroup.ANY, new Entry(attribute, modifier, displayType));
            return this;
        }

        public Builder addAll(ListMultimap<EquipmentSlotGroup, Entry> entries) {
            this.entries.putAll(entries);
            return this;
        }

        public CombinedAttributeModifiers build() {
            return new CombinedAttributeModifiers(this.entries.build());
        }

        public static class MergeBuilder {
            ListMultimap<Holder<Attribute>, AttributeModifier> sourceEntries = ArrayListMultimap.create();

            MergeBuilder() {}

            public MergeBuilder add(Holder<Attribute> attribute, AttributeModifier modifier) {
                this.sourceEntries.put(attribute, modifier);
                return this;
            }

            public ListMultimap<Holder<Attribute>, AttributeModifier> build(boolean keepOperationsSeparate, boolean ignoreZeroValues, double sharpnessBonus) {
                ImmutableListMultimap.Builder<Holder<Attribute>, AttributeModifier> entriesToReturn = ImmutableListMultimap.builder();

                for (Holder<Attribute> key : sourceEntries.keySet()) {
                    if (BetterCombatCompat.isModLoaded && key.equals(Attributes.ENTITY_INTERACTION_RANGE)) continue;

                    if (keepOperationsSeparate) {
                        ListMultimap<AttributeModifier.Operation, AttributeModifier> modifierGroups = Multimaps.index(sourceEntries.get(key), AttributeModifier::operation);
                        for (Collection<AttributeModifier> modifiers : modifierGroups.asMap().values()) {
                            AttributeModifier modifier = modifiers.iterator().next();
                            double amount = getMergedValue(modifiers, getBaseValue(key), true)
                                    + (modifier.is(Item.BASE_ATTACK_DAMAGE_ID) ? sharpnessBonus : 0);
                            if (!ignoreZeroValues || amount != 0) {
                                entriesToReturn.put(key, new AttributeModifier(modifier.id(), amount, modifier.operation()));
                            }
                        }
                    } else {
                        Set<AttributeModifier> modifiers = new HashSet<>(sourceEntries.get(key));
                        AttributeModifier modifier = modifiers.iterator().next();
                        double amount = getMergedValue(modifiers, getBaseValue(key), false)
                                + (modifier.is(Item.BASE_ATTACK_DAMAGE_ID) ? sharpnessBonus : 0);
                        if (!ignoreZeroValues || amount != 0) {
                            entriesToReturn.put(key, new AttributeModifier(modifier.id(), amount, modifier.operation()));
                        }
                    }
                }

                return entriesToReturn.build();
            }

            private static double getMergedValue(Collection<AttributeModifier> modifiers, double baseValue, boolean keepOperationsSeparate) {
                double totalAddValue = baseValue;
                double totalBaseMultiplier = 1;
                double totalMultiplier = 1;

                if (keepOperationsSeparate) {
                    totalAddValue += modifiers.stream().mapToDouble(AttributeModifier::amount).sum();
                } else {
                    for (AttributeModifier modifier : modifiers) {
                        double amount = modifier.amount();
                        switch (modifier.operation()) {
                            case ADD_VALUE -> totalAddValue += amount;
                            case ADD_MULTIPLIED_BASE -> totalBaseMultiplier += amount;
                            case ADD_MULTIPLIED_TOTAL -> totalMultiplier *= (1 + amount);
                        }
                    }
                }

                return ((totalAddValue * totalBaseMultiplier) * totalMultiplier) - baseValue;
            }
        }
    }

    public record Entry(Holder<Attribute> attribute, AttributeModifier modifier, AttributeDisplayType displayType) {

        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Attribute.CODEC.fieldOf("type").forGetter(Entry::attribute),
                AttributeModifier.MAP_CODEC.forGetter(Entry::modifier),
                AttributeDisplayType.CODEC.optionalFieldOf("display_type", AttributeDisplayType.NUMBER).forGetter(Entry::displayType)
        ).apply(instance, Entry::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                Attribute.STREAM_CODEC, Entry::attribute,
                AttributeModifier.STREAM_CODEC, Entry::modifier,
                AttributeDisplayType.STREAM_CODEC, Entry::displayType,
                Entry::new
        );

        public static Comparator<Entry> comparator = Comparator
                .comparing((Entry entry) -> AttributeManager.getPriority(entry.attribute()))
                .thenComparing((Entry entry) -> entry.attribute().toString(), String.CASE_INSENSITIVE_ORDER);

        public boolean matchesAttribute(Holder<Attribute> otherAttribute) {
            return this.attribute.equals(otherAttribute);
        }

        public boolean matchesModifier(AttributeModifier otherModifier) {
            return this.modifier.equals(otherModifier);
        }

        public boolean matchesOperation(AttributeModifier otherModifier) {
            return this.modifier.operation().equals(otherModifier.operation());
        }

        public Entry getWithoutAmount() {
            return new Entry(this.attribute(), new AttributeModifier(this.modifier().id(), 0, this.modifier().operation()), this.displayType());
        }

        public boolean isComparable(Entry comparedEntry, boolean keepOperationsSeparate) {
            boolean baseCheck = this.matchesAttribute(comparedEntry.attribute());
            return keepOperationsSeparate ? baseCheck && this.matchesOperation(comparedEntry.modifier()) : baseCheck;
        }

        public Comparison getComparison(Entry comparedEntry) {
            return getComparison(comparedEntry.modifier().amount(), getBaseValue(comparedEntry.attribute()));
        }

        public Comparison getComparison(double otherValue, double otherBaseValue) {
            double value = this.modifier().amount();
            double comparedValue = otherValue;

            if (this.displayType().hasBaseValue()) {
                value += getBaseValue(this.attribute);
                comparedValue += otherBaseValue;
            }

            return Comparison.getComparison(value, comparedValue);
        }
    }
}
