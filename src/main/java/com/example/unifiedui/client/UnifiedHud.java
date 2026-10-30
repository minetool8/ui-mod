package com.example.unifiedui.client;

import com.example.unifiedui.UiConfig;
import com.example.unifiedui.UnifiedUiMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Ring hotbar on the left (selected item large in the centre, the other eight on a ring that
 * rotates smoothly), vanilla health/armor/food/air moved to the right. Selection is still
 * vanilla's, so scrolling and number keys behave as before.
 */
@Mod.EventBusSubscriber(modid = UnifiedUiMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class UnifiedHud {
    private static final ResourceLocation HOTBAR = VanillaGuiOverlay.HOTBAR.id();
    private static final ResourceLocation HEALTH = VanillaGuiOverlay.PLAYER_HEALTH.id();
    private static final ResourceLocation ARMOR = VanillaGuiOverlay.ARMOR_LEVEL.id();
    private static final ResourceLocation FOOD = VanillaGuiOverlay.FOOD_LEVEL.id();
    private static final ResourceLocation AIR = VanillaGuiOverlay.AIR_LEVEL.id();

    private static float visual = -1f;
    private static long lastNanos = 0L;
    private static ResourceLocation pushedFor = null;

    private UnifiedHud() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onOverlayPre(RenderGuiOverlayEvent.Pre event) {
        if (!UiConfig.CLIENT.hudEnabled.get()) return;
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        ResourceLocation id = event.getOverlay().id();
        GuiGraphics g = event.getGuiGraphics();
        int w = event.getWindow().getGuiScaledWidth();
        int h = event.getWindow().getGuiScaledHeight();

        if (id.equals(HOTBAR)) {
            if (player.isSpectator()) return; // keep the spectator menu working
            event.setCanceled(true);
            drawRing(g, mc, player, w, h);
            return;
        }

        if (!UiConfig.CLIENT.moveBarsRight.get()) return;

        int dx;
        int dy = 0;
        if (id.equals(HEALTH) || id.equals(ARMOR)) {
            dx = w / 2 + 2;            // left edge -> right edge of the screen
        } else if (id.equals(FOOD)) {
            dx = w / 2 - 99;
            dy = -20;                  // stack above hearts/armor
        } else if (id.equals(AIR)) {
            dx = w / 2 - 99;
            dy = -20;
        } else {
            return;
        }
        g.pose().pushPose();
        g.pose().translate((float) dx, (float) dy, 0f);
        pushedFor = id;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onOverlayPost(RenderGuiOverlayEvent.Post event) {
        if (pushedFor != null && pushedFor.equals(event.getOverlay().id())) {
            event.getGuiGraphics().pose().popPose();
            pushedFor = null;
        }
    }

    private static float wrap(float x) {
        return ((x + 4.5f) % 9f + 9f) % 9f - 4.5f;
    }

    private static void drawRing(GuiGraphics g, Minecraft mc, Player player, int w, int h) {
        int sel = player.getInventory().selected;

        // Smooth rotation toward the selected slot (shortest way round).
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

        // Draw far items first so the selected one ends up on top.
        Integer[] order = {0, 1, 2, 3, 4, 5, 6, 7, 8};
        final float vis = visual;
        java.util.Arrays.sort(order, (a, b) -> Float.compare(Math.abs(wrap(b - vis)), Math.abs(wrap(a - vis))));

        for (int i : order) {
            float d = wrap(i - vis);
            float t = Math.min(1f, Math.abs(d));
            float ease = t * t * (3f - 2f * t);
            double ang = Math.toRadians(180.0 + d * 40.0); // gap at the bottom of the ring
            float r = radius * ease;
            float x = cx + r * (float) Math.sin(ang);
            float y = cy - r * (float) Math.cos(ang);
            float scale = centerScale + (ringScale - centerScale) * ease;

            ItemStack stack = player.getInventory().getItem(i);
            int half = Math.round(8f * scale) + 3;

            // Backing plate + outline fades out as the slot leaves the centre.
            int plateAlpha = (int) (0x66 * (1f - ease));
            if (plateAlpha > 4) {
                int px = Math.round(x), py = Math.round(y);
                g.fill(px - half, py - half, px + half, py + half, plateAlpha << 24);
                int outline = ((int) (0xDD * (1f - ease)) << 24) | 0xFFFFFF;
                g.renderOutline(px - half, py - half, half * 2, half * 2, outline);
            }

            if (stack.isEmpty()) {
                int px = Math.round(x), py = Math.round(y);
                g.fill(px - 1, py - 1, px + 1, py + 1, 0x88FFFFFF);
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
