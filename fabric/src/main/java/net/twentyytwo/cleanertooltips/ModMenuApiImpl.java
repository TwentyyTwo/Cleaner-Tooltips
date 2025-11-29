package net.twentyytwo.cleanertooltips;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfig;
import net.twentyytwo.cleanertooltips.config.CleanerTooltipsConfig;

public class ModMenuApiImpl implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> AutoConfig.getConfigScreen(CleanerTooltipsConfig.class, parent).get();
    }
}
