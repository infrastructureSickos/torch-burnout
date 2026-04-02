package com.infrastructuresickos.torch_burnout;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class TBConfig {
    public static final ForgeConfigSpec SPEC;
    public static final TBConfig INSTANCE;

    static {
        Pair<TBConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(TBConfig::new);
        INSTANCE = specPair.getLeft();
        SPEC = specPair.getRight();
    }

    /** Base chance per random tick that a torch burns out (0.0–1.0). */
    public final ForgeConfigSpec.DoubleValue baseChance;
    /** Multiplier applied to baseChance when it's raining and torch has sky access. */
    public final ForgeConfigSpec.DoubleValue rainMultiplier;
    /** Multiplier applied to baseChance per Enderman within endermanRadius blocks. */
    public final ForgeConfigSpec.DoubleValue endermanMultiplier;
    /** Search radius for Endermen near a torch. */
    public final ForgeConfigSpec.IntValue endermanRadius;

    private TBConfig(ForgeConfigSpec.Builder builder) {
        builder.push("burnout");
        baseChance = builder
                .comment("Base chance per random tick that a torch burns out (0.0–1.0, default 0.005)")
                .defineInRange("baseChance", 0.005, 0.0, 1.0);
        rainMultiplier = builder
                .comment("Chance multiplier when raining and torch can see sky (default 10.0)")
                .defineInRange("rainMultiplier", 10.0, 1.0, 100.0);
        endermanMultiplier = builder
                .comment("Additional chance multiplier per nearby Enderman (default 5.0)")
                .defineInRange("endermanMultiplier", 5.0, 1.0, 100.0);
        endermanRadius = builder
                .comment("Block radius to scan for Endermen near a torch (default 8)")
                .defineInRange("endermanRadius", 8, 1, 32);
        builder.pop();
    }
}
