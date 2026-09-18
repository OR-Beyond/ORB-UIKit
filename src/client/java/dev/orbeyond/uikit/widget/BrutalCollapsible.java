package dev.orbeyond.uikit.widget;

import io.wispforest.owo.ui.component.CheckboxComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.CollapsibleContainer;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.CursorStyle;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.VerticalAlignment;
import io.wispforest.owo.ui.event.MouseDown;

import net.minecraft.network.chat.Component;

import dev.orbeyond.uikit.theme.UikitTheme;

/**
 * An accordion group drawn brutalist: flat dark header with a 1px outline, flat body, no rounded
 * panel, no left accent bar.
 *
 * <p>The header row is rebuilt as {@code [ label · spacer · (toggle) · chevron ]}. When built with
 * {@link #withToggle}, the feature's master enable/disable checkbox sits inline in the header next
 * to the chevron; clicking it toggles the feature without expanding the group (the checkbox
 * consumes its own click), while clicking the label / spacer / chevron expands/collapses.
 *
 * <p>Expand/collapse is wired explicitly on those three elements rather than relying on
 * {@code CollapsibleContainer}'s built-in header hitbox, which does not fire reliably once the group
 * is nested inside a scroll container.
 */
public final class BrutalCollapsible extends CollapsibleContainer {

    private final CheckboxComponent headerToggle;

    private BrutalCollapsible(Component title, boolean expanded, boolean withToggle) {
        super(Sizing.fill(100), Sizing.content(), title, expanded);

        UikitTheme theme = UikitTheme.DEFAULT;

        this.titleLayout.clearChildren();
        this.titleLayout.horizontalSizing(Sizing.fill(100));
        this.titleLayout.verticalAlignment(VerticalAlignment.CENTER);
        this.titleLayout.padding(Insets.of(4, 4, 6, 6));
        this.titleLayout.surface(Surface.flat(theme.collapsibleHeader()).and(Surface.outline(theme.outline())));

        LabelComponent label = UIComponents.label(title);
        label.cursorStyle(CursorStyle.HAND);
        FlowLayout spacer = UIContainers.horizontalFlow(Sizing.expand(100), Sizing.fixed(1));
        spacer.cursorStyle(CursorStyle.HAND);

        this.titleLayout.child(label);
        this.titleLayout.child(spacer);

        if (withToggle) {
            this.headerToggle = new BrutalCheckbox(Component.empty());
            this.headerToggle.margins(Insets.right(4));
            this.titleLayout.child(this.headerToggle);
        } else {
            this.headerToggle = null;
        }
        this.titleLayout.child(this.spinnyBoi);

        MouseDown toggle = (click, doubled) -> {
            this.toggleExpansion();
            return true;
        };
        label.mouseDown().subscribe(toggle);
        spacer.mouseDown().subscribe(toggle);
        this.spinnyBoi.mouseDown().subscribe(toggle);

        // CollapsibleContainer builds contentLayout content-sized on both axes, which collapses
        // fill-width rows to nothing. The group itself is fill(100), so the body must be too.
        this.contentLayout.horizontalSizing(Sizing.fill(100));
        this.contentLayout.surface(Surface.flat(theme.collapsibleBody()));
        this.contentLayout.padding(Insets.of(6));
    }

    public static BrutalCollapsible of(Component title, boolean expanded) {
        return new BrutalCollapsible(title, expanded, false);
    }

    public static BrutalCollapsible withToggle(Component title, boolean expanded) {
        return new BrutalCollapsible(title, expanded, true);
    }

    /** The inline header enable/disable checkbox, or {@code null} for a plain group. */
    public CheckboxComponent headerToggle() {
        return this.headerToggle;
    }
}