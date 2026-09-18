package dev.orbeyond.uikit.widget;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.util.NinePatchTexture;

import net.minecraft.resources.Identifier;

import dev.orbeyond.uikit.UikitClient;

/**
 * The flat 9-slice skin every button on the config screen wears.
 *
 * <p>Mirrors owo's built-in {@code VANILLA} button renderer exactly - {@code !active -> disabled},
 * {@code hovered -> hovered}, else {@code active} - but points at
 * {@code uikit:nine_patch_textures/button/*} instead of owo's own button sheet. The three JSONs
 * describe a 3px corner / 10px centre patch of {@code textures/gui/brutal_button.png}, so the border
 * stays 1px crisp at any button width and the flat centre just stretches. No hover animation, no
 * rounded corners - it reads the same as {@link BrutalSlider} and {@link BrutalCollapsible}.
 */
public final class BrutalButton {
    private static final Identifier ACTIVE = UikitClient.id("button/active");
    private static final Identifier HOVERED = UikitClient.id("button/hovered");
    private static final Identifier DISABLED = UikitClient.id("button/disabled");

    /** Shared, stateless: hand this to {@code ButtonComponent.renderer(...)}. */
    public static final ButtonComponent.Renderer RENDERER = (graphics, button, delta) -> {
        Identifier texture = !button.active()
                ? DISABLED
                : button.isHovered() ? HOVERED : ACTIVE;
        NinePatchTexture.draw(texture, graphics,
                button.getX(), button.getY(), button.getWidth(), button.getHeight());
    };

    private BrutalButton() {
    }
}