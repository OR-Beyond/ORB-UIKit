package dev.orbeyond.uikit.widget;

import java.util.function.Consumer;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;
import io.wispforest.owo.ui.core.VerticalAlignment;

import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import com.mojang.blaze3d.platform.InputConstants;

import dev.orbeyond.uikit.theme.UikitTheme;

/**
 * A brutalist keybind row: a flat outlined button showing the current binding, which enters a
 * listening mode when pressed. While listening the button highlights and the next key or
 * mouse-button press is captured:
 *
 * <ul>
 *   <li><b>Escape</b> cancels without binding.
 *   <li><b>Left click</b> on the button itself cancels (a second press while listening).
 *   <li><b>Any other key</b> binds via {@link InputConstants#getKey(KeyEvent)} (the 1.21.11
 *       equivalent of the old {@code getKey(int, int)} - it resolves the KEYSYM key when the event
 *       carries one, else the SCANCODE key).
 *   <li><b>Right / middle click</b> binds the mouse button via
 *       {@code InputConstants.Type.MOUSE.getOrCreate(button)} (0 = left, 1 = right, 2 = middle).
 *   <li><b>Clicking anywhere else</b> (focus loss) cancels.
 * </ul>
 *
 * <p>The button reports itself click-focusable, which is what makes owo route key events to it
 * while listening. The unset binding renders as {@code —}, listening as {@code ...}, otherwise the
 * key's display name.
 */
public final class BrutalKeybind {

    private final FlowLayout root;
    private final KeybindButton button;

    private InputConstants.Key key;
    private boolean listening;
    private Consumer<InputConstants.Key> onChange = k -> { };

    public BrutalKeybind(Component label) {
        this.root = UIContainers.horizontalFlow(Sizing.content(), Sizing.content());
        this.root.verticalAlignment(VerticalAlignment.CENTER);
        this.root.gap(6);

        if (label != null) {
            LabelComponent labelComponent = UIComponents.label(label);
            labelComponent.horizontalSizing(Sizing.fixed(UikitTheme.DEFAULT.labelWidth()));
            this.root.child(labelComponent);
        }

        this.button = new KeybindButton(Component.literal("—"), b -> toggleListening());
        this.button.renderer(this.renderer);
        this.button.horizontalSizing(Sizing.expand(100));
        this.button.verticalSizing(Sizing.fixed(UikitTheme.DEFAULT.rowHeight()));
        this.root.child(this.button);

        // While listening, capture key presses. Escape cancels; anything else binds.
        this.button.keyPress().subscribe(input -> {
            if (!this.listening) {
                return false;
            }
            if (input.isEscape()) {
                cancelListening();
            } else {
                bind(InputConstants.getKey(input));
            }
            return true;
        });

        // Consume typed characters while listening so they never reach the screen.
        this.button.charTyped().subscribe(input -> this.listening);

        // Mouse buttons while listening: left click is left to the button's own press (cancel);
        // any other button binds the mouse button itself.
        this.button.mouseDown().subscribe((click, doubled) -> {
            if (!this.listening || click.button() == 0) {
                return false;
            }
            bind(InputConstants.Type.MOUSE.getOrCreate(click.button()));
            return true;
        });

        // Clicking anywhere else moves focus away and cancels listening.
        this.button.focusLost().subscribe(() -> {
            if (this.listening) {
                cancelListening();
            }
        });
    }

    /** The row fragment to drop into a slider/colour list. */
    public FlowLayout component() {
        return this.root;
    }

    /** Push a binding in from the config without firing {@code onChange} (pusher / reset path). */
    public void setKey(InputConstants.Key key) {
        this.key = key;
        updateMessage();
    }

    /** The current binding, or {@code null} when unset. */
    public InputConstants.Key key() {
        return this.key;
    }

    /** Install the listener fired on every committed binding change. */
    public void onChange(Consumer<InputConstants.Key> listener) {
        this.onChange = listener;
    }

    private void toggleListening() {
        this.listening = !this.listening;
        updateMessage();
    }

    private void cancelListening() {
        this.listening = false;
        updateMessage();
    }

    private void bind(InputConstants.Key key) {
        this.key = key;
        this.listening = false;
        updateMessage();
        this.onChange.accept(key);
    }

    private void updateMessage() {
        if (this.listening) {
            this.button.setMessage(Component.literal("..."));
        } else if (this.key == null) {
            this.button.setMessage(Component.literal("—"));
        } else {
            this.button.setMessage(this.key.getDisplayName());
        }
    }

    /** Flat fill + 1px outline; while listening the fill is washed and the outline turns white. */
    private final ButtonComponent.Renderer renderer = (graphics, button, delta) -> {
        UikitTheme theme = UikitTheme.DEFAULT;
        int fill = !button.active()
                ? theme.menu()
                : (this.listening || button.isHovered()) ? wash(theme.menu(), theme.hoverWash()) : theme.menu();
        graphics.fill(button.getX(), button.getY(),
                button.getX() + button.getWidth(), button.getY() + button.getHeight(), fill);
        graphics.drawRectOutline(button.getX(), button.getY(),
                button.getWidth(), button.getHeight(), this.listening ? theme.marker() : theme.outline());
    };

    /** Composite {@code wash} (a translucent white) over {@code base}, opaque result. */
    private static int wash(int base, int wash) {
        int a = (wash >>> 24) & 0xFF;
        int r = (base >> 16) & 0xFF, g = (base >> 8) & 0xFF, b = base & 0xFF;
        int wr = (wash >> 16) & 0xFF, wg = (wash >> 8) & 0xFF, wb = wash & 0xFF;
        r += (wr - r) * a / 255;
        g += (wg - g) * a / 255;
        b += (wb - b) * a / 255;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /**
     * The binding button. Reports itself click-focusable so owo routes key events to it while
     * listening; {@code KeyMapping} is referenced only to keep the import contract explicit.
     */
    private static final class KeybindButton extends ButtonComponent {

        private KeybindButton(Component message, Consumer<ButtonComponent> onPress) {
            super(message, onPress);
        }

        @Override
        public boolean canFocus(UIComponent.FocusSource source) {
            return source == UIComponent.FocusSource.MOUSE_CLICK;
        }
    }
}