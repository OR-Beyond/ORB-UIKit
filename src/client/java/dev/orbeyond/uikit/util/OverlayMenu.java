package dev.orbeyond.uikit.util;

import java.util.function.Consumer;

import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Positioning;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.UIComponent;

import dev.orbeyond.uikit.theme.UikitTheme;

/**
 * A brutalist popup menu, ported from Vantage's {@code openMenu}/{@code closeMenu} pair.
 *
 * <p>Mounting as the last child of the root is what puts the menu on top. owo has no z-index, but
 * {@code ParentUIComponent.onMouseDown} and {@code childAt} both walk children in reverse while
 * drawing walks them forward - so the last child draws last and hit-tests first. Absolute
 * positioning keeps it out of the root's horizontal flow, and the root is above the drawer's scroll
 * container so nothing clips it.
 *
 * <p>The entries are built fresh on every open, so a value added a moment ago is simply in the
 * list; there is no retained menu whose handlers could go stale.
 */
public final class OverlayMenu {

    private OverlayMenu() {
    }

    /**
     * Opens a menu under {@code anchor}, clamped inside {@code root}'s bounds. {@code entries}
     * populates the menu before it is mounted (so its width/height are laid out for the clamp).
     *
     * @return the mounted menu, for the caller to track and later {@link #close}
     */
    public static FlowLayout open(UIComponent anchor, FlowLayout root, Consumer<FlowLayout> entries) {
        UikitTheme theme = UikitTheme.DEFAULT;
        FlowLayout menu = UIContainers.verticalFlow(Sizing.content(), Sizing.content());
        menu.surface(Surface.flat(theme.menu()).and(Surface.outline(theme.outline())));
        menu.padding(Insets.of(2));
        menu.gap(1);
        menu.allowOverflow(true);
        entries.accept(menu);

        // child() runs a layout pass, so width()/height() are valid for the clamp below.
        root.child(menu);

        int x = anchor.x() - root.x();
        int y = anchor.y() + anchor.height() - root.y();
        // Clamped against the ROOT's size, which is the adapter's virtual resolution - not the
        // screen's pixel size, which is the other half of the coordinate-space mistake above.
        x = Math.max(0, Math.min(x, root.width() - menu.width()));
        y = Math.max(0, Math.min(y, root.height() - menu.height()));
        menu.positioning(Positioning.absolute(x, y));

        return menu;
    }

    /**
     * Dismounts the menu, deferred so an entry can close the menu from inside its own press.
     * Null-friendly: a missing menu or root is a no-op.
     */
    public static void close(FlowLayout root, FlowLayout menu) {
        if (menu == null || root == null) {
            return;
        }
        // Removing a component while owo is dispatching a click into it leaves the focus handler
        // holding a dismounted widget, and BaseParentUIComponent.onMouseUp routes the matching
        // mouse-up straight to whatever focused() returns - so that release is swallowed and the
        // next click has to re-seat focus before anything responds. queue() runs the removal after
        // the dispatch finishes instead.
        root.queue(() -> root.removeChild(menu));
    }
}