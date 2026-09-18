package dev.orbeyond.uikit.widget;

import java.util.function.IntConsumer;
import java.util.regex.Pattern;

import io.wispforest.owo.ui.component.BoxComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.OverlayContainer;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.CursorStyle;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.OwoUIGraphics;
import io.wispforest.owo.ui.core.Positioning;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.VerticalAlignment;

import net.minecraft.network.chat.Component;

import dev.orbeyond.uikit.theme.UikitTheme;

/**
 * A packed-{@code 0xRRGGBB} colour control drawn brutalist: a flat square swatch, flush with the
 * {@code #RRGGBB} hex field beside it. Clicking the swatch opens a {@link BrutalColorPicker} in a
 * modal overlay (an SV square + a vertical rainbow hue slider, too wide to sit inline in a narrow
 * drawer). The swatch, the hex field and the picker all stay in sync; every committed change calls
 * {@code onChange} with the new packed int.
 *
 * <p>{@code overlayHost} must be the UI adapter's {@code rootComponent} - the overlay is mounted
 * there (never inside the drawer), and it is sized to the root's live laid-out dimensions rather
 * than {@code Sizing.fill(100)} so it spans the full <em>virtual</em> canvas even while the screen
 * is rendering at a scaled resolution.
 */
public final class BrutalColorField {
    private static final Pattern HEX = Pattern.compile("#?[0-9A-Fa-f]{6}");

    /** Hex field width, in px (not a theme token; the theme has no hex-field width). */
    private static final int HEX_W = 58;

    private final FlowLayout root;
    private final Swatch swatch;
    private final TextBoxComponent hex;
    private final IntConsumer onChange;
    private final FlowLayout overlayHost;

    private int rgb;
    private boolean muted;

    public BrutalColorField(int initialRgb, IntConsumer onChange, FlowLayout overlayHost) {
        this.onChange = onChange;
        this.overlayHost = overlayHost;

        this.root = UIContainers.horizontalFlow(Sizing.content(), Sizing.content());
        this.root.gap(6);
        this.root.verticalAlignment(VerticalAlignment.CENTER);

        this.swatch = new Swatch();
        this.swatch.cursorStyle(CursorStyle.HAND);
        this.swatch.mouseDown().subscribe((click, doubled) -> {
            openPicker();
            return true;
        });

        this.hex = UIComponents.textBox(Sizing.fixed(HEX_W));
        this.hex.verticalSizing(Sizing.fixed(UikitTheme.DEFAULT.swatchSize()));   // match the swatch exactly, flush
        this.hex.setMaxLength(7);
        this.hex.onChanged().subscribe(this::onHexTyped);

        this.root.child(this.swatch);
        this.root.child(this.hex);

        set(initialRgb);
    }

    /** The row fragment to drop into a slider/colour list. */
    public FlowLayout component() {
        return this.root;
    }

    /** Push a value in from the config without firing {@code onChange} (pusher / reset path). */
    public void set(int newRgb) {
        this.rgb = newRgb & 0xFFFFFF;
        this.muted = true;
        this.swatch.rgb(this.rgb);
        this.hex.text(String.format("#%06X", this.rgb));
        this.muted = false;
    }

    private void commit(int newRgb) {
        this.rgb = newRgb & 0xFFFFFF;
        this.swatch.rgb(this.rgb);
        this.onChange.accept(this.rgb);
    }

    private void onHexTyped(String s) {
        if (this.muted) {
            return;
        }
        String t = s.trim();
        if (!HEX.matcher(t).matches()) {
            return;
        }
        commit(Integer.parseInt(t.replace("#", ""), 16));
    }

    private void openPicker() {
        BrutalColorPicker picker = new BrutalColorPicker();
        picker.selectedColor(Color.ofRgb(this.rgb));
        picker.onChanged(c -> {
            int v = c.rgb() & 0xFFFFFF;
            this.muted = true;
            this.hex.text(String.format("#%06X", v));
            this.muted = false;
            commit(v);
        });

        UikitTheme theme = UikitTheme.DEFAULT;
        FlowLayout panel = UIContainers.verticalFlow(Sizing.content(), Sizing.content());
        // theme.panel() == 0xFF121212, theme.marker() == 0xFFFFFFFF: the reference picker panel.
        panel.surface(Surface.flat(theme.panel()).and(Surface.outline(theme.marker())));
        panel.padding(Insets.of(10));
        panel.gap(6);
        panel.horizontalAlignment(HorizontalAlignment.CENTER);
        panel.child(UIComponents.label(Component.translatable("uikit.color.pick")));
        panel.child(picker);

        OverlayContainer<FlowLayout> overlay = UIContainers.overlay(panel);
        overlay.positioning(Positioning.absolute(0, 0));
        // Span the whole VIRTUAL canvas, read live from the adapter root - Sizing.fill(100) can
        // mis-resolve while the screen renders at a scaled resolution.
        int w = this.overlayHost.width();
        int h = this.overlayHost.height();
        if (w > 0 && h > 0) {
            overlay.sizing(Sizing.fixed(w), Sizing.fixed(h));
        } else {
            overlay.sizing(Sizing.fill(100), Sizing.fill(100));
        }
        // Visible dim so the overlay bounds are obvious and it clearly sits above rail + drawer.
        overlay.surface(Surface.flat(theme.overlayDim()));

        this.overlayHost.child(overlay);
    }

    /**
     * The inline square swatch - the same height as the hex field so the row reads flush. A
     * {@link BoxComponent} subclass so it still slots into the row and carries the click handler,
     * but it paints its own flat fill + 1px outline rather than relying on {@code BoxComponent}'s
     * gradient draw. The black outline is structural (the picker's own outline colour), not a theme
     * token.
     */
    private static final class Swatch extends BoxComponent {
        private int argb = 0xFF000000;

        private Swatch() {
            super(Sizing.fixed(UikitTheme.DEFAULT.swatchSize()), Sizing.fixed(UikitTheme.DEFAULT.swatchSize()));
        }

        private void rgb(int rgb) {
            this.argb = 0xFF000000 | (rgb & 0xFFFFFF);
        }

        @Override
        public void draw(OwoUIGraphics graphics, int mouseX, int mouseY, float partialTick, float delta) {
            graphics.fill(this.x, this.y, this.x + this.width, this.y + this.height, this.argb);
            graphics.drawRectOutline(this.x, this.y, this.width, this.height, 0xFF000000);
        }
    }
}