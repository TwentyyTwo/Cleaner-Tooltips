package net.twentyytwo.cleanertooltips;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue MOD_ENABLED;
    public static final ModConfigSpec.BooleanValue ADD_SHARPNESS;

    static {
        MOD_ENABLED = BUILDER
                .comment("Whether Cleaner Tooltips should be enabled.")
                .define("enabled", true);

        ADD_SHARPNESS = BUILDER
                .comment("Should sharpness be included when calculating the attack damage?")
                .define("sharpness", true);
    }

    static final ModConfigSpec SPEC = BUILDER.build();
}