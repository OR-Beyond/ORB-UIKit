package dev.orbeyond.uikit.widget;

import io.wispforest.owo.ui.component.CheckboxComponent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import dev.orbeyond.uikit.UikitClient;

/**
 * A checkbox drawn brutalist: a flat {@code #1E1E1E} square with a 1px outline, and a hard white
 * tick when checked. Two states only - checked / unchecked - matching the rest of the workspace.
 *
 * <p>Subclasses owo's {@link CheckboxComponent} only to repaint the box. {@code super.renderContents}
 * still positions and draws the label, so a labelled option row is unaffected; this just blits the
 * uikit sprite over the vanilla box afterwards (the sprite is opaque, so the vanilla one never
 * shows). Registered as the {@code <brutal-checkbox>} owo-ui tag in {@code UikitClient}.
 */
public final class BrutalCheckbox extends CheckboxComponent {
    private static final Identifier CHECKED = UikitClient.id("checkbox/checked");
    private static final Identifier UNCHECKED = UikitClient.id("checkbox/unchecked");

    public BrutalCheckbox(Component label) {
        super(label);
    }

    @Override
    public void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderContents(graphics, mouseX, mouseY, partialTick);
        int box = Checkbox.getBoxSize(Minecraft.getInstance().font);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED,
                selected() ? CHECKED : UNCHECKED, getX(), getY(), box, box);
    }
}