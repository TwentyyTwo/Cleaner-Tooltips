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
import net.twentyytwo.cleanertooltips.compat.BetterCombatHandler;
import net.twentyytwo.cleanertooltips.util.AttributeDisplayType;
import net.twentyytwo.cleanertooltips.util.AttributeManager;
import net.twentyytwo.cleanertooltips.util.TooltipsUtil;
import net.twentyytwo.cleanertooltips.util.Comparison;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.BiConsumer;

import static net.twentyytwo.cleanertooltips.util.TooltipsUtil.getBaseValue;

/**
 * A record containing data to simplify working with attribute modifiers.
 *
 * @see ItemAttributeModifiers
 */
@SuppressWarnings("unused")
public class CombinedAttributeModifiers {
    private ListMultimap<EquipmentSlotGroup, Entry> modifiers;

    private boolean isArmor = false;
    private boolean copyValues = false;

    private static final ResourceLocation sharpnessId = CleanerTooltips.location("sharpness_attack_damage");
    private static final Operation[] OPERATIONS = Operation.values();

    public static final CombinedAttributeModifiers EMPTY = new CombinedAttributeModifiers(ImmutableListMultimap.of());

    public CombinedAttributeModifiers(ListMultimap<EquipmentSlotGroup, Entry> modifiers) {
        this.modifiers = modifiers;
    }

    public CombinedAttributeModifiers(ItemStack stack) {
        this.isArmor = stack.getItem() instanceof ArmorItem;
        this.copyValues = false;

        Builder builder = builder().orderValues(isArmor);
        boolean skip = BetterCombatHandler.isModLoaded && BetterCombatHandler.hasAttributes(stack);

        ListMultimap<Holder<Attribute>, AttributeModifier> source = ArrayListMultimap.create();
        for (var slot : TooltipsUtil.shiftArray(EquipmentSlotGroup.values(), getPrimary(stack))) {
            stack.forEachModifier(slot, (k, v) -> {
                if (!skip || !k.equals(Attributes.ENTITY_INTERACTION_RANGE)) source.put(k, v);
                if (v.is(Item.BASE_ATTACK_DAMAGE_ID)) {
                    double damage = TooltipsUtil.getSharpnessBonus(stack);
                    if (damage > 0) source.put(k, createModifier(sharpnessId, damage, 0));
                }
            });

            builder.putAll(slot, Merger.merge(source, TooltipsUtil.isExclusive(slot)));
            source.clear();
        }
        this.modifiers = builder.build().modifiers();
    }

    private static int getPrimary(ItemStack stack) {
        return stack.getItem() instanceof ArmorItem armorItem
                ? EquipmentSlotGroup.bySlot(armorItem.getEquipmentSlot()).ordinal() : 1;
    }

    public void combine(CombinedAttributeModifiers other) {
        ListMultimap<EquipmentSlotGroup, Entry> otherModifiers = other.modifiers();

        // If the other modifiers don't have any slot groups in common with the current modifiers, don't combine
        if (Collections.disjoint(this.modifiers.keySet(), otherModifiers.keySet())) return;

        Builder builder = builder().orderValues(isArmor).putAll(this.modifiers);
        otherModifiers.asMap().forEach((slot, otherEntries) -> {
            if (!this.modifiers.containsKey(slot)) {
                // If the current modifiers don't contain this slot of the compared modifiers,
                // add all the other entries of that slot unconditionally
                for (Entry e : otherEntries) builder.put(slot, copyValues ? e : e.withoutAmount());
            } else {
                // Compare each of the other entries against the current entries, and add missing
                // entries to the current modifiers if none of them match based on stream predicate
                Collection<Entry> entries = this.modifiers.get(slot);
                for (Entry e1 : otherEntries) {
                    if (entries.stream().noneMatch(e2 -> e1.isComparableWith(e2, slot))) {
                        builder.put(slot, copyValues ? e1 : e1.withoutAmount());
                    }
                }
            }
        });
        this.modifiers = builder.build().modifiers();
    }

    private static AttributeModifier createModifier(ResourceLocation id, double amount, int operation) {
        return new AttributeModifier(id, amount, OPERATIONS[operation]);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        ImmutableListMultimap.Builder<EquipmentSlotGroup, Entry> entries =
                ImmutableListMultimap.builder();

        Builder() {}

        public Builder orderValues(boolean isArmor) {
            this.entries.orderValuesBy(Comparator
                    .comparing((Entry e) -> AttributeManager.getPriority(e.attribute(), isArmor))
                    .thenComparing((Entry e) -> e.attribute().toString(), String.CASE_INSENSITIVE_ORDER));
            return this;
        }

        public Builder put(EquipmentSlotGroup slotGroup, Entry entry) {
            this.entries.put(slotGroup, entry);
            return this;
        }

        public Builder put(EquipmentSlotGroup slotGroup, Holder<Attribute> attribute,
                           AttributeModifier modifier, AttributeDisplayType displayType) {
            this.entries.put(slotGroup, new Entry(attribute, modifier, displayType));
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
        static final ResourceLocation mergedId = CleanerTooltips.location("merged_modifier");
        ListMultimap<Holder<Attribute>, AttributeModifier> sourceEntries = ArrayListMultimap.create();

        Merger() {}

        public Merger put(Holder<Attribute> attribute, AttributeModifier modifier) {
            this.sourceEntries.put(attribute, modifier);
            return this;
        }

        public Collection<Entry> merge(boolean exclusiveSlot) {
            return merge(this.sourceEntries, exclusiveSlot);
        }

        public static Collection<Entry> merge(ListMultimap<Holder<Attribute>, AttributeModifier> source,
                                              boolean exclusiveSlot) {
            return exclusiveSlot ? mergeCombined(source) : mergeSeparate(source);
        }

        private static Collection<Entry> mergeSeparate(ListMultimap<Holder<Attribute>, AttributeModifier> source) {
            Collection<Entry> entries = new ArrayList<>(source.size());
            BiConsumer<Holder<Attribute>, AttributeModifier> adder = (a, m) ->
                    entries.add(new Entry(a, m, AttributeManager.getDisplayType(a).verify(m)));

            source.asMap().forEach((attribute, modifiers) -> {
                if (modifiers.size() > 1) {
                    // First, gather the modifier amounts and sort them based on operation
                    double[] amounts = new double[3];
                    for (var m : modifiers) amounts[m.operation().id()] += m.amount();
                    // Then add an entry to the list for every non-zero modifier amount
                    for (int i = 0; i < 3; i++) {
                        if (amounts[i] > 0.0) adder.accept(attribute, createModifier(mergedId, amounts[i], i));
                    }
                } else {
                    var modifier = modifiers.iterator().next();
                    if (modifier.amount() != 0) adder.accept(attribute, modifier);
                }
            });
            return entries;
        }

        private static Collection<Entry> mergeCombined(ListMultimap<Holder<Attribute>, AttributeModifier> source) {
            Collection<Entry> entries = new ArrayList<>(source.keySet().size());
            source.asMap().forEach((attribute, modifiers) -> {
                var type = AttributeManager.getDisplayType(attribute);

                double baseValue = getBaseValue(attribute);
                double amount = mergeValues(modifiers, baseValue);
                if (amount + (type.hasBaseValue() ? baseValue : 0) != 0) {
                    entries.add(new Entry(attribute, createModifier(mergedId, amount, 0), type));
                }
            });
            return entries;
        }

        private static double mergeValues(Collection<AttributeModifier> modifiers, double baseValue) {
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
    }

    public record Entry(
            Holder<Attribute> attribute,
            AttributeModifier modifier,
            AttributeDisplayType displayType
    ) {

        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        Attribute.CODEC.fieldOf("type").forGetter(Entry::attribute),
                        AttributeModifier.MAP_CODEC.forGetter(Entry::modifier),
                        AttributeDisplayType.CODEC
                                .optionalFieldOf("display_type", AttributeDisplayType.NUMBER)
                                .forGetter(Entry::displayType)
        ).apply(instance, Entry::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC =
                StreamCodec.composite(
                        Attribute.STREAM_CODEC, Entry::attribute,
                        AttributeModifier.STREAM_CODEC, Entry::modifier,
                        AttributeDisplayType.STREAM_CODEC, Entry::displayType,
                        Entry::new
        );

        public boolean matchesAttribute(Entry that) {
            return this.attribute.equals(that.attribute());
        }

        public boolean matchesModifier(Entry that) {
            return this.modifier.equals(that.modifier());
        }

        public boolean matchesOperation(Entry that) {
            return this.modifier.operation().equals(that.modifier().operation());
        }

        public boolean isComparableWith(Entry that, EquipmentSlotGroup slot) {
            return this.matchesAttribute(that) && (TooltipsUtil.isExclusive(slot) || this.matchesOperation(that));
        }

        public Entry withoutAmount() {
            return new Entry(
                    this.attribute, new AttributeModifier(modifier.id(), 0, modifier.operation()), this.displayType
            );
        }

        public Comparison getComparison(double thatValue, double thatBaseValue) {
            double value = this.modifier().amount();
            double comparedValue = thatValue;

            if (this.displayType().hasBaseValue()) {
                value += getBaseValue(this.attribute);
                comparedValue += thatBaseValue;
            }

            return Comparison.getComparison(value, comparedValue);
        }

        public Comparison getComparison(Entry that) {
            return getComparison(that.modifier().amount(), getBaseValue(that.attribute()));
        }

        public Comparison getComparison() {
            return getComparison(0, 0);
        }
    }

    public ListMultimap<EquipmentSlotGroup, Entry> modifiers() {
        return modifiers;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CombinedAttributeModifiers that)) return false;
        return isArmor == that.isArmor && copyValues == that.copyValues && Objects.equals(modifiers, that.modifiers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modifiers, isArmor, copyValues);
    }

    @Override
    public String toString() {
        return "CombinedAttributeModifiers{" +
                "modifiers=" + modifiers + ", isArmor=" + isArmor + ", copyValues=" + copyValues + '}';
    }
}
