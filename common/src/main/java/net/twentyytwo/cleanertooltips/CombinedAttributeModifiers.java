package net.twentyytwo.cleanertooltips;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.twentyytwo.cleanertooltips.util.AttributeDisplayType;
import net.twentyytwo.cleanertooltips.util.AttributeHelper;
import net.twentyytwo.cleanertooltips.util.AttributeManager;
import net.twentyytwo.cleanertooltips.util.TooltipsUtil;
import net.twentyytwo.cleanertooltips.util.Comparison;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import static net.minecraft.resources.ResourceLocation.withDefaultNamespace;
import static net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE;

/**
 * This record aims to simplify working with attribute modifiers.
 *
 * @see ItemAttributeModifiers
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public record CombinedAttributeModifiers(Multimap<String, Entry> modifiers) {
    public static final CombinedAttributeModifiers EMPTY = new CombinedAttributeModifiers(ImmutableListMultimap.of());

    private static final ResourceLocation SHARPNESS_BONUS_ID = CleanerTooltips.location("sharpness_bonus_damage");
    private static final ResourceLocation BASE_ENTITY_REACH_ID = withDefaultNamespace("base_entity_reach");

    private static final Comparator<Entry> ENTRY_COMPARATOR = Comparator
            .comparing((Entry e) -> AttributeManager.getPriority(e.attribute()))
            .thenComparing((Entry e) -> e.attribute().toString(), String.CASE_INSENSITIVE_ORDER);

    public static CombinedAttributeModifiers fromStack(ItemStack stack) {
        return fromStack(stack, AttributeHelper.ATTRIBUTE_FILTER, AttributeHelper.MODIFIER_FILTER);
    }

    /**
     * Returns a {@code CombinedAttributeModifiers} object with the given parameters.
     * <p>
     * Note: This method automatically creates an additional entry representing the attack damage bonus
     * by the sharpness enchantment, if a modifier with the {@link Item#BASE_ATTACK_DAMAGE_ID} id is present.
     *
     * @param stack             the {@code ItemStack} whose attribute modifiers are used
     * @param attributeFilter   a predicate which returns {@code true} for attributes to be ignored
     * @param modifierFilter    a predicate which returns {@code true} for modifiers to be ignored
     */
    public static CombinedAttributeModifiers fromStack(ItemStack stack,
                                                       Predicate<Holder<Attribute>> attributeFilter,
                                                       Predicate<AttributeModifier> modifierFilter) {
        Builder builder = builder();

        if (CleanerTooltips.config.customOrder) builder.orderValues();

        for (EquipmentSlotGroup slot : EquipmentSlotGroup.values()) {
            Multimap<Holder<Attribute>, AttributeModifier> source = LinkedListMultimap.create();

            stack.forEachModifier(slot, (k, v) -> {
                if (attributeFilter.test(k) || modifierFilter.test(v)) return;

                source.put(k, v);

                // Add the sharpness bonus if the base attack damage modifier id is present
                if (v.is(Item.BASE_ATTACK_DAMAGE_ID)) {
                    double damage = TooltipsUtil.getSharpnessBonus(stack);
                    if (damage > 0) source.put(k, new AttributeModifier(SHARPNESS_BONUS_ID, damage, ADD_VALUE));
                }
            });

            if (source.isEmpty()) continue;

            boolean isExclusive = slot == EquipmentSlotGroup.MAINHAND || slot == EquipmentSlotGroup.OFFHAND
                    || slot == EquipmentSlotGroup.BODY;
            builder.putAll(slot.getSerializedName(), Merger.merge(sortBaseModifiers(source), isExclusive));
        }

        return builder.build();
    }

    private static Multimap<Holder<Attribute>, AttributeModifier> sortBaseModifiers(
            Multimap<Holder<Attribute>, AttributeModifier> map) {
        return map.entries().stream()
                  .sorted(Comparator.comparing(e -> !isBaseModifier(e.getValue())))
                  .collect(Multimaps.toMultimap(Map.Entry::getKey, Map.Entry::getValue, LinkedListMultimap::create));
    }

    public static CombinedAttributeModifiers combine(CombinedAttributeModifiers primary,
                                                     CombinedAttributeModifiers secondary, boolean keepValues) {
        Multimap<String, Entry> thisModifiers = primary.modifiers();
        Multimap<String, Entry> thatModifiers = secondary.modifiers();

        if (Collections.disjoint(thisModifiers.keySet(), thatModifiers.keySet())) return primary;

        Builder builder = builder().putAll(thisModifiers);
        thatModifiers.asMap().forEach((slot, thatEntries) -> {
            if (!thisModifiers.containsKey(slot)) {
                // If the primary modifiers don't contain this slot of the secondary modifiers,
                // add all the entries of that slot unconditionally
                for (Entry e : thatEntries) builder.put(slot, keepValues ? e : e.withoutAmount());
            } else {
                // Compare each of the secondary entries against the primary entries, and add missing
                // entries to the primary modifiers if none of them match based on stream predicate
                Collection<Entry> entries = thisModifiers.get(slot);
                for (Entry e : thatEntries) {
                    if (entries.stream().noneMatch(e::isComparableWith)) {
                        builder.put(slot, keepValues ? e : e.withoutAmount());
                    }
                }
            }
        });

        return builder.build();
    }

    private static boolean isBaseModifier(AttributeModifier modifier) {
        return modifier.is(Item.BASE_ATTACK_DAMAGE_ID)
                || modifier.is(Item.BASE_ATTACK_SPEED_ID)
                || modifier.is(BASE_ENTITY_REACH_ID);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        ImmutableListMultimap.Builder<String, Entry> entries = ImmutableListMultimap.builder();

        Builder() {}

        public Builder orderValues() {
            this.entries.orderValuesBy(ENTRY_COMPARATOR);
            return this;
        }

        public Builder put(String slotGroup, Entry entry) {
            this.entries.put(slotGroup, entry);
            return this;
        }

        public Builder put(String slotGroup, Holder<Attribute> attribute, AttributeModifier modifier,
                           AttributeDisplayType displayType, boolean isExclusive) {
            this.entries.put(slotGroup, new Entry(attribute, modifier, displayType, isExclusive));
            return this;
        }

        public Builder putAll(Multimap<String, Entry> entries) {
            this.entries.putAll(entries);
            return this;
        }

        public Builder putAll(String slot, Collection<Entry> entries) {
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
        static final ResourceLocation MERGED_ID = CleanerTooltips.location("merged_modifier");
        Multimap<Holder<Attribute>, AttributeModifier> sourceEntries = LinkedListMultimap.create();

        Merger() {}

        public Merger put(Holder<Attribute> attribute, AttributeModifier modifier) {
            this.sourceEntries.put(attribute, modifier);
            return this;
        }

        public Collection<Entry> merge(boolean exclusiveSlot) {
            return merge(this.sourceEntries, exclusiveSlot);
        }

        public static Collection<Entry> merge(Multimap<Holder<Attribute>, AttributeModifier> source,
                                              boolean exclusiveSlot) {
            return exclusiveSlot ? mergeCombined(source) : mergeSeparate(source);
        }

        // Merges modifiers with the same operation
        private static Collection<Entry> mergeSeparate(Multimap<Holder<Attribute>, AttributeModifier> source) {
            Collection<Entry> entries = new ArrayList<>(source.size());
            BiConsumer<Holder<Attribute>, AttributeModifier> adder = (a, m) ->
                    entries.add(new Entry(a, m, AttributeManager.getDisplayType(a).verify(m), false));

            source.asMap().forEach((attribute, modifiers) -> {
                if (modifiers.size() > 1) {
                    // First, gather the modifier amounts and sort them based on operation
                    double[] amounts = new double[3];
                    for (var m : modifiers) amounts[m.operation().id()] += m.amount();
                    // Then add an entry to the list for every non-zero modifier amount
                    for (int i = 0; i < 3; i++) {
                        if (amounts[i] > 0.0) {
                            adder.accept(attribute, new AttributeModifier(MERGED_ID, amounts[i], ADD_VALUE));
                        }
                    }
                } else {
                    for (AttributeModifier modifier : modifiers) {
                        if (modifier.amount() != 0) adder.accept(attribute, modifier);
                    }
                }
            });
            return entries;
        }

        // Merges modifiers regardless of their operations
        private static Collection<Entry> mergeCombined(Multimap<Holder<Attribute>, AttributeModifier> source) {
            Collection<Entry> entries = new ArrayList<>(source.keySet().size());
            source.asMap().forEach((attribute, modifiers) -> {
                var type = AttributeManager.getDisplayType(attribute);

                if (modifiers.size() > 1) {
                    double baseValue = AttributeHelper.getBaseValue(attribute);
                    double amount = mergeValues(modifiers, baseValue);
                    if (amount + (type.hasBaseValue() ? baseValue : 0) != 0) {
                        entries.add(new Entry(attribute, new AttributeModifier(MERGED_ID, amount, ADD_VALUE), type, true));
                    }
                } else {
                    for (AttributeModifier m : modifiers) {
                        if (m.amount() + (type.hasBaseValue() ? AttributeHelper.getBaseValue(attribute) : 0) != 0) {
                            entries.add(new Entry(attribute, m, type, true));
                        }
                    }
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

    // isExclusive indicates whether this entry affects exclusively the item it is applied to.
    public record Entry(
            Holder<Attribute> attribute,
            AttributeModifier modifier,
            AttributeDisplayType displayType,
            boolean isExclusive
    ) {

        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        Attribute.CODEC.fieldOf("type").forGetter(Entry::attribute),
                        AttributeModifier.MAP_CODEC.forGetter(Entry::modifier),
                        AttributeDisplayType.CODEC
                                .optionalFieldOf("display_type", AttributeDisplayType.NUMBER)
                                .forGetter(Entry::displayType),
                        Codec.BOOL.fieldOf("is_exclusive").forGetter(Entry::isExclusive)
                ).apply(instance, Entry::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC =
                StreamCodec.composite(
                        Attribute.STREAM_CODEC, Entry::attribute,
                        AttributeModifier.STREAM_CODEC, Entry::modifier,
                        AttributeDisplayType.STREAM_CODEC, Entry::displayType,
                        ByteBufCodecs.BOOL, Entry::isExclusive,
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

        public boolean isComparableWith(Entry that) {
            return this.matchesAttribute(that)
                    && ((this.isExclusive && that.isExclusive) || this.matchesOperation(that));
        }

        public Entry withoutAmount() {
            return new Entry(
                    this.attribute, new AttributeModifier(modifier.id(), 0, modifier.operation()),
                    this.displayType, this.isExclusive
            );
        }

        public Comparison getComparison(double thatValue, double thatBaseValue) {
            double value = this.modifier().amount();
            double comparedValue = thatValue;

            if (this.displayType().hasBaseValue()) {
                value += AttributeHelper.getBaseValue(this.attribute);
                comparedValue += thatBaseValue;
            }

            return Comparison.getComparison(value, comparedValue);
        }

        public Comparison getComparison(Entry that) {
            return getComparison(that.modifier().amount(), AttributeHelper.getBaseValue(that.attribute()));
        }

        public Comparison getComparison() {
            return getComparison(0, 0);
        }
    }
}
