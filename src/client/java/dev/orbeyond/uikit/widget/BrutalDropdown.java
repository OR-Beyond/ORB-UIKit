package dev.orbeyond.uikit.widget;

import java.util.List;
import java.util.function.Consumer;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.VerticalAlignment;

import net.minecraft.network.chat.Component;

import dev.orbeyond.uikit.theme.UikitTheme;
import dev.orbeyond.uikit.util.OverlayMenu;

/**
 * A brutalist dropdown: a flat outlined button showing the current selection, opening an
 * {@link OverlayMenu} of full-width flat rows under it. The currently selected row is marked with a
 * {@code ●} prefix; picking a row commits the selection and fires {@code onSelect} with its index.
 *
 * <p>{@code overlayHost} must be the UI adapter's {@code rootComponent} - the menu is mounted there
 * (never inside the drawer) so nothing clips it and it hit-tests above the drawer's scroll
 * container.
 */
public final class BrutalDropdown {
    /** Menu row width, in px - matches the reference Vantage menu entry width. */
    private static final int MENU_WIDTH = 150;

    private final FlowLayout root;
    private final ButtonComponent button;
    private final FlowLayout overlayHost;

    private List<Component> entries = List.of();
    private int selectedIndex = -1;
    private Consumer<Integer> onSelect = i -> { };
    private FlowLayout openMenu;

    public BrutalDropdown(FlowLayout overlayHost, Component label) {
        this.overlayHost = overlayHost;

        this.root = UIContainers.horizontalFlow(Sizing.content(), Sizing.content());
        this.root.verticalAlignment(VerticalAlignment.CENTER);
        this.root.gap(6);

        if (label != null) {
            LabelComponent labelComponent = UIComponents.label(label);
            labelComponent.horizontalSizing(Sizing.fixed(UikitTheme.DEFAULT.labelWidth()));
            this.root.child(labelComponent);
        }

        this.button = UIComponents.button(Component.literal("—"), b -> toggleMenu());
        this.button.renderer(RENDERER);
        this.button.horizontalSizing(Sizing.expand(100));
        this.button.verticalSizing(Sizing.fixed(UikitTheme.DEFAULT.rowHeight()));
        this.root.child(this.button);
    }

    /** The row fragment to drop into a slider/colour list. */
    public FlowLayout component() {
        return this.root;
    }

    /** Replace the entry list, select {@code selectedIndex} and install the selection callback. */
    public void setEntries(List<Component> entries, int selectedIndex, Consumer<Integer> onSelect) {
        this.entries = entries;
        this.onSelect = onSelect;
        setSelected(selectedIndex);
    }

    /** Push a selection in from the config without firing {@code onSelect} (pusher / reset path). */
    public void setSelected(int index) {
        this.selectedIndex = index;
        this.button.setMessage(index >= 0 && index < this.entries.size()
                ? this.entries.get(index)
                : Component.literal("—"));
    }

    private void toggleMenu() {
        if (this.openMenu != null) {
            OverlayMenu.close(this.overlayHost, this.openMenu);
            this.openMenu = null;
            return;
        }
        this.openMenu = OverlayMenu.open(this.button, this.overlayHost, menu -> {
            for (int i = 0; i < this.entries.size(); i++) {
                final int index = i;
                Component text = this.entries.get(i);
                Component message = index == this.selectedIndex
                        ? Component.literal("● ").append(text)
                        : text;
                ButtonComponent entry = UIComponents.button(message, b -> {
                    setSelected(index);
                    this.onSelect.accept(index);
                    OverlayMenu.close(this.overlayHost, this.openMenu);
                    this.openMenu = null;
                });
                entry.renderer(RENDERER);
                entry.horizontalSizing(Sizing.fixed(MENU_WIDTH));
                entry.verticalSizing(Sizing.fixed(UikitTheme.DEFAULT.rowHeight()));
                menu.child(entry);
            }
        });
    }

    /** Flat fill + 1px outline, hover washed with the theme's hover wash. */
    private static final ButtonComponent.Renderer RENDERER = (graphics, button, delta) -> {
        UikitTheme theme = UikitTheme.DEFAULT;
        int fill = !button.active()
                ? theme.menu()
                : button.isHovered() ? wash(theme.menu(), theme.hoverWash()) : theme.menu();
        graphics.fill(button.getX(), button.getY(),
                button.getX() + button.getWidth(), button.getY() + button.getHeight(), fill);
        graphics.drawRectOutline(button.getX(), button.getY(),
                button.getWidth(), button.getHeight(), theme.outline());
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
}