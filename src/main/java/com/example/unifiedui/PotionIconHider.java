package com.example.unifiedui;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Hides active-effect icons so players cannot see which effects they have. */
@Mod.EventBusSubscriber(modid = UnifiedUiMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class PotionIconHider {
    private PotionIconHider() {}

    @SubscribeEvent
    public static void onOverlayPre(RenderGuiOverlayEvent.Pre event) {
        if (UiConfig.CLIENT.hidePotionIcons.get()
                && event.getOverlay().id().equals(VanillaGuiOverlay.POTION_ICONS.id())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onInventoryEffects(ScreenEvent.RenderInventoryMobEffects event) {
        if (UiConfig.CLIENT.hidePotionIcons.get()) {
            event.setCanceled(true);
        }
    }
}
