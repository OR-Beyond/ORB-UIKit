package dev.orbeyond.uikit.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.InputConstants;

import org.lwjgl.glfw.GLFW;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import dev.orbeyond.uikit.api.Category;
import dev.orbeyond.uikit.api.Option;
import dev.orbeyond.uikit.api.OptionBinding;
import dev.orbeyond.uikit.api.OptionGroup;
import dev.orbeyond.uikit.api.UikitConfig;

/**
 * Demo configuration for the ORB-UIKit example mod. Plain static mutable fields, persisted to a
 * Gson file at {@code config/orb-uikit-example.json} (Gson ships with Minecraft - no dependency).
 */
public final class ExampleConfig {

    public static boolean enabled = true;
    public static boolean fog = true;
    public static int range = 64;
    public static int renderSteps = 8;
    public static float detail = 0.5f;
    public static ViewMode viewMode = ViewMode.SMOOTH;
    public static String themeName = "Dark";
    public static String language = "English";
    public static int accentColor = 0xFFD479; // packed 0xRRGGBB
    public static InputConstants.Key toggleKey = InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_G);

    public enum ViewMode {
        SMOOTH("Smooth"),
        FLAT("Flat"),
        CULLED("Culled");

        public final Component display;

        ViewMode(String display) {
            this.display = Component.literal(display);
        }
    }

    private ExampleConfig() {
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("orb-uikit-example.json");
    }

    public static void save() {
        JsonObject root = new JsonObject();
        root.addProperty("enabled", enabled);
        root.addProperty("fog", fog);
        root.addProperty("range", range);
        root.addProperty("renderSteps", renderSteps);
        root.addProperty("detail", detail);
        root.addProperty("viewMode", viewMode.name());
        root.addProperty("themeName", themeName);
        root.addProperty("language", language);
        root.addProperty("accentColor", accentColor);
        root.addProperty("toggleKey", toggleKey.getName());
        try {
            Files.writeString(configPath(), root.toString());
        } catch (IOException e) {
            throw new RuntimeException("Failed to save ORB-UIKit example config", e);
        }
    }

    public static void load() {
        Path path = configPath();
        if (!Files.exists(path)) {
            return; // missing file: keep defaults
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            if (root.has("enabled")) enabled = root.get("enabled").getAsBoolean();
            if (root.has("fog")) fog = root.get("fog").getAsBoolean();
            if (root.has("range")) range = root.get("range").getAsInt();
            if (root.has("renderSteps")) renderSteps = root.get("renderSteps").getAsInt();
            if (root.has("detail")) detail = root.get("detail").getAsFloat();
            if (root.has("viewMode")) {
                try {
                    viewMode = ViewMode.valueOf(root.get("viewMode").getAsString());
                } catch (IllegalArgumentException ignored) {
                    // unknown value: keep current
                }
            }
            if (root.has("themeName")) themeName = root.get("themeName").getAsString();
            if (root.has("language")) language = root.get("language").getAsString();
            if (root.has("accentColor")) accentColor = root.get("accentColor").getAsInt();
            if (root.has("toggleKey")) toggleKey = InputConstants.getKey(root.get("toggleKey").getAsString());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load ORB-UIKit example config", e);
        }
    }

    public static Screen screen(Screen parent) {
        Category general = Category.builder(Component.literal("General"))
                .icon(Identifier.fromNamespaceAndPath("uikit", "textures/gui/brutal_button.png"))
                .group(OptionGroup.builder(Component.literal("Features"))
                        .option(Option.toggle(Component.literal("Enabled"),
                                new OptionBinding<>(() -> enabled, v -> enabled = v), () -> true))
                        .option(Option.intSlider(Component.literal("Range"), 16, 256,
                                new OptionBinding<>(() -> range, v -> range = v), () -> 64))
                        .option(Option.blockSlider(Component.literal("Render steps"), 1, 16,
                                new OptionBinding<>(() -> renderSteps, v -> renderSteps = v), () -> 8))
                        .option(Option.floatSlider(Component.literal("Detail"), 0.0f, 1.0f, 2,
                                new OptionBinding<>(() -> detail, v -> detail = v), () -> 0.5f))
                        .build())
                .group(OptionGroup.builder(Component.literal("View"))
                        .withToggle(new OptionBinding<>(() -> fog, v -> fog = v))
                        .option(Option.dropdownEnum(Component.literal("View mode"), ViewMode.class, m -> m.display,
                                new OptionBinding<>(() -> viewMode, v -> viewMode = v), () -> ViewMode.SMOOTH))
                        .option(Option.dropdownString(Component.literal("Theme"), List.of("Dark", "Light", "Auto"),
                                s -> Component.literal(s),
                                new OptionBinding<>(() -> themeName, v -> themeName = v), () -> "Dark"))
                        .build())
                .build();

        Category appearance = Category.builder(Component.literal("Appearance"))
                .group(OptionGroup.builder(Component.literal("Style"))
                        .option(Option.cyclingEnum(Component.literal("View cycler"), ViewMode.class, m -> m.display,
                                new OptionBinding<>(() -> viewMode, v -> viewMode = v), () -> ViewMode.SMOOTH))
                        .option(Option.cyclingString(Component.literal("Language"), List.of("English", "Deutsch", "日本語"),
                                s -> Component.literal(s),
                                new OptionBinding<>(() -> language, v -> language = v), () -> "English"))
                        .option(Option.color(Component.literal("Accent"),
                                new OptionBinding<>(() -> accentColor, v -> accentColor = v), () -> 0xFFD479))
                        .option(Option.keybind(Component.literal("Toggle key"),
                                new OptionBinding<>(() -> toggleKey, v -> toggleKey = v),
                                () -> InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_G)))
                        .build())
                .build();

        return UikitConfig.create(b -> b
                .title(Component.literal("ORB-UIKit Demo"))
                .category(general)
                .category(appearance)
                .saveConsumer(ExampleConfig::save)
                .cancelConsumer(ExampleConfig::load))
                .generateScreen(parent);
    }
}