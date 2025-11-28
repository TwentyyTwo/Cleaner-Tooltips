package net.twentyytwo.cleanertooltips;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue MOD_ENABLED;
    public static final ModConfigSpec.BooleanValue ADD_SHARPNESS;
    public static final ModConfigSpec.BooleanValue DURABILITY;
    public static final ModConfigSpec.EnumValue<POS_VALUES> DURABILITY_POS;

    public enum POS_VALUES {
        INLINE,
        BOTTOM,
        BELOW
    }

    static {
        MOD_ENABLED = BUILDER
                .comment("Whether Cleaner Tooltips should be enabled.")
                .define("enabled", true);

        ADD_SHARPNESS = BUILDER
                .comment("Should sharpness change the damage value?")
                .define("sharpness", true);

        DURABILITY = BUILDER
                .comment("Should durability be displayed in the tooltip?")
                .define("durability", false);

        DURABILITY_POS = BUILDER // Not yet implemented
                .comment("Set to INLINE to display the durability next to the attributes.\nSet to BELOW to display the durability below the attributes.\nSet to BOTTOM to display the durability at the bottom of the tooltip.")
                .defineEnum("durability_pos", POS_VALUES.INLINE);
    }

    static final ModConfigSpec SPEC = BUILDER.build();
}