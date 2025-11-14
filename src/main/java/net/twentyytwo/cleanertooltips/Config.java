package net.twentyytwo.cleanertooltips;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue MOD_ENABLED;
    public static final ModConfigSpec.BooleanValue ADD_SHARPNESS;

    static {
        MOD_ENABLED = BUILDER
                .comment("Should the cleaner tooltips be enabled?")
                .define("modEnabled", true);

        ADD_SHARPNESS = BUILDER
                .comment("Should the sharpness enchantment be included in the attribute value?")
                .define("addSharpness", true);
    }

    static final ModConfigSpec SPEC = BUILDER.build();
}