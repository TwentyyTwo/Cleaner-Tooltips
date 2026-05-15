package net.twentyytwo.cleanertooltips.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.twentyytwo.cleanertooltips.CleanerTooltips;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

public class AttributeManager extends SimplePreparableReloadListener<Map<ResourceLocation, AttributeManager.IntermediateHolder>> {
    private static final Codec<Map<ResourceLocation, IntermediateHolder>> CODEC = Codec.unboundedMap(ResourceLocation.CODEC, IntermediateHolder.CODEC);
    private static final ResourceLocation ATTRIBUTE_DISPLAY_LOCATION = CleanerTooltips.location("attribute_display.json");
    private static final Map<ResourceLocation, DisplayTextureHolder> HOLDER_MAP = new HashMap<>();

    @Override
    protected @NotNull Map<ResourceLocation, IntermediateHolder> prepare(@NotNull ResourceManager manager, @NotNull ProfilerFiller profiler) {
        Map<ResourceLocation, IntermediateHolder> holderMap = new HashMap<>();
        profiler.startTick();

        try (Reader reader = manager.openAsReader(ATTRIBUTE_DISPLAY_LOCATION)) {
            JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
            CODEC.parse(JsonOps.INSTANCE, object)
                    .resultOrPartial()
                    .ifPresent(holderMap::putAll);
        } catch (IOException ignored) {}

        profiler.endTick();
        return holderMap;
    }

    @Override
    protected void apply(@NotNull Map<ResourceLocation, IntermediateHolder> map, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        HOLDER_MAP.clear();
        map.forEach(AttributeManager::accept);
    }

    private static void accept(ResourceLocation k, IntermediateHolder v) {
        String path = "textures/gui/attribute/" + k.getPath().replaceFirst("(generic|player)\\.", "") + ".png";
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(CleanerTooltips.MOD_ID, path);
        if (CleanerTooltips.MC.getResourceManager().getResource(location).isPresent()) {
            HOLDER_MAP.put(k, new DisplayTextureHolder(location, v.displayType(), v.priority()));
        }
    }

    protected record IntermediateHolder(AttributeDisplayType displayType, int priority) {
        public static final Codec<IntermediateHolder> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                AttributeDisplayType.CODEC.fieldOf("display_type").forGetter(IntermediateHolder::displayType),
                Codec.INT.fieldOf("priority").forGetter(IntermediateHolder::priority)
        ).apply(instance, IntermediateHolder::new));
    }

    private record DisplayTextureHolder(ResourceLocation texture, AttributeDisplayType displayType, int priority) {}

    public static AttributeDisplayType getDisplayType(Holder<Attribute> attribute, AttributeModifier modifier, boolean keepOperationsSeparate) {
        if (keepOperationsSeparate && !modifier.operation().equals(AttributeModifier.Operation.ADD_VALUE)) {
            return AttributeDisplayType.PERCENTAGE;
        }

        ResourceLocation key = BuiltInRegistries.ATTRIBUTE.getKey(attribute.value());
        if (HOLDER_MAP.containsKey(key)) {
            return HOLDER_MAP.get(key).displayType();
        }
        return AttributeDisplayType.NUMBER;
    }

    @Nullable
    public static ResourceLocation getTexture(Holder<Attribute> attribute) {
        ResourceLocation key = BuiltInRegistries.ATTRIBUTE.getKey(attribute.value());
        return HOLDER_MAP.containsKey(key) ? HOLDER_MAP.get(key).texture() : null;
    }

    public static int getPriority(Holder<Attribute> attribute) {
        ResourceLocation key = BuiltInRegistries.ATTRIBUTE.getKey(attribute.value());
        return HOLDER_MAP.containsKey(key) ? HOLDER_MAP.get(key).priority() : 99;
    }
}
