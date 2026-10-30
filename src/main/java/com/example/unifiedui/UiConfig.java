package com.example.unifiedui;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.LinkedHashMap;
import java.util.Map;

public final class UiConfig {
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final Client CLIENT;

    static {
        Pair<Client, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(Client::new);
        CLIENT = pair.getLeft();
        CLIENT_SPEC = pair.getRight();
    }

    private UiConfig() {}

    /** Bar keys and their default extra Y offset (negative = up). */
    private static final Object[][] BARS = {
            {"health", -40}, {"armor", -40}, {"lso_health_overhaul", -40}, {"lso_body_damage", -40},
            {"food", 0}, {"lso_cold_hunger", 0}, {"lso_thirst", 0}, {"lso_temperature", 0},
            {"lso_wetness", 0}, {"air", -20}
    };

    public static final class Client {
        public final ForgeConfigSpec.BooleanValue hudEnabled;
        public final ForgeConfigSpec.BooleanValue moveBarsRight;
        public final ForgeConfigSpec.BooleanValue hidePotionIcons;
        public final ForgeConfigSpec.IntValue ringRadius;
        public final ForgeConfigSpec.DoubleValue centerScale;
        public final ForgeConfigSpec.DoubleValue scrollSpeed;
        public final Map<String, ForgeConfigSpec.IntValue[]> barOffsets = new LinkedHashMap<>();

        Client(ForgeConfigSpec.Builder b) {
            b.push("hud");
            hudEnabled = b.comment("Replace the vanilla hotbar with the ring hotbar.")
                    .define("hudEnabled", true);
            moveBarsRight = b.comment("Move health, hunger, thirst, temperature and other bars to the right side.")
                    .define("moveBarsRight", true);
            ringRadius = b.comment("Radius of the hotbar ring in pixels.")
                    .defineInRange("ringRadius", 36, 20, 80);
            centerScale = b.comment("Scale of the selected item in the centre.")
                    .defineInRange("centerScale", 1.8D, 1.0D, 3.0D);
            scrollSpeed = b.comment("How fast the ring rotates when scrolling. Higher is faster.")
                    .defineInRange("scrollSpeed", 14.0D, 1.0D, 40.0D);
            b.pop();

            b.push("barOffsets");
            b.comment("Extra X/Y pixel offset added to each bar after it is moved to the right side.",
                    "Use these to fine-tune spacing. Negative Y moves a bar up, negative X moves it left.");
            for (Object[] bar : BARS) {
                String key = (String) bar[0];
                int dy = (Integer) bar[1];
                ForgeConfigSpec.IntValue x = b.defineInRange(key + "_x", 0, -2000, 2000);
                ForgeConfigSpec.IntValue y = b.defineInRange(key + "_y", dy, -2000, 2000);
                barOffsets.put(key, new ForgeConfigSpec.IntValue[]{x, y});
            }
            b.pop();

            b.push("effects");
            hidePotionIcons = b.comment("Hide all active potion effect icons (HUD and inventory) and effect tints on the bars.")
                    .define("hidePotionIcons", true);
            b.pop();
        }
    }
}
