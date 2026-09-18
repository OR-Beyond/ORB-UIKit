package dev.orbeyond.uikit.api;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import dev.orbeyond.uikit.screen.UikitConfigScreen;
import dev.orbeyond.uikit.theme.UikitTheme;

/**
 * The entry point for building a config screen, mirroring YACL's {@code YaclScreen} flow:
 *
 * <pre>{@code
 * UikitConfig config = UikitConfig.create(builder -> builder
 *         .title(Component.literal("My Mod"))
 *         .category(Category.builder(Component.literal("General"))
 *                 .group(OptionGroup.builder(Component.literal("Rendering"))
 *                         .option(Option.toggle(Component.literal("Enabled"),
 *                                 new OptionBinding<>(() -> MyConfig.enabled, v -> MyConfig.enabled = v),
 *                                 () -> true))
 *                         .build())
 *                 .build())
 *         .saveConsumer(() -> MyConfig.save())
 *         .cancelConsumer(() -> MyConfig.load()));
 *
 * Screen screen = config.generateScreen(parent);
 * }</pre>
 */
public final class UikitConfig {

    private final Component title;
    private final List<Category> categories;
    private final UikitTheme theme;
    private final Runnable saveConsumer;
    private final Runnable cancelConsumer;

    private UikitConfig(Component title, List<Category> categories, UikitTheme theme,
                        Runnable saveConsumer, Runnable cancelConsumer) {
        this.title = title;
        this.categories = categories;
        this.theme = theme;
        this.saveConsumer = saveConsumer;
        this.cancelConsumer = cancelConsumer;
    }

    public static UikitConfig create(Consumer<Builder> builderConsumer) {
        Builder builder = new Builder();
        builderConsumer.accept(builder);
        return builder.build();
    }

    /** Build the config screen, returning to {@code parent} on close. */
    public Screen generateScreen(Screen parent) {
        return new UikitConfigScreen(parent, this);
    }

    // --- internal getters (consumed by UikitConfigScreen) ------------------------------------

    public Component title() {
        return this.title;
    }

    public List<Category> categories() {
        return this.categories;
    }

    public UikitTheme theme() {
        return this.theme;
    }

    public Runnable saveConsumer() {
        return this.saveConsumer;
    }

    public Runnable cancelConsumer() {
        return this.cancelConsumer;
    }

    public static final class Builder {
        private Component title = Component.literal("ORB-UIKit");
        private final List<Category> categories = new ArrayList<>();
        private UikitTheme theme = UikitTheme.DEFAULT;
        private Runnable saveConsumer = () -> { };
        private Runnable cancelConsumer = () -> { };

        private Builder() {
        }

        public Builder title(Component title) {
            this.title = title;
            return this;
        }

        public Builder category(Category category) {
            this.categories.add(category);
            return this;
        }

        public Builder theme(UikitTheme theme) {
            this.theme = theme;
            return this;
        }

        /** Called when the user presses Save - the persistence hook. */
        public Builder saveConsumer(Runnable saveConsumer) {
            this.saveConsumer = saveConsumer;
            return this;
        }

        /** Called on explicit Cancel. */
        public Builder cancelConsumer(Runnable cancelConsumer) {
            this.cancelConsumer = cancelConsumer;
            return this;
        }

        public UikitConfig build() {
            return new UikitConfig(this.title, List.copyOf(this.categories), this.theme,
                    this.saveConsumer, this.cancelConsumer);
        }
    }
}