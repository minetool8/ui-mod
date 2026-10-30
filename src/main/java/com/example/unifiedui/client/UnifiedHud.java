package com.example.unifiedui.client;

import com.example.unifiedui.UiConfig;
import com.example.unifiedui.UnifiedUiMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

/**
 * Ring hotbar on the left (selected item large in the centre, eight items evenly spaced on a ring
 * that rotates smoothly when you scroll). Vanilla and Legendary Survival Overhaul bars are shifted
 * to the right side. Slot selection is vanilla's, so scrolling and number keys are unchanged.
 */
@Mod.EventBusSubscriber(modid = UnifiedUiMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class UnifiedHud {
    private static final String LSO = "legendarysurvivaloverhaul";
    private static final ResourceLocation HOTBAR = VanillaGuiOverlay.HOTBAR.id();

    /** key -> overlay id, and which side of the screen the overlay is anchored to originally. */
    private record Bar(String key, boolean leftAnchored) {}
    private static final Map<ResourceLocation, Bar> BARS = new HashMap<>();
    static {
        BARS.put(VanillaGuiOverlay.PLAYER_HEALTH.id(), new Bar("health", true));
        BARS.put(VanillaGuiOverlay.ARMOR_LEVEL.id(), new Bar("armor", true));
        BARS.put(VanillaGuiOverlay.FOOD_LEVEL.id(), new Bar("food", false));
        BARS.put(VanillaGuiOverlay.AIR_LEVEL.id(), new Bar("air", false));
        BARS.put(new ResourceLocation(LSO, "health_overhaul"), new Bar("lso_health_overhaul", true));
        BARS.put(new ResourceLocation(LSO, "body_damage"), new Bar("lso_body_damage", true));
        BARS.put(new ResourceLocation(LSO, "cold_hunger"), new Bar("lso_cold_hunger", false));
        BARS.put(new ResourceLocation(LSO, "thirst"), new Bar("lso_thirst", false));
        BARS.put(new ResourceLocation(LSO, "temperature"), new Bar("lso_temperature", false));
        BARS.put(new ResourceLocation(LSO, "wetness"), new Bar("lso_wetness", false));
    }

    /** Overlays that tint themselves based on active effects (poison, wither, regen, hunger). */
    private static final MobEffect[] TINT_EFFECTS = {
            MobEffects.POISON, MobEffects.WITHER, MobEffects.REGENERATION, MobEffects.HUNGER
    };
    private static final Map<MobEffect, MobEffectInstance> stash = new HashMap<>();
    private static boolean stripped = false;

    private static float visual = -1f;
    private static long lastNanos = 0L;
    private static ResourceLocation pushedFor = null;

    private UnifiedHud() {}

    // ---------------------------------------------------------------- overlays

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onOverlayPre(RenderGuiOverlayEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        ResourceLocation id = event.getOverlay().id();
        Bar bar = BARS.get(id);

        // Hide effect-based tinting (poison/wither hearts, hunger bar) so effects can't be guessed.
        if (bar != null && UiConfig.CLIENT.hidePotionIcons.get()
                && (bar.key().equals("health") || bar.key().equals("food")
                || bar.key().equals("lso_health_overhaul") || bar.key().equals("lso_cold_hunger"))) {
            stripTintEffects(player);
        }

        if (!UiConfig.CLIENT.hudEnabled.get()) return;

        GuiGraphics g = event.getGuiGraphics();
        int w = event.getWindow().getGuiScaledWidth();
        int h = event.getWindow().getGuiScaledHeight();

        if (id.equals(HOTBAR)) {
            if (player.isSpectator()) return; // keep the spectator menu working
            event.setCanceled(true);
            drawRing(g, mc, player, w, h);
            return;
        }

        if (bar == null || !UiConfig.CLIENT.moveBarsRight.get()) return;

        int dx = bar.leftAnchored() ? (w / 2 + 2) : (w / 2 - 99);
        int[] extra = extraOffset(bar.key());
        g.pose().pushPose();
        g.pose().translate((float) (dx + extra[0]), (float) extra[1], 0f);
        pushedFor = id;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onOverlayPost(RenderGuiOverlayEvent.Post event) {
        if (pushedFor != null && pushedFor.equals(event.getOverlay().id())) {
            event.getGuiGraphics().pose().popPose();
            pushedFor = null;
        }
        Player player = Minecraft.getInstance().player;
        if (player != null) restoreTintEffects(player);
    }

    /** Safety net: effects are always restored by the end of the frame. */
    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.END && stripped) {
            Player player = Minecraft.getInstance().player;
            if (player != null) restoreTintEffects(player);
            stash.clear();
            stripped = false;
        }
    }

    private static int[] extraOffset(String key) {
        var pair = UiConfig.CLIENT.barOffsets.get(key);
        return pair == null ? new int[]{0, 0} : new int[]{pair[0].get(), pair[1].get()};
    }

    private static void stripTintEffects(Player player) {
        if (stripped) return;
        Map<MobEffect, MobEffectInstance> active = player.getActiveEffectsMap();
        for (MobEffect effect : TINT_EFFECTS) {
            MobEffectInstance inst = active.remove(effect);
            if (inst != null) stash.put(effect, inst);
        }
        stripped = true;
    }

    private static void restoreTintEffects(Player player) {
        if (!stripped) return;
        player.getActiveEffectsMap().putAll(stash);
        stash.clear();
        stripped = false;
    }

    // ---------------------------------------------------------------- ring hotbar

    private static float wrap(float x) {
        return ((x + 4.5f) % 9f + 9f) % 9f - 4.5f;
    }

    private static void drawRing(GuiGraphics g, Minecraft mc, Player player, int w, int h) {
        int sel = player.getInventory().selected;

        // Smooth rotation toward the selected slot, always the short way round.
        long now = System.nanoTime();
        float dt = lastNanos == 0L ? 0f : Math.min((now - lastNanos) / 1.0e9f, 0.25f);
        lastNanos = now;
        if (visual < 0f) visual = sel;
        float delta = wrap(sel - visual);
        float f = 1f - (float) Math.exp(-UiConfig.CLIENT.scrollSpeed.get() * dt);
        visual = ((visual + delta * f) % 9f + 9f) % 9f;

        int radius = UiConfig.CLIENT.ringRadius.get();
        float centerScale = UiConfig.CLIENT.centerScale.get().floatValue();
        float ringScale = 0.95f;
        int margin = 6;
        int cx = margin + radius + 14;
        int cy = h - margin - radius - 14;

        final float vis = visual;
        Integer[] order = {0, 1, 2, 3, 4, 5, 6, 7, 8};
        // Far items first so the selected one is drawn on top.
        java.util.Arrays.sort(order, (a, b) -> Float.compare(Math.abs(wrap(b - vis)), Math.abs(wrap(a - vis))));

        for (int i : order) {
            float d = wrap(i - vis);
            float a = Math.abs(d);
            float t = Math.min(1f, a);
            float ease = t * t * (3f - 2f * t);

            // Eight items evenly spaced 45 degrees apart; the centre slot moves out smoothly.
            float angOff = a < 1f ? 22.5f * a : 22.5f + 45f * (a - 1f);
            double ang = Math.toRadians(180.0 + Math.signum(d) * angOff);
            float r = radius * ease;
            float x = cx + r * (float) Math.sin(ang);
            float y = cy - r * (float) Math.cos(ang);
            float scale = centerScale + (ringScale - centerScale) * ease;

            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) {
                int px = Math.round(x), py = Math.round(y);
                g.fill(px - 1, py - 1, px + 1, py + 1, 0x66FFFFFF);
                continue;
            }

            g.pose().pushPose();
            g.pose().translate(x, y, 0f);
            g.pose().scale(scale, scale, 1f);
            g.renderItem(stack, -8, -8);
            g.renderItemDecorations(mc.font, stack, -8, -8);
            g.pose().popPose();
        }
    }
}
