package com.example.unifiedui;

import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(UnifiedUiMod.MOD_ID)
public class UnifiedUiMod {
    public static final String MOD_ID = "unifiedui";

    public UnifiedUiMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, UiConfig.CLIENT_SPEC);
    }
}
