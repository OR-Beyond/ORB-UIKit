package dev.orbeyond.uikit.screen;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import io.wispforest.owo.ui.base.BaseUIModelScreen;
import io.wispforest.owo.ui.component.BoxComponent;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextureComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.CursorStyle;
import io.wispforest.owo.ui.core.Easing;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Positioning;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.VerticalAlignment;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import dev.orbeyond.uikit.UikitClient;
import dev.orbeyond.uikit.api.Category;
import dev.orbeyond.uikit.api.Option;
import dev.orbeyond.uikit.api.OptionGroup;
import dev.orbeyond.uikit.api.UikitConfig;
import dev.orbeyond.uikit.binding.OptionBinder;
import dev.orbeyond.uikit.widget.BrutalButton;

/**
 * The whole ORB-UIKit configuration on one brutalist owo-ui workspace, generalized from Vantage's
 * {@code VantageConfigScreen} (Shader-Mod by tkedickson, MIT, extracted with permission).
 *
 * <p>Left-anchored panel (rail + drawer), per-category drawer of {@code BrutalCollapsible}
 * accordion groups whose enable toggle sits inline in the header. The shell is
 * {@code assets/uikit/owo_ui/config.xml}; the option rows are built here via
 * {@link OptionBinder}. The right side of the screen is transparent.
 *
 * <p>State pipeline is unchanged from the reference: every widget change writes the option's
 * setter immediately; explicit {@code dirty} tracking via a baseline snapshot; Save runs the
 * config's save consumer and re-snapshots; Cancel/ESC restores the baseline through the setters.
 */
public final class UikitConfigScreen extends BaseUIModelScreen<FlowLayout> {

    private static final Surface RAIL_SELECTED = Surface.flat(0xFF222222).and(Surface.outline(0xFF333333));
    private static final Surface RAIL_HOVER = Surface.flat(0x18FFFFFF);

    /** How far left to park the panel for the slide animation (wider than rail 48 + drawer 248). */
    private static final int PANEL_OFFSCREEN = 300;

    private final Screen parent;
    private final UikitConfig config;

    /** Diff target for the Save button. Re-taken on open + Save. */
    private Map<Option<?>, Object> baseline;
    private boolean dirty;

    private OptionBinder binder;
    private FlowLayout drawerContent;
    private FlowLayout panel;
    private BoxComponent railMarker;
    private ButtonComponent saveButton;
    private LabelComponent dirtyHint;
    private LabelComponent drawerTitle;
    private final Map<Integer, FlowLayout> panels = new LinkedHashMap<>();
    private final Map<Integer, FlowLayout> tabs = new LinkedHashMap<>();
    private int activeTab = -1;
    private boolean closing;
    private boolean closeRequested;
    private boolean laidOut;

    public UikitConfigScreen(Screen parent, UikitConfig config) {
        super(FlowLayout.class, UikitClient.id("config"));
        this.parent = parent;
        this.config = config;
        this.baseline = snapshot();
    }

    // --- build -----------------------------------------------------------------------------------

    @Override
    protected void build(FlowLayout root) {
        this.drawerContent = root.childById(FlowLayout.class, "drawer-content");
        this.panel = root.childById(FlowLayout.class, "panel");
        this.railMarker = root.childById(BoxComponent.class, "rail-marker");
        this.saveButton = root.childById(ButtonComponent.class, "btn-save");
        this.dirtyHint = root.childById(LabelComponent.class, "dirty-hint");
        this.drawerTitle = root.childById(LabelComponent.class, "drawer-title");
        this.binder = new OptionBinder(config.theme(), this::recomputeDirty, root);

        FlowLayout rail = root.childById(FlowLayout.class, "rail");
        List<Category> categories = config.categories();
        for (int i = 0; i < categories.size(); i++) {
            Category category = categories.get(i);

            // 44x40 rail tab: 16px icon texture, or the category name as text.
            FlowLayout tab = UIContainers.verticalFlow(Sizing.fixed(44), Sizing.fixed(40));
            tab.horizontalAlignment(HorizontalAlignment.CENTER);
            tab.verticalAlignment(VerticalAlignment.CENTER);
            if (category.icon != null) {
                TextureComponent icon = UIComponents.texture(category.icon, 0, 0, 16, 16, 16, 16);
                icon.blend(true);
                tab.child(icon);
            } else {
                tab.child(UIComponents.label(category.name));
            }
            rail.child(tab);
            this.tabs.put(i, tab);

            // Drawer panel: one collapsible per group.
            FlowLayout categoryPanel = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
            categoryPanel.gap(3);
            for (OptionGroup group : category.groups) {
                categoryPanel.child(this.binder.group(group));
            }
            this.panels.put(i, categoryPanel);
        }

        for (int i = 0; i < categories.size(); i++) {
            final int index = i;
            FlowLayout tab = this.tabs.get(i);
            tab.cursorStyle(CursorStyle.HAND);
            tab.mouseDown().subscribe((click, doubled) -> {
                showTab(index);
                return true;
            });
            tab.mouseEnter().subscribe(() -> {
                if (index != this.activeTab) {
                    tab.surface(RAIL_HOVER);
                }
            });
            tab.mouseLeave().subscribe(() -> {
                if (index != this.activeTab) {
                    tab.surface(Surface.BLANK);
                }
            });
        }

        ButtonComponent cancel = root.childById(ButtonComponent.class, "btn-cancel");
        cancel.setMessage(Component.literal("Cancel"));
        cancel.onPress(b -> cancel());
        cancel.renderer(BrutalButton.RENDERER);
        this.saveButton.setMessage(Component.literal("Save"));
        this.saveButton.onPress(b -> save());
        this.saveButton.renderer(BrutalButton.RENDERER);

        // Rail hiding: with no icons the rail is dead weight - collapse it so the drawer takes the
        // full panel. The marker box stays mounted (clipped by the 0-width rail, harmless).
        boolean anyIcon = categories.stream().anyMatch(c -> c.icon != null);
        if (!anyIcon) {
            rail.horizontalSizing(Sizing.fixed(0));
            rail.surface(Surface.BLANK);
        }

        if (!categories.isEmpty()) {
            showTab(0);
        }
        this.dirty = false;
        this.saveButton.active(false);
        this.dirtyHint.text(Component.empty());

        // Slide-in: park the panel off the left edge, then ease it home.
        this.panel.margins(Insets.left(-PANEL_OFFSCREEN));
        this.panel.margins().animate(config.theme().panelInMs(), Easing.CUBIC, Insets.of(0)).forwards();
    }

    // --- independent UI scaling (ignore MC GUI scale) ------------------------------------------

    @Override
    protected void init() {
        super.init();
        // Lay the adapter out in a virtual resolution of screenSize / f, so that after the render-time
        // scale(f) it covers the whole screen edge-to-edge - otherwise a sub-1 scale leaves the panel
        // stopping short of the bottom.
        if (this.uiAdapter != null) {
            float f = uiScale();
            this.uiAdapter.moveAndResize(0, 0,
                    Math.round(this.width / f), Math.round(this.height / f));
            this.laidOut = true;
            snapMarker();
        }
    }

    /** Render/input scale so the workspace stays compact regardless of the MC GUI scale (never enlarges). */
    private float uiScale() {
        if (this.minecraft == null) {
            return 1f;
        }
        int mc = this.minecraft.getWindow().getGuiScale();
        return mc <= 0 ? 1f : Math.min(1f, config.theme().targetGuiScale() / mc);
    }

    private MouseButtonEvent rescale(MouseButtonEvent event) {
        float f = uiScale();
        return f == 1f ? event : new MouseButtonEvent(event.x() / f, event.y() / f, event.buttonInfo());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (this.closeRequested) {
            this.closeRequested = false;
            this.minecraft.setScreen(this.parent);
            return;
        }
        float f = uiScale();
        if (this.invalid || this.uiAdapter == null || f == 1f) {
            super.render(graphics, mouseX, mouseY, delta);
            return;
        }
        graphics.pose().pushMatrix();
        graphics.pose().scale(f, f);
        this.uiAdapter.render(graphics, (int) (mouseX / f), (int) (mouseY / f), delta);
        graphics.pose().popMatrix();

        // super.render() is skipped, so the ScreenEvents.afterRender tooltip hook never fires - draw it here.
        drawComponentTooltip(graphics, mouseX, mouseY, delta);
    }

    @Override
    protected void drawComponentTooltip(GuiGraphics graphics, int mouseX, int mouseY, float tickDelta) {
        float f = uiScale();
        if (this.uiAdapter == null || f == 1f) {
            super.drawComponentTooltip(graphics, mouseX, mouseY, tickDelta);
            return;
        }
        graphics.pose().pushMatrix();
        graphics.pose().scale(f, f);
        this.uiAdapter.drawTooltip(graphics, (int) (mouseX / f), (int) (mouseY / f), tickDelta);
        graphics.pose().popMatrix();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return super.mouseClicked(rescale(event), doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return super.mouseReleased(rescale(event));
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        float f = uiScale();
        return super.mouseDragged(rescale(event), deltaX / f, deltaY / f);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        float f = uiScale();
        super.mouseMoved(mouseX / f, mouseY / f);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        float f = uiScale();
        return super.mouseScrolled(mouseX / f, mouseY / f, scrollX, scrollY);
    }

    // --- navigation ---------------------------------------------------------------------------

    private void showTab(int index) {
        if (index < 0 || index >= this.tabs.size()) {
            return;
        }
        this.activeTab = index;

        this.drawerContent.clearChildren();
        this.drawerContent.child(this.panels.get(index));
        this.drawerTitle.text(this.config.categories().get(index).name);

        this.tabs.forEach((i, tab) -> tab.surface(i == index ? RAIL_SELECTED : Surface.BLANK));

        Positioning target = Positioning.absolute(0, railTabY(index));
        if (this.laidOut) {
            this.railMarker.positioning().animate(config.theme().markerMs(), Easing.CUBIC, target).forwards();
        } else {
            this.railMarker.positioning(target);   // pre-layout: no animation to fight
        }
    }

    /**
     * The marker's {@code positioning.y} (rail-local, before the rail's own top padding) so that its
     * top lines up exactly with the active tab box. Derived from the live layout - the offset of the
     * tab from the first tab - so it's independent of padding / gap / scale. Falls back to nominal
     * per-tab height (40 + 2 gap) before the first layout pass.
     */
    private int railTabY(int index) {
        FlowLayout first = this.tabs.get(0);
        FlowLayout tab = this.tabs.get(index);
        int delta = tab.y() - first.y();
        return (tab.y() != 0 || index == 0) ? delta : 42 * index;
    }

    /** Snap the marker onto the active tab once the layout is first valid (post-mount / resize). */
    private void snapMarker() {
        if (this.activeTab >= 0 && this.railMarker != null) {
            this.railMarker.positioning().animate(1, Easing.LINEAR, Positioning.absolute(0, railTabY(this.activeTab))).forwards();
        }
    }

    // --- state ------------------------------------------------------------------------------------

    private void recomputeDirty() {
        boolean now = !snapshot().equals(this.baseline);
        if (now == this.dirty) {
            return;
        }
        this.dirty = now;
        this.saveButton.active(now);   // instant - no pulse; strict brutalist snappiness
        this.dirtyHint.text(now ? Component.literal("Unsaved changes") : Component.empty());
    }

    private void save() {
        this.config.saveConsumer().run();
        this.baseline = snapshot();
        this.dirty = false;
        this.saveButton.active(false);
        this.dirtyHint.text(Component.empty());
    }

    private void cancel() {
        this.config.cancelConsumer().run();
        restoreBaseline();
        this.binder.pushAll();
        this.baseline = snapshot();
        this.dirty = false;
        onClose();
    }

    @Override
    public void onClose() {
        if (this.closing) {
            return;
        }
        if (this.dirty) {
            restoreBaseline();
        }
        slideOutThenClose();
    }

    /** Reverse the entrance slide; {@code render()} performs the actual screen swap once it finishes. */
    private void slideOutThenClose() {
        this.closing = true;
        if (this.panel == null || !this.laidOut) {
            this.closeRequested = true;
            return;
        }
        var slide = this.panel.margins().animate(config.theme().panelOutMs(), Easing.CUBIC, Insets.left(-PANEL_OFFSCREEN));
        // Flag rather than setScreen() here - the callback fires from inside owo's component update.
        slide.finished().subscribe((direction, looping) -> this.closeRequested = true);
        slide.forwards();
    }

    /** Read every option's getter into a diff/rollback map. */
    private Map<Option<?>, Object> snapshot() {
        Map<Option<?>, Object> map = new LinkedHashMap<>();
        for (Category category : this.config.categories()) {
            for (OptionGroup group : category.groups) {
                for (Option<?> option : group.options) {
                    map.put(option, option.binding.getter().get());
                }
            }
        }
        return map;
    }

    /** Push the baseline snapshot back through every option's setter (Cancel / dirty-ESC). */
    private void restoreBaseline() {
        for (Map.Entry<Option<?>, Object> entry : this.baseline.entrySet()) {
            // The baseline value was read from this option's own getter, so its type always matches.
            ((Consumer<Object>) entry.getKey().binding.setter()).accept(entry.getValue());
        }
    }
}