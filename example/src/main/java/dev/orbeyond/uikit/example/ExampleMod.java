package dev.orbeyond.uikit.example;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/**
 * Client entrypoint: registers the G keybind that opens the ORB-UIKit demo config screen.
 */
public final class ExampleMod implements ClientModInitializer {

    private static final KeyMapping KEY = KeyBindingHelper.registerKeyBinding(
            new KeyMapping("Open ORB-UIKit Demo", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G,
                    KeyMapping.Category.register(Identifier.fromNamespaceAndPath("orb-uikit-example", "demo"))));

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (KEY.consumeClick() && Minecraft.getInstance().screen == null) {
                Minecraft.getInstance().setScreen(ExampleConfig.screen(null));
            }
        });
    }
}