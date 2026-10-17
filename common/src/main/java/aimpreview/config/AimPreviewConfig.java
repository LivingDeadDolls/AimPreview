package aimpreview.config;

import aimpreview.Constants;
import aimpreview.platform.Services;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.ColorControllerBuilder;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.FloatSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import java.awt.Color;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Settings saved to config/aimpreview.json5. Every feature has a switch here. */
public final class AimPreviewConfig {
    public static final ConfigClassHandler<AimPreviewConfig> HANDLER = ConfigClassHandler.createBuilder(AimPreviewConfig.class)
            .id(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "config"))
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(Services.PLATFORM.getConfigDir().resolve(Constants.MOD_ID + ".json5"))
                    .setJson5(true)
                    .build())
            .build();

    /** When the preview of the item in hand is shown. */
    public enum Timing {
        /** Bow and trident while drawn, crossbow while loaded, thrown items while held. */
        AIMING,
        /** Whenever a supported item is held; an undrawn bow shows the fully drawn path. */
        HOLDING
    }

    public enum LineStyle { SOLID, DASHED, CUBES }

    /** Which projectiles already in the air get their path drawn. */
    public enum FlyingMode { OFF, OWN, ALL }

    public enum HitSound { DING, BELL, CHIME, PLING, CLICK }

    // General
    @SerialEntry public boolean enabled = true;
    @SerialEntry public Timing timing = Timing.AIMING;
    @SerialEntry public boolean throughWalls = false;
    @SerialEntry public boolean hideWithHud = true;
    @SerialEntry public int maxTicks = 200;
    @SerialEntry public float startDistance = 1.0f;
    @SerialEntry public boolean steadyLine = true;

    // Line
    @SerialEntry public LineStyle lineStyle = LineStyle.SOLID;
    @SerialEntry public float lineWidth = 2.5f;
    @SerialEntry public Color lineColor = new Color(255, 255, 255, 220);
    @SerialEntry public boolean colorOnHit = true;
    @SerialEntry public Color hitColor = new Color(255, 64, 64, 230);
    @SerialEntry public float dashLength = 0.5f;
    @SerialEntry public float cubeSize = 0.08f;
    @SerialEntry public float cubeSpacing = 0.5f;

    // Landing marker
    @SerialEntry public boolean marker = true;
    @SerialEntry public Color markerColor = new Color(255, 255, 255, 110);
    @SerialEntry public float markerSize = 0.5f;

    // Spread area
    @SerialEntry public boolean spread = true;
    @SerialEntry public float spreadScale = 1.0f;
    @SerialEntry public Color spreadColor = new Color(255, 200, 60, 70);

    // Target highlight
    @SerialEntry public boolean highlight = true;
    @SerialEntry public Color highlightColor = new Color(255, 64, 64, 230);

    // Hit sound
    @SerialEntry public boolean sound = true;
    @SerialEntry public HitSound soundType = HitSound.DING;
    @SerialEntry public float soundVolume = 0.6f;
    @SerialEntry public float soundPitch = 1.4f;

    // Distance HUD
    @SerialEntry public boolean distanceHud = true;
    @SerialEntry public int hudOffsetX = 8;
    @SerialEntry public int hudOffsetY = 4;
    @SerialEntry public Color hudColor = new Color(255, 255, 255);
    @SerialEntry public boolean hudShadow = true;
    @SerialEntry public int hudDecimals = 1;

    // Projectiles in flight
    @SerialEntry public FlyingMode flyingMode = FlyingMode.ALL;
    @SerialEntry public int flyingMaxCount = 8;
    @SerialEntry public int flyingRange = 64;
    @SerialEntry public Color flyingColor = new Color(90, 200, 255, 200);
    @SerialEntry public boolean flyingMarker = true;
    @SerialEntry public boolean flyingHighlight = true;

    // Items
    @SerialEntry public boolean bow = true;
    @SerialEntry public boolean crossbow = true;
    @SerialEntry public boolean crossbowFirework = true;
    @SerialEntry public boolean multishotAll = true;
    @SerialEntry public boolean trident = true;
    @SerialEntry public boolean snowball = true;
    @SerialEntry public boolean egg = true;
    @SerialEntry public boolean enderPearl = true;
    @SerialEntry public boolean splashPotion = true;
    @SerialEntry public boolean lingeringPotion = true;
    @SerialEntry public boolean experienceBottle = true;
    @SerialEntry public boolean windCharge = true;
    @SerialEntry public boolean fishingRod = true;

    public static AimPreviewConfig get() {
        return HANDLER.instance();
    }

    public static Screen createScreen(Screen parent) {
        return YetAnotherConfigLib.create(HANDLER, (defaults, config, builder) -> builder
                .title(Component.translatable("aimpreview.config.title"))
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("aimpreview.config.category.general"))
                        .option(bool("enabled", defaults.enabled, () -> config.enabled, v -> config.enabled = v))
                        .option(Option.<Timing>createBuilder()
                                .name(Component.translatable("aimpreview.config.timing"))
                                .description(desc("timing"))
                                .binding(defaults.timing, () -> config.timing, v -> config.timing = v)
                                .controller(o -> EnumControllerBuilder.create(o).enumClass(Timing.class)
                                        .formatValue(v -> Component.translatable("aimpreview.config.timing." + v.name().toLowerCase())))
                                .build())
                        .option(bool("throughWalls", defaults.throughWalls, () -> config.throughWalls, v -> config.throughWalls = v))
                        .option(bool("hideWithHud", defaults.hideWithHud, () -> config.hideWithHud, v -> config.hideWithHud = v))
                        .option(intSlider("maxTicks", 20, 600, 10, defaults.maxTicks, () -> config.maxTicks, v -> config.maxTicks = v))
                        .option(floatSlider("startDistance", 0f, 5f, 0.25f, defaults.startDistance, () -> config.startDistance, v -> config.startDistance = v))
                        .option(bool("steadyLine", defaults.steadyLine, () -> config.steadyLine, v -> config.steadyLine = v))
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("aimpreview.config.category.look"))
                        .group(OptionGroup.createBuilder()
                                .name(Component.translatable("aimpreview.config.group.line"))
                                .option(Option.<LineStyle>createBuilder()
                                        .name(Component.translatable("aimpreview.config.lineStyle"))
                                        .description(desc("lineStyle"))
                                        .binding(defaults.lineStyle, () -> config.lineStyle, v -> config.lineStyle = v)
                                        .controller(o -> EnumControllerBuilder.create(o).enumClass(LineStyle.class)
                                                .formatValue(v -> Component.translatable("aimpreview.config.lineStyle." + v.name().toLowerCase())))
                                        .build())
                                .option(floatSlider("lineWidth", 0.5f, 10f, 0.5f, defaults.lineWidth, () -> config.lineWidth, v -> config.lineWidth = v))
                                .option(color("lineColor", defaults.lineColor, () -> config.lineColor, v -> config.lineColor = v))
                                .option(bool("colorOnHit", defaults.colorOnHit, () -> config.colorOnHit, v -> config.colorOnHit = v))
                                .option(color("hitColor", defaults.hitColor, () -> config.hitColor, v -> config.hitColor = v))
                                .option(floatSlider("dashLength", 0.1f, 2f, 0.1f, defaults.dashLength, () -> config.dashLength, v -> config.dashLength = v))
                                .option(floatSlider("cubeSize", 0.02f, 0.5f, 0.02f, defaults.cubeSize, () -> config.cubeSize, v -> config.cubeSize = v))
                                .option(floatSlider("cubeSpacing", 0.1f, 2f, 0.1f, defaults.cubeSpacing, () -> config.cubeSpacing, v -> config.cubeSpacing = v))
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Component.translatable("aimpreview.config.group.spread"))
                                .option(bool("spread", defaults.spread, () -> config.spread, v -> config.spread = v))
                                .option(floatSlider("spreadScale", 0.1f, 1f, 0.05f, defaults.spreadScale, () -> config.spreadScale, v -> config.spreadScale = v))
                                .option(color("spreadColor", defaults.spreadColor, () -> config.spreadColor, v -> config.spreadColor = v))
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Component.translatable("aimpreview.config.group.marker"))
                                .option(bool("marker", defaults.marker, () -> config.marker, v -> config.marker = v))
                                .option(color("markerColor", defaults.markerColor, () -> config.markerColor, v -> config.markerColor = v))
                                .option(floatSlider("markerSize", 0.1f, 2f, 0.1f, defaults.markerSize, () -> config.markerSize, v -> config.markerSize = v))
                                .option(bool("highlight", defaults.highlight, () -> config.highlight, v -> config.highlight = v))
                                .option(color("highlightColor", defaults.highlightColor, () -> config.highlightColor, v -> config.highlightColor = v))
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("aimpreview.config.category.hints"))
                        .group(OptionGroup.createBuilder()
                                .name(Component.translatable("aimpreview.config.group.sound"))
                                .option(bool("sound", defaults.sound, () -> config.sound, v -> config.sound = v))
                                .option(Option.<HitSound>createBuilder()
                                        .name(Component.translatable("aimpreview.config.soundType"))
                                        .description(desc("soundType"))
                                        .binding(defaults.soundType, () -> config.soundType, v -> config.soundType = v)
                                        .controller(o -> EnumControllerBuilder.create(o).enumClass(HitSound.class)
                                                .formatValue(v -> Component.translatable("aimpreview.config.soundType." + v.name().toLowerCase())))
                                        .build())
                                .option(floatSlider("soundVolume", 0f, 1f, 0.05f, defaults.soundVolume, () -> config.soundVolume, v -> config.soundVolume = v))
                                .option(floatSlider("soundPitch", 0.5f, 2f, 0.05f, defaults.soundPitch, () -> config.soundPitch, v -> config.soundPitch = v))
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Component.translatable("aimpreview.config.group.hud"))
                                .option(bool("distanceHud", defaults.distanceHud, () -> config.distanceHud, v -> config.distanceHud = v))
                                .option(intSlider("hudOffsetX", -100, 100, 1, defaults.hudOffsetX, () -> config.hudOffsetX, v -> config.hudOffsetX = v))
                                .option(intSlider("hudOffsetY", -100, 100, 1, defaults.hudOffsetY, () -> config.hudOffsetY, v -> config.hudOffsetY = v))
                                .option(color("hudColor", defaults.hudColor, () -> config.hudColor, v -> config.hudColor = v))
                                .option(bool("hudShadow", defaults.hudShadow, () -> config.hudShadow, v -> config.hudShadow = v))
                                .option(intSlider("hudDecimals", 0, 2, 1, defaults.hudDecimals, () -> config.hudDecimals, v -> config.hudDecimals = v))
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Component.translatable("aimpreview.config.group.flying"))
                                .option(Option.<FlyingMode>createBuilder()
                                        .name(Component.translatable("aimpreview.config.flyingMode"))
                                        .description(desc("flyingMode"))
                                        .binding(defaults.flyingMode, () -> config.flyingMode, v -> config.flyingMode = v)
                                        .controller(o -> EnumControllerBuilder.create(o).enumClass(FlyingMode.class)
                                                .formatValue(v -> Component.translatable("aimpreview.config.flyingMode." + v.name().toLowerCase())))
                                        .build())
                                .option(intSlider("flyingMaxCount", 1, 32, 1, defaults.flyingMaxCount, () -> config.flyingMaxCount, v -> config.flyingMaxCount = v))
                                .option(intSlider("flyingRange", 8, 256, 8, defaults.flyingRange, () -> config.flyingRange, v -> config.flyingRange = v))
                                .option(color("flyingColor", defaults.flyingColor, () -> config.flyingColor, v -> config.flyingColor = v))
                                .option(bool("flyingMarker", defaults.flyingMarker, () -> config.flyingMarker, v -> config.flyingMarker = v))
                                .option(bool("flyingHighlight", defaults.flyingHighlight, () -> config.flyingHighlight, v -> config.flyingHighlight = v))
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("aimpreview.config.category.items"))
                        .option(bool("bow", defaults.bow, () -> config.bow, v -> config.bow = v))
                        .option(bool("crossbow", defaults.crossbow, () -> config.crossbow, v -> config.crossbow = v))
                        .option(bool("crossbowFirework", defaults.crossbowFirework, () -> config.crossbowFirework, v -> config.crossbowFirework = v))
                        .option(bool("multishotAll", defaults.multishotAll, () -> config.multishotAll, v -> config.multishotAll = v))
                        .option(bool("trident", defaults.trident, () -> config.trident, v -> config.trident = v))
                        .option(bool("snowball", defaults.snowball, () -> config.snowball, v -> config.snowball = v))
                        .option(bool("egg", defaults.egg, () -> config.egg, v -> config.egg = v))
                        .option(bool("enderPearl", defaults.enderPearl, () -> config.enderPearl, v -> config.enderPearl = v))
                        .option(bool("splashPotion", defaults.splashPotion, () -> config.splashPotion, v -> config.splashPotion = v))
                        .option(bool("lingeringPotion", defaults.lingeringPotion, () -> config.lingeringPotion, v -> config.lingeringPotion = v))
                        .option(bool("experienceBottle", defaults.experienceBottle, () -> config.experienceBottle, v -> config.experienceBottle = v))
                        .option(bool("windCharge", defaults.windCharge, () -> config.windCharge, v -> config.windCharge = v))
                        .option(bool("fishingRod", defaults.fishingRod, () -> config.fishingRod, v -> config.fishingRod = v))
                        .build()))
                .generateScreen(parent);
    }

    /** The picture shown under each option's description: what the option changes, recorded in game. */
    private static final Map<String, String> PICTURES = Map.ofEntries(
            Map.entry("throughWalls", "through-walls.png"),
            Map.entry("steadyLine", "walking.webp"),
            Map.entry("lineStyle", "styles.webp"), Map.entry("lineWidth", "styles.webp"), Map.entry("lineColor", "styles.webp"),
            Map.entry("dashLength", "styles.webp"), Map.entry("cubeSize", "styles.webp"), Map.entry("cubeSpacing", "styles.webp"),
            Map.entry("colorOnHit", "bow.webp"), Map.entry("hitColor", "bow.webp"),
            Map.entry("highlight", "bow.webp"), Map.entry("highlightColor", "bow.webp"),
            Map.entry("sound", "bow.webp"), Map.entry("soundType", "bow.webp"), Map.entry("bow", "bow.webp"),
            Map.entry("marker", "snowball.webp"), Map.entry("markerColor", "snowball.webp"), Map.entry("markerSize", "snowball.webp"),
            Map.entry("timing", "snowball.webp"), Map.entry("snowball", "snowball.webp"),
            Map.entry("spread", "spread.png"), Map.entry("spreadScale", "spread.png"), Map.entry("spreadColor", "spread.png"),
            Map.entry("distanceHud", "pearl.webp"), Map.entry("hudOffsetX", "pearl.webp"), Map.entry("hudOffsetY", "pearl.webp"),
            Map.entry("hudColor", "pearl.webp"), Map.entry("hudShadow", "pearl.webp"), Map.entry("hudDecimals", "pearl.webp"),
            Map.entry("enderPearl", "pearl.webp"),
            Map.entry("flyingMode", "flying.webp"), Map.entry("flyingMaxCount", "flying.webp"), Map.entry("flyingRange", "flying.webp"),
            Map.entry("flyingColor", "flying.webp"), Map.entry("flyingMarker", "flying.webp"), Map.entry("flyingHighlight", "flying.webp"),
            Map.entry("crossbow", "multishot.webp"), Map.entry("multishotAll", "multishot.webp"));

    private static OptionDescription desc(String key) {
        OptionDescription.Builder description = OptionDescription.createBuilder()
                .text(Component.translatable("aimpreview.config." + key + ".desc"));
        String picture = PICTURES.get(key);
        if (picture != null) {
            Identifier id = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/config/" + picture);
            if (picture.endsWith(".webp")) description.webpImage(id);
            else description.image(id, 320, 180);
        }
        return description.build();
    }

    private static Option<Boolean> bool(String key, boolean def, Supplier<Boolean> get, Consumer<Boolean> set) {
        return Option.<Boolean>createBuilder()
                .name(Component.translatable("aimpreview.config." + key))
                .description(desc(key))
                .binding(def, get, set)
                .controller(TickBoxControllerBuilder::create)
                .build();
    }

    private static Option<Integer> intSlider(String key, int min, int max, int step, int def, Supplier<Integer> get, Consumer<Integer> set) {
        return Option.<Integer>createBuilder()
                .name(Component.translatable("aimpreview.config." + key))
                .description(desc(key))
                .binding(def, get, set)
                .controller(o -> IntegerSliderControllerBuilder.create(o).range(min, max).step(step))
                .build();
    }

    private static Option<Float> floatSlider(String key, float min, float max, float step, float def, Supplier<Float> get, Consumer<Float> set) {
        return Option.<Float>createBuilder()
                .name(Component.translatable("aimpreview.config." + key))
                .description(desc(key))
                .binding(def, get, set)
                .controller(o -> FloatSliderControllerBuilder.create(o).range(min, max).step(step))
                .build();
    }

    private static Option<Color> color(String key, Color def, Supplier<Color> get, Consumer<Color> set) {
        return Option.<Color>createBuilder()
                .name(Component.translatable("aimpreview.config." + key))
                .description(desc(key))
                .binding(def, get, set)
                .controller(o -> ColorControllerBuilder.create(o).allowAlpha(true))
                .build();
    }
}
