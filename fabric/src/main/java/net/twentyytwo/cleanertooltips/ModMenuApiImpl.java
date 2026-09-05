package net.twentyytwo.cleanertooltips;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.twentyytwo.cleanertooltips.config.TooltipsClothConfig;

public class ModMenuApiImpl implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return TooltipsClothConfig::getConfigScreen;
    }
}
