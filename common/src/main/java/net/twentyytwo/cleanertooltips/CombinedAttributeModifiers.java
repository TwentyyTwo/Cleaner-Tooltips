package net.twentyytwo.cleanertooltips;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

import static net.twentyytwo.cleanertooltips.CleanerTooltips.location;
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
        EquipmentSlotGroup primaryGroup = getPrimaryGroup(stack);
        ListMultimap<Holder<Attribute>, AttributeModifier> sourceEntries = ArrayListMultimap.create();
        double sharpnessBonus = CleanerTooltipsUtil.getSharpnessBonus(stack);

        EquipmentSlotGroup[] values = EquipmentSlotGroup.values();
        for (int i = 0, length = values.length; i < length; i++) {
            EquipmentSlotGroup slot = values[(i + primaryGroup.ordinal()) % length]; // start at the "primary" group, i.e. the group we want rendered first

            stack.forEachModifier(slot, (attribute, modifier) -> {
                if (modifier.amount() != 0) sourceEntries.put(attribute, modifier);
            });

            boolean keepOperationsSeparate = CleanerTooltipsUtil.shouldSeparateOperations(slot);
            builder.putAll(slot, Merger.merge(sourceEntries, keepOperationsSeparate, sharpnessBonus));
            sourceEntries.clear();
        }
        return builder.build();
    }

    private static EquipmentSlotGroup getPrimaryGroup(ItemStack stack) {
        return stack.getItem() instanceof ArmorItem armorItem
                ? EquipmentSlotGroup.bySlot(armorItem.getEquipmentSlot())
                : EquipmentSlotGroup.MAINHAND;
    }

    public CombinedAttributeModifiers merge(CombinedAttributeModifiers other, boolean copyValues) {
        ListMultimap<EquipmentSlotGroup, Entry> otherModifiers = other.modifiers();
        if (otherModifiers.isEmpty() || Collections.disjoint(this.modifiers.keySet(), otherModifiers.keySet())) {
            return this;
        }

        Builder builder = builder().orderValues().putAll(this.modifiers);
        for (Map.Entry<EquipmentSlotGroup, Collection<Entry>> entry : otherModifiers.asMap().entrySet()) {
            EquipmentSlotGroup slot = entry.getKey();
            Collection<Entry> otherEntries = entry.getValue();
            boolean keepOperationsSeparate = CleanerTooltipsUtil.shouldSeparateOperations(slot);

            if (!this.modifiers.containsKey(slot)) {
                for (Entry e : otherEntries) {
                    builder.put(slot, copyValues ? e : e.getWithoutAmount());
                }
            } else {
                Collection<Entry> thisEntries = this.modifiers.get(slot);
                for (Entry otherEntry : otherEntries) {
                    boolean found = false;
                    for (Entry thisEntry : thisEntries) {
                        if (otherEntry.matchesAttribute(thisEntry.attribute())
                                && (!keepOperationsSeparate || otherEntry.matchesOperation(thisEntry.modifier()))) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        builder.put(slot, copyValues ? otherEntry : otherEntry.getWithoutAmount());
                    }
                }
            }
        }
        return builder.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        ImmutableListMultimap.Builder<EquipmentSlotGroup, Entry> entries = ImmutableListMultimap.builder();

        Builder() {}

        public Builder orderValues() {
            this.entries.orderValuesBy(Entry.comparator);
            return this;
        }

        public Builder put(EquipmentSlotGroup slotGroup, Entry entry) {
            this.entries.put(slotGroup, entry);
            return this;
        }

        public Builder put(EquipmentSlotGroup slotGroup, Holder<Attribute> attribute, AttributeModifier modifier, AttributeDisplayType displayType) {
            this.entries.put(slotGroup, new Entry(attribute, modifier, displayType));
            return this;
        }

        public Builder putAny(Holder<Attribute> attribute, AttributeModifier modifier, AttributeDisplayType displayType) {
            this.entries.put(EquipmentSlotGroup.ANY, new Entry(attribute, modifier, displayType));
            return this;
        }

        public Builder putAny(Entry entry) {
            this.entries.put(EquipmentSlotGroup.ANY, entry);
            return this;
        }

        public Builder putAll(ListMultimap<EquipmentSlotGroup, Entry> entries) {
            this.entries.putAll(entries);
            return this;
        }

        public Builder putAll(EquipmentSlotGroup slot, Collection<Entry> entries) {
            this.entries.putAll(slot, entries);
            return this;
        }

        public CombinedAttributeModifiers build() {
            return new CombinedAttributeModifiers(this.entries.build());
        }
    }

    public static Merger merger() {
        return new Merger();
    }

    public static class Merger {
        static ResourceLocation mergedId = location("merged_modifier");
        ListMultimap<Holder<Attribute>, AttributeModifier> sourceEntries = ArrayListMultimap.create();

        Merger() {}

        public Merger put(Holder<Attribute> attribute, AttributeModifier modifier) {
            this.sourceEntries.put(attribute, modifier);
            return this;
        }

        public Collection<Entry> merge(boolean keepOperationsSeparate,
                                       double sharpnessBonus) {
            return merge(this.sourceEntries, keepOperationsSeparate, sharpnessBonus);
        }

        public static Collection<Entry> merge(ListMultimap<Holder<Attribute>, AttributeModifier> source,
                                              boolean keepOperationsSeparate,
                                              double sharpnessBonus) {
            if (BetterCombatCompat.isModLoaded) source.removeAll(Attributes.ENTITY_INTERACTION_RANGE);

            return keepOperationsSeparate
                    ? mergeSeparate(source)
                    : mergeCombined(source, sharpnessBonus);
        }

        private static Collection<Entry> mergeSeparate(ListMultimap<Holder<Attribute>, AttributeModifier> source) {
            Collection<Entry> entries = new ArrayList<>(source.size());
            for (Map.Entry<Holder<Attribute>, Collection<AttributeModifier>> entry : source.asMap().entrySet()) {
                Holder<Attribute> attribute = entry.getKey();
                Collection<AttributeModifier> modifiers = entry.getValue();

                if (modifiers.size() > 1) {
                    double[] mergedAmounts = new double[3];
                    for (AttributeModifier modifier : modifiers) mergedAmounts[modifier.operation().id()] += modifier.amount();
                    for (int i = 0; i < 3; i++) if (mergedAmounts[i] > 0.0) {
                        AttributeModifier mergedModifier = new AttributeModifier(mergedId, mergedAmounts[i], Operation.values()[i]);
                        entries.add(new Entry(attribute, mergedModifier, true));
                    }
                } else {
                    AttributeModifier modifier = modifiers.iterator().next();
                    entries.add(new Entry(attribute, modifier, true));
                }
            }
            return entries;
        }

        private static Collection<Entry> mergeCombined(ListMultimap<Holder<Attribute>, AttributeModifier> source,
                                                       double sharpnessBonus) {
            Collection<Entry> entries = new ArrayList<>(source.keySet().size());
            for (Map.Entry<Holder<Attribute>, Collection<AttributeModifier>> entry : source.asMap().entrySet()) {
                Holder<Attribute> attribute = entry.getKey();
                Collection<AttributeModifier> modifiers = entry.getValue();

                if (modifiers.size() > 1) {
                    double mergedAmount = getMergedValue(modifiers, getBaseValue(attribute));
                    if (shouldAddSharpness(attribute, modifiers)) mergedAmount += sharpnessBonus;
                    AttributeModifier mergedModifier = new AttributeModifier(mergedId, mergedAmount, Operation.ADD_VALUE);
                    entries.add(new Entry(attribute, mergedModifier, false));
                } else {
                    AttributeModifier modifier = modifiers.iterator().next();
                    entries.add(new Entry(attribute, modifier, false));
                }
            }
            return entries;
        }

        private static double getMergedValue(Collection<AttributeModifier> modifiers, double baseValue) {
            double totalAddValue = baseValue;
            double totalBaseMultiplier = 1;
            double totalMultiplier = 1;

            for (AttributeModifier modifier : modifiers) {
                switch (modifier.operation()) {
                    case ADD_VALUE -> totalAddValue += modifier.amount();
                    case ADD_MULTIPLIED_BASE -> totalBaseMultiplier += modifier.amount();
                    case ADD_MULTIPLIED_TOTAL -> totalMultiplier *= (1 + modifier.amount());
                }
            }

            return ((totalAddValue * totalBaseMultiplier) * totalMultiplier) - baseValue;
        }

        private static boolean shouldAddSharpness(Holder<Attribute> attribute, Collection<AttributeModifier> modifiers) {
            if (attribute.equals(Attributes.ATTACK_DAMAGE)) {
                for (AttributeModifier modifier : modifiers) {
                    if (modifier.is(Item.BASE_ATTACK_DAMAGE_ID)) return true;
                }
            }
            return false;
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

        public Entry(Holder<Attribute> attribute,
                     AttributeModifier modifier,
                     boolean keepOperationsSeparate) {
            this(attribute, modifier, AttributeManager.getDisplayType(attribute, modifier, keepOperationsSeparate));
        }

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
