package net.twentyytwo.cleanertooltips.util;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.twentyytwo.cleanertooltips.CleanerTooltips;

public class FabricAttributeManager extends AttributeManager implements IdentifiableResourceReloadListener {

    private static final ResourceLocation ID = CleanerTooltips.location("attribute_manager");

    @Override
    public ResourceLocation getFabricId() {
        return ID;
    }
}
