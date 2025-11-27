package net.twentyytwo.cleanertooltips;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue MOD_ENABLED;
    public static final ModConfigSpec.BooleanValue ADD_SHARPNESS;
    public static final ModConfigSpec.BooleanValue DURABILITY;
    public static final ModConfigSpec.EnumValue<POS_VALUES> DURABILITY_POS;

    public enum POS_VALUES {
        DEFAULT,
        INLINE
    }

    static {
        MOD_ENABLED = BUILDER
                .comment("Whether Cleaner Tooltips should be enabled.")
                .define("enabled", true);

        ADD_SHARPNESS = BUILDER
                .comment("Should sharpness be included when calculating the attack damage?")
                .define("sharpness", true);

        DURABILITY = BUILDER
                .comment("Should durability be included in the tooltip?")
                .define("durability", false);

        DURABILITY_POS = BUILDER // Not yet implemented
                .comment("Set to DEFAULT to display the durability at the bottom of the tooltip.\nSet to INLINE to display the durability next to the attributes.")
                .defineEnum("durability_pos", POS_VALUES.DEFAULT);
    }

    static final ModConfigSpec SPEC = BUILDER.build();
}