package com.infrastructuresickos.torch_burnout;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(TorchBurnout.MOD_ID)
public class TorchBurnout {
    public static final String MOD_ID = "torch_burnout";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public TorchBurnout() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, TBConfig.SPEC);
        MinecraftForge.EVENT_BUS.register(new TorchBurnoutEventHandler());
        LOGGER.info("TorchBurnout initialized");
    }
}
