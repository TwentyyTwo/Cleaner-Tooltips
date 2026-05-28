package net.twentyytwo.cleanertooltips.compat;

import com.anthonyhilyard.iceberg.component.TitleBreakComponent;
import com.mojang.datafixers.util.Either;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.twentyytwo.cleanertooltips.services.Services;

import java.util.List;

public class LegendaryTooltipsHandler {

    public static final boolean isModLoaded = Services.getInstance()
            .isModLoaded("legendarytooltips");
    public static final int increasedHeight = 1;

    public static boolean hasTitleBreakNeoforge(List<Either<FormattedText, TooltipComponent>> elements) {
        return elements.stream().anyMatch(either -> either.right()
                .filter(component -> component instanceof TitleBreakComponent)
                .isPresent());
    }

    public static boolean hasTitleBreakFabric(List<ClientTooltipComponent> clientTooltipComponents) {
        return clientTooltipComponents.stream()
                .anyMatch(component -> component instanceof TitleBreakComponent);
    }
}
