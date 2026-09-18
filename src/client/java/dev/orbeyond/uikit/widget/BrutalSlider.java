package dev.orbeyond.uikit.widget;

import io.wispforest.owo.ui.component.SlimSliderComponent;
import io.wispforest.owo.ui.core.OwoUIGraphics;
import io.wispforest.owo.ui.core.Sizing;

import dev.orbeyond.uikit.theme.UikitTheme;

/**
 * A horizontal slider drawn brutalist: a 1px track, a solid fill up to the thumb, and a flat square
 * thumb. No texture, no rounded corners, no animation.
 *
 * <p>Subclasses {@link SlimSliderComponent} purely to reuse its drag / value / event machinery
 * ({@code min}/{@code max}/{@code stepSize}/{@code value}/{@code onChanged}); only {@link #draw} is
 * replaced. Values are in real units - the binder calls {@code min()/max()/stepSize()/value()}.
 */
public final class BrutalSlider extends SlimSliderComponent {

    public BrutalSlider() {
        super(Axis.HORIZONTAL);
        this.sizing(Sizing.expand(100), Sizing.fixed(16));
    }

    @Override
    public void draw(OwoUIGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta) {
        UikitTheme theme = UikitTheme.DEFAULT;
        int midY = this.y + this.height / 2;
        int left = this.x + 1;
        int right = this.x + this.width - 1;
        double t = this.value.get();   // normalised 0..1, already clamped
        int thumbX = left + (int) Math.round((right - left - theme.sliderThumbWidth()) * t);

        graphics.fill(left, midY, right, midY + 1, theme.sliderTrack());
        if (thumbX > left) {
            graphics.fill(left, midY, thumbX, midY + 1, theme.sliderFill());
        }
        graphics.fill(thumbX, this.y + 3, thumbX + theme.sliderThumbWidth(), this.y + this.height - 3, theme.sliderThumb());
        graphics.drawRectOutline(thumbX, this.y + 3, theme.sliderThumbWidth(), this.height - 6, theme.sliderTrack());
    }
}