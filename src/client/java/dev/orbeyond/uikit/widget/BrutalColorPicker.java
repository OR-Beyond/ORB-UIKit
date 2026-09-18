package dev.orbeyond.uikit.widget;

import java.util.function.Consumer;

import io.wispforest.owo.ui.base.BaseUIComponent;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.CursorStyle;
import io.wispforest.owo.ui.core.OwoUIGraphics;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;

import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Mth;

/**
 * A compact HSV colour picker drawn brutalist, so it does not depend on owo's
 * {@code ColorPickerComponent} (whose spectrum needs the {@code GUI_HSV} render pipeline).
 *
 * <ul>
 *   <li><b>SV square</b> - the standard three-layer composite, all through vanilla
 *       {@code GuiGraphics} (which alpha-blends): a solid base of the selected hue, then a
 *       horizontal white to transparent-white pass for saturation, then a vertical
 *       transparent-black to black {@code fillGradient} for value. owo's own {@code drawGradientRect}
 *       does not blend its corner alpha here, which is what turned the square solid black.
 *   <li><b>Vertical hue slider</b> - the full rainbow, one 1px {@code fill} row per hue step.
 *   <li>A 5px reticle on the square and a white bar across the hue slider, both clamped strictly
 *       inside their track.
 * </ul>
 *
 * <p>Click <em>or drag</em> either region to update the colour live - the component reports itself
 * focusable ({@link #canFocus}), which is what makes owo route {@link #onMouseDrag} to it. Every
 * change calls the {@link #onChanged} listener with the new {@link Color}. State is kept as raw
 * {@code hue/sat/val} so a fully desaturated or black colour still remembers the hue.
 *
 * <p>The drawing colours below (black outline, white reticle, transparent/solid black gradients)
 * are structural to the composite and are kept verbatim from the Vantage original - the theme
 * record has no fields for them.
 */
public final class BrutalColorPicker extends BaseUIComponent {
    /** Saturation/value square edge, in px. Also the hue slider height. */
    private static final int SV = 110;
    /** Hue slider width, in px. */
    private static final int HUE_W = 14;
    /** Gap between the square and the hue slider. */
    private static final int GAP = 6;

    private static final int OUTLINE = 0xFF000000;
    private static final int RETICLE = 0xFFFFFFFF;
    private static final int CLEAR_BLACK = 0x00000000;
    private static final int SOLID_BLACK = 0xFF000000;

    private float hue;
    private float sat = 1.0f;
    private float val = 1.0f;
    private Consumer<Color> onChanged = c -> { };

    public BrutalColorPicker() {
        this.cursorStyle = CursorStyle.HAND;
        this.sizing(Sizing.fixed(SV + GAP + HUE_W), Sizing.fixed(SV));
    }

    public BrutalColorPicker onChanged(Consumer<Color> listener) {
        this.onChanged = listener;
        return this;
    }

    /** Seed the selectors from an existing colour (no listener fired). */
    public BrutalColorPicker selectedColor(Color color) {
        float[] hsv = color.hsv();
        this.hue = hsv[0];
        this.sat = hsv[1];
        this.val = hsv[2];
        return this;
    }

    public Color selectedColor() {
        return Color.ofHsv(this.hue, this.sat, this.val);
    }

    // --- rendering -----------------------------------------------------------------------------

    @Override
    public void draw(OwoUIGraphics graphics, int mouseX, int mouseY, float partialTick, float delta) {
        final int x = this.x;
        final int y = this.y;

        // SV square, three alpha-blended layers via vanilla GuiGraphics:
        //   1. base  - the selected hue, fully saturated and bright.
        graphics.fill(x, y, x + SV, y + SV, Color.ofHsv(this.hue, 1.0f, 1.0f).argb());
        //   2. saturation - horizontal 0xFFFFFFFF (left) -> 0x00FFFFFF (right). GuiGraphics has no
        //      horizontal gradient, so paint it as 1px columns of fading white.
        for (int px = 0; px < SV; px++) {
            int alpha = Math.round(255.0f * (1.0f - px / (float) (SV - 1)));
            graphics.fill(x + px, y, x + px + 1, y + SV, (alpha << 24) | 0x00FFFFFF);
        }
        //   3. value - vertical 0x00000000 (top) -> 0xFF000000 (bottom).
        graphics.fillGradient(x, y, x + SV, y + SV, CLEAR_BLACK, SOLID_BLACK);
        graphics.drawRectOutline(x, y, SV, SV, OUTLINE);

        // Vertical hue slider: the rainbow, one 1px row per hue step.
        final int hueX = x + SV + GAP;
        for (int py = 0; py < SV; py++) {
            int band = Color.ofHsv(py / (float) (SV - 1), 1.0f, 1.0f).argb();
            graphics.fill(hueX, y + py, hueX + HUE_W, y + py + 1, band);
        }
        graphics.drawRectOutline(hueX, y, HUE_W, SV, OUTLINE);

        // SV reticle - centre clamped so the 5px box never leaves the square.
        int selX = Mth.clamp(x + Math.round(this.sat * (SV - 1)), x + 2, x + SV - 3);
        int selY = Mth.clamp(y + Math.round((1.0f - this.val) * (SV - 1)), y + 2, y + SV - 3);
        graphics.drawRectOutline(selX - 2, selY - 2, 5, 5, RETICLE);

        // Hue bar - full track width, height clamped so it stays inside the track.
        int hueY = Mth.clamp(y + Math.round(this.hue * (SV - 1)), y + 2, y + SV - 2);
        graphics.fill(hueX, hueY - 2, hueX + HUE_W, hueY + 2, OUTLINE);
        graphics.fill(hueX, hueY - 1, hueX + HUE_W, hueY + 1, RETICLE);
    }

    // --- input --------------------------------------------------------------------------------

    /**
     * Focusable on click so owo tracks it as the drag target - without this, owo only delivers
     * {@code onMouseDrag} to the focused component and the picker would be click-only.
     */
    @Override
    public boolean canFocus(UIComponent.FocusSource source) {
        return source == UIComponent.FocusSource.MOUSE_CLICK;
    }

    @Override
    public boolean onMouseDown(MouseButtonEvent event, boolean doubleClick) {
        super.onMouseDown(event, doubleClick);
        sample(event.x(), event.y());
        return true;
    }

    @Override
    public boolean onMouseDrag(MouseButtonEvent event, double deltaX, double deltaY) {
        super.onMouseDrag(event, deltaX, deltaY);
        sample(event.x(), event.y());
        return true;
    }

    /** owo hands these coordinates in already component-local (0..size). */
    private void sample(double localX, double localY) {
        float t = (float) (Mth.clamp(localY, 0.0, SV - 1) / (SV - 1));
        if (localX >= SV + GAP) {
            this.hue = t;
        } else {
            this.sat = (float) (Mth.clamp(localX, 0.0, SV - 1) / (SV - 1));
            this.val = 1.0f - t;
        }
        this.onChanged.accept(selectedColor());
    }
}