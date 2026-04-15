package net.twentyytwo.cleanertooltips.compat;

import com.anthonyhilyard.iceberg.component.TitleBreakComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.twentyytwo.cleanertooltips.services.Services;

import java.util.List;

public class LegendaryTooltipsCompat {

    public static final boolean isModLoaded = Services.getInstance().isModLoaded("legendarytooltips");
    public static final int increasedHeight = 1;

    public static boolean hasTitleBreak(List<ClientTooltipComponent> clientTooltipComponents) {
        return clientTooltipComponents.stream().anyMatch(component -> component instanceof TitleBreakComponent);
    }
}
