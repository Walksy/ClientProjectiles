package walksy.clientprojectiles.config;

import main.walksy.lib.api.WalksyLibApi;
import main.walksy.lib.api.config.Config;
import main.walksy.lib.api.config.option.Category;
import main.walksy.lib.api.config.option.OptionGroup;
import main.walksy.lib.api.config.option.Options;
import main.walksy.lib.api.config.PathUtils;

public final class ClientProjectilesConfig implements WalksyLibApi {
    public static boolean modEnabled = true;
    public static boolean enderPearls = true;
    public static boolean windCharge = true;
    public static boolean splashPotions = true;
    public static boolean lingeringPotions = true;
    public static boolean expBottles = true;
    public static boolean bow = true;
    public static boolean crossbow = true;
    public static boolean pearlWindChargeCollision = true;
    @Override
    public Config define() {
        return Config.builder()
            .path(PathUtils.ofConfigDir("clientprojectiles"))
            .category(Category.builder("General")
                .group(OptionGroup.builder("General")
                    .option(Options.ofBoolean("Mod Enabled", () -> modEnabled, true, value -> modEnabled = value).build())
                    .build())
                .group(OptionGroup.builder("Throwables")
                    .option(Options.ofBoolean("Ender Pearl", () -> enderPearls, true, value -> enderPearls = value).build())
                    .option(Options.ofBoolean("Wind Charge", () -> windCharge, true, value -> windCharge = value).build())
                    .option(Options.ofBoolean("Splash Potions", () -> splashPotions, true, value -> splashPotions = value).build())
                    .option(Options.ofBoolean("Lingering Potions", () -> lingeringPotions, true, value -> lingeringPotions = value).build())
                    .option(Options.ofBoolean("Experience Bottles", () -> expBottles, true, value -> expBottles = value).build())
                    .build())
                .group(OptionGroup.builder("Bows")
                    .option(Options.ofBoolean("Bow", () -> bow, true, value -> bow = value).build())
                    .option(Options.ofBoolean("Crossbow", () -> crossbow, true, value -> crossbow = value).build())
                    .build())
                .group(OptionGroup.builder("Interactions")
                    .option(Options.ofBoolean("Pearl And Wind Charge Collision", () -> pearlWindChargeCollision, true, value -> pearlWindChargeCollision = value)
                        .saveKey("Pearl and Wind Charge Collision")
                        .description("Your pearls and wind charges can hit each other client side")
                        .build())
                    .build())
                .build())
            .build();
    }
}
