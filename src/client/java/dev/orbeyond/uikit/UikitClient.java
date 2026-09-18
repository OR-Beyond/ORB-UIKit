package dev.orbeyond.uikit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.wispforest.owo.ui.parsing.UIParsing;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import dev.orbeyond.uikit.widget.BrutalCheckbox;

/**
 * Client entrypoint for ORB-UIKit.
 *
 * <p>Note: {@code fabric.mod.json} deliberately has no entrypoint (the library is wired up by the
 * consuming mod's screen), so {@link #onInitializeClient} is only invoked when a consumer registers
 * this class. The {@code <brutal-checkbox>} owo-ui tag registration lives here so any screen that
 * parses a model with that tag gets the brutalist checkbox.
 */
public final class UikitClient implements ClientModInitializer {
    public static final String MOD_ID = "uikit";
    public static final Logger LOGGER = LoggerFactory.getLogger("ORB-UIKit");

    /**
     * Builds a {@code uikit:} identifier. 1.21.11 Mojang mappings have no {@code Identifier.of};
     * the equivalent factory is {@link Identifier#fromNamespaceAndPath(String, String)}.
     */
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitializeClient() {
        // owo-ui: <brutal-checkbox> in config.xml. Registered before any screen parses the model.
        UIParsing.registerFactory("brutal-checkbox", element -> new BrutalCheckbox(Component.empty()));
    }
}