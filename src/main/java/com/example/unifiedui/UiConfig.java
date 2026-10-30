package com.example.unifiedui;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public final class UiConfig {
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final Client CLIENT;

    static {
        Pair<Client, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(Client::new);
        CLIENT = pair.getLeft();
        CLIENT_SPEC = pair.getRight();
    }

    private UiConfig() {}

    public static final class Client {
        public final ForgeConfigSpec.BooleanValue hudEnabled;
        public final ForgeConfigSpec.BooleanValue moveBarsRight;
        public final ForgeConfigSpec.BooleanValue hidePotionIcons;
        public final ForgeConfigSpec.IntValue ringRadius;
        public final ForgeConfigSpec.DoubleValue centerScale;
        public final ForgeConfigSpec.DoubleValue scrollSpeed;

        Client(ForgeConfigSpec.Builder b) {
            b.push("hud");
            hudEnabled = b.comment("Replace the vanilla hotbar with the ring hotbar.")
                    .define("hudEnabled", true);
            moveBarsRight = b.comment("Move health, armor, hunger and air bubbles to the right side.")
                    .define("moveBarsRight", true);
            ringRadius = b.comment("Radius of the hotbar ring in pixels.")
                    .defineInRange("ringRadius", 36, 20, 80);
            centerScale = b.comment("Scale of the selected item in the centre.")
                    .defineInRange("centerScale", 1.8D, 1.0D, 3.0D);
            scrollSpeed = b.comment("How fast the ring rotates when scrolling. Higher is faster.")
                    .defineInRange("scrollSpeed", 14.0D, 1.0D, 40.0D);
            b.pop();

            b.push("effects");
            hidePotionIcons = b.comment("Hide active potion effect icons (HUD and inventory).")
                    .define("hidePotionIcons", true);
            b.pop();
        }
    }
}
