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
import net.twentyytwo.cleanertooltips.util.AttributeManager.IntermediateHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import static net.twentyytwo.cleanertooltips.CleanerTooltips.location;

public class AttributeManager
        extends SimplePreparableReloadListener<Map<ResourceLocation, IntermediateHolder>> {
    public static final ResourceLocation LOCATION = location("attribute_display.json");

    private static final Codec<Map<ResourceLocation, IntermediateHolder>> CODEC =
            Codec.unboundedMap(ResourceLocation.CODEC, IntermediateHolder.CODEC);
    private static final Map<Holder<Attribute>, DisplayTextureHolder> HOLDER_MAP = new HashMap<>();

    @Override
    protected @NotNull Map<ResourceLocation, IntermediateHolder> prepare(
            @NotNull ResourceManager manager,
            @NotNull ProfilerFiller profiler) {
        Map<ResourceLocation, IntermediateHolder> holderMap = new HashMap<>();
        profiler.startTick();

        try (Reader reader = manager.openAsReader(LOCATION)) {
            JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
            CODEC.parse(JsonOps.INSTANCE, object)
                    .resultOrPartial()
                    .ifPresent(holderMap::putAll);
        } catch (IOException ignored) {
        }

        profiler.endTick();
        return holderMap;
    }

    @Override
    protected void apply(@NotNull Map<ResourceLocation, IntermediateHolder> map,
                         @NotNull ResourceManager manager,
                         @NotNull ProfilerFiller profiler) {
        HOLDER_MAP.clear();

        map.forEach((k, v) -> {
            String path = "textures/gui/attribute/"
                    + k.getPath().replaceFirst("(generic|player)\\.", "") + ".png";

            ResourceLocation location = location(path);
            if (manager.getResource(location).isPresent()) {
                var attribute = BuiltInRegistries.ATTRIBUTE.get(k).orElseThrow();
                HOLDER_MAP.put(attribute, new DisplayTextureHolder(location, v.displayType(),
                        v.priority(),
                        v.isPriorityArmor()));
            }
        });
    }

    protected record IntermediateHolder(
            AttributeDisplayType displayType,
            int priority,
            boolean isPriorityArmor
    ) {
        public static final Codec<IntermediateHolder> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                AttributeDisplayType.CODEC.fieldOf("display_type")
                        .forGetter(IntermediateHolder::displayType),
                Codec.INT.fieldOf("priority")
                        .forGetter(IntermediateHolder::priority),
                Codec.BOOL.fieldOf("is_priority_armor")
                        .forGetter(IntermediateHolder::isPriorityArmor)
        ).apply(instance, IntermediateHolder::new));
    }

    private record DisplayTextureHolder(
            ResourceLocation texture,
            AttributeDisplayType displayType,
            int priority,
            boolean isPriorityArmor
    ) {}

    public static AttributeDisplayType getDisplayType(Holder<Attribute> attribute) {
        DisplayTextureHolder holder = HOLDER_MAP.get(attribute);
        return holder != null ? holder.displayType() : AttributeDisplayType.NUMBER;
    }

    @Nullable
    public static ResourceLocation getTexture(Holder<Attribute> attribute) {
        DisplayTextureHolder holder = HOLDER_MAP.get(attribute);
        return holder != null ? holder.texture() : null;
    }

    public static int getPriority(Holder<Attribute> attribute) {
        DisplayTextureHolder holder = HOLDER_MAP.get(attribute);
        return holder != null ? holder.priority() : 99;
    }

    public static int getFullPriority(Holder<Attribute> attribute, boolean isArmor) {
        DisplayTextureHolder holder = HOLDER_MAP.get(attribute);
        if (holder != null) {
            int priority = holder.priority();
            boolean isPriorityArmor = holder.isPriorityArmor();
            return isArmor && isPriorityArmor ? priority
                    : !isArmor && !isPriorityArmor ? priority : priority + 1;
        }
        return 99;
    }

}
