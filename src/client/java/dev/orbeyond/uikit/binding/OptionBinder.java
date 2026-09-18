package dev.orbeyond.uikit.binding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.CheckboxComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.VerticalAlignment;

import net.minecraft.network.chat.Component;

import com.mojang.blaze3d.platform.InputConstants;

import dev.orbeyond.uikit.api.Option;
import dev.orbeyond.uikit.api.OptionBinding;
import dev.orbeyond.uikit.api.OptionGroup;
import dev.orbeyond.uikit.api.OptionKind;
import dev.orbeyond.uikit.theme.UikitTheme;
import dev.orbeyond.uikit.widget.BrutalCheckbox;
import dev.orbeyond.uikit.widget.BrutalCollapsible;
import dev.orbeyond.uikit.widget.BrutalColorField;
import dev.orbeyond.uikit.widget.BrutalDropdown;
import dev.orbeyond.uikit.widget.BrutalKeybind;
import dev.orbeyond.uikit.widget.BrutalSlider;

/**
 * Builds one brutalist config row and wires it to an {@link Option}'s binding.
 *
 * <p>Rows are {@code [ label · control · readout? · reset ]} horizontal flows, 20px tall (sliders
 * stack their label above a 16px control row). Every change writes the option's setter immediately
 * and calls {@code onChange} (the screen's dirty check). Nothing touches disk; the screen's Save
 * button does that. Reset restores the option's {@code defaultValue} through the setter, never a
 * literal.
 *
 * <p>Each row registers a <em>pusher</em>: {@link #pushAll()} re-seeds every widget from the live
 * getter, used on Cancel and after a baseline restore.
 */
public final class OptionBinder {

    private final UikitTheme theme;
    private final Runnable onChange;
    private final FlowLayout overlayHost;
    private final List<Runnable> pushers = new ArrayList<>();

    public OptionBinder(UikitTheme theme, Runnable onChange, FlowLayout overlayHost) {
        this.theme = theme;
        this.onChange = onChange;
        this.overlayHost = overlayHost;
    }

    public void pushAll() {
        this.pushers.forEach(Runnable::run);
    }

    // --- groups ------------------------------------------------------------------------------

    /**
     * A {@link BrutalCollapsible} holding one option row per group option. When the group carries
     * an enable toggle, the checkbox sits inline in the header and drives the master boolean.
     */
    public FlowLayout group(OptionGroup group) {
        BrutalCollapsible collapsible = group.enableToggle != null
                ? BrutalCollapsible.withToggle(group.name, true)
                : BrutalCollapsible.of(group.name, true);
        if (group.enableToggle != null) {
            bindHeaderToggle(collapsible.headerToggle(), group.enableToggle);
        }
        for (Option<?> option : group.options) {
            collapsible.child(row(option));
        }
        return collapsible;
    }

    /** Wires an existing checkbox (a {@code BrutalCollapsible} header toggle) to a master boolean. */
    private void bindHeaderToggle(CheckboxComponent box, OptionBinding<Boolean> binding) {
        box.checked(binding.getter().get());
        box.onChanged(v -> {
            binding.setter().accept(v);
            this.onChange.run();
        });
        this.pushers.add(() -> box.checked(binding.getter().get()));
    }

    // --- rows --------------------------------------------------------------------------------

    public FlowLayout row(Option<?> option) {
        return switch (option.kind) {
            case TOGGLE -> toggle(option);
            case INT_SLIDER, FLOAT_SLIDER, BLOCK_SLIDER -> slider(option);
            case ENUM_CYCLER -> enumCycler(option);
            case STRING_CYCLER -> stringCycler(option);
            case ENUM_DROPDOWN -> enumDropdown(option);
            case STRING_DROPDOWN -> stringDropdown(option);
            case COLOR -> color(option);
            case KEYBIND -> keybind(option);
        };
    }

    private FlowLayout toggle(Option<?> option) {
        FlowLayout row = row();
        CheckboxComponent box = new BrutalCheckbox(option.name);
        box.checked(currentBoolean(option));
        box.onChanged(v -> {
            writeBoolean(option, v);
            this.onChange.run();
        });
        ButtonComponent reset = resetButton();
        reset.onPress(b -> {
            writeBoolean(option, defaultBoolean(option));
            box.checked(currentBoolean(option));
            this.onChange.run();
        });
        this.pushers.add(() -> box.checked(currentBoolean(option)));

        row.child(box);
        row.child(spacer());
        row.child(reset);
        return row;
    }

    /** Stacked: option name on top, [ slider · readout · reset ] tight underneath. */
    private FlowLayout slider(Option<?> option) {
        FlowLayout stack = stackedRow();
        LabelComponent label = UIComponents.label(option.name);
        label.horizontalSizing(Sizing.fill(100));

        FlowLayout row = row();
        row.verticalSizing(Sizing.fixed(16));

        BrutalSlider s = new BrutalSlider();
        s.min(option.min).max(option.max).stepSize(option.decimals <= 0 ? 1.0 : Math.pow(10, -option.decimals));

        LabelComponent readout = UIComponents.label(Component.empty());
        readout.horizontalSizing(Sizing.fixed(this.theme.readoutWidth()));
        readout.horizontalTextAlignment(HorizontalAlignment.RIGHT);

        boolean blocks = option.kind == OptionKind.BLOCK_SLIDER;
        int decimals = option.decimals;

        Runnable refresh = () -> readout.text(Component.literal(format(readValue(option), decimals, blocks)));
        s.value(readValue(option));
        refresh.run();
        s.onChanged().subscribe(v -> {
            writeValue(option, v);
            refresh.run();
            this.onChange.run();
        });

        ButtonComponent reset = resetButton();
        reset.onPress(b -> {
            writeValue(option, readDefault(option));
            s.value(readValue(option));
            refresh.run();
            this.onChange.run();
        });
        this.pushers.add(() -> {
            s.value(readValue(option));
            refresh.run();
        });

        row.child(s);
        row.child(readout);
        row.child(reset);

        stack.child(label);
        stack.child(row);
        return stack;
    }

    private FlowLayout enumCycler(Option<?> option) {
        FlowLayout row = row();
        LabelComponent label = UIComponents.label(option.name);
        label.horizontalSizing(Sizing.fixed(this.theme.labelWidth()));

        ButtonComponent btn = UIComponents.button(Component.empty(), b -> { });
        btn.horizontalSizing(Sizing.expand(100));
        btn.renderer(ButtonComponent.Renderer.flat(0xFF262626, 0xFF3A3A3A, 0xFF1A1A1A));

        Runnable relabel = () -> btn.setMessage(option.displayName().apply(currentEnum(option)));
        relabel.run();
        btn.onPress(b -> {
            advanceEnum(option);
            relabel.run();
            this.onChange.run();
        });

        ButtonComponent reset = resetButton();
        reset.onPress(b -> {
            writeEnum(option, defaultEnum(option));
            relabel.run();
            this.onChange.run();
        });
        this.pushers.add(relabel);

        row.child(label);
        row.child(btn);
        row.child(reset);
        return row;
    }

    private FlowLayout stringCycler(Option<?> option) {
        FlowLayout row = row();
        LabelComponent label = UIComponents.label(option.name);
        label.horizontalSizing(Sizing.fixed(this.theme.labelWidth()));

        ButtonComponent btn = UIComponents.button(Component.empty(), b -> { });
        btn.horizontalSizing(Sizing.expand(100));
        btn.renderer(ButtonComponent.Renderer.flat(0xFF262626, 0xFF3A3A3A, 0xFF1A1A1A));

        Runnable relabel = () -> btn.setMessage(option.displayName().apply(currentString(option)));
        relabel.run();
        btn.onPress(b -> {
            advanceString(option);
            relabel.run();
            this.onChange.run();
        });

        ButtonComponent reset = resetButton();
        reset.onPress(b -> {
            writeString(option, defaultString(option));
            relabel.run();
            this.onChange.run();
        });
        this.pushers.add(relabel);

        row.child(label);
        row.child(btn);
        row.child(reset);
        return row;
    }

    private FlowLayout enumDropdown(Option<?> option) {
        FlowLayout row = row();
        BrutalDropdown dropdown = new BrutalDropdown(this.overlayHost, option.name);
        Enum<?>[] values = option.enumType.getEnumConstants();
        List<Component> entries = new ArrayList<>(values.length);
        for (Enum<?> value : values) {
            entries.add(option.displayName().apply(value));
        }
        Enum<?> cur = currentEnum(option);
        dropdown.setEntries(entries, cur == null ? 0 : cur.ordinal(), i -> {
            writeEnum(option, values[i]);
            this.onChange.run();
        });
        ButtonComponent reset = resetButton();
        reset.onPress(b -> {
            writeEnum(option, defaultEnum(option));
            Enum<?> d = currentEnum(option);
            dropdown.setSelected(d == null ? 0 : d.ordinal());
            this.onChange.run();
        });
        this.pushers.add(() -> {
            Enum<?> v = currentEnum(option);
            dropdown.setSelected(v == null ? 0 : v.ordinal());
        });

        row.child(dropdown.component());
        row.child(reset);
        return row;
    }

    private FlowLayout stringDropdown(Option<?> option) {
        FlowLayout row = row();
        BrutalDropdown dropdown = new BrutalDropdown(this.overlayHost, option.name);
        List<Component> entries = new ArrayList<>(option.stringValues.size());
        for (String value : option.stringValues) {
            entries.add(option.displayName().apply(value));
        }
        String cur = currentString(option);
        dropdown.setEntries(entries, Math.max(0, option.stringValues.indexOf(cur)), i -> {
            writeString(option, option.stringValues.get(i));
            this.onChange.run();
        });
        ButtonComponent reset = resetButton();
        reset.onPress(b -> {
            writeString(option, defaultString(option));
            dropdown.setSelected(Math.max(0, option.stringValues.indexOf(currentString(option))));
            this.onChange.run();
        });
        this.pushers.add(() -> dropdown.setSelected(Math.max(0, option.stringValues.indexOf(currentString(option)))));

        row.child(dropdown.component());
        row.child(reset);
        return row;
    }

    private FlowLayout color(Option<?> option) {
        FlowLayout row = row();
        BrutalColorField field = new BrutalColorField(
                currentColor(option),
                v -> {
                    writeColor(option, v);
                    this.onChange.run();
                },
                this.overlayHost);
        ButtonComponent reset = resetButton();
        reset.onPress(b -> {
            int d = defaultColor(option) & 0xFFFFFF;
            writeColor(option, d);
            field.set(d);
            this.onChange.run();
        });
        this.pushers.add(() -> field.set(currentColor(option)));

        row.child(label(option.name));
        row.child(field.component());
        row.child(spacer());
        row.child(reset);
        return row;
    }

    private FlowLayout keybind(Option<?> option) {
        FlowLayout row = row();
        BrutalKeybind keybind = new BrutalKeybind(option.name);
        keybind.setKey(currentKey(option));
        keybind.onChange(k -> {
            writeKey(option, k);
            this.onChange.run();
        });
        ButtonComponent reset = resetButton();
        reset.onPress(b -> {
            writeKey(option, defaultKey(option));
            keybind.setKey(currentKey(option));
            this.onChange.run();
        });
        this.pushers.add(() -> keybind.setKey(currentKey(option)));

        row.child(keybind.component());
        row.child(reset);
        return row;
    }

    // --- shared builders ---------------------------------------------------------------------

    private FlowLayout row() {
        FlowLayout f = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.fixed(this.theme.rowHeight()));
        f.verticalAlignment(VerticalAlignment.CENTER);
        f.gap(5);
        return f;
    }

    /** Label-over-control vertical stack: name on top, the control row tight beneath it. */
    private FlowLayout stackedRow() {
        FlowLayout f = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
        f.gap(1);
        return f;
    }

    private LabelComponent label(Component name) {
        LabelComponent l = UIComponents.label(name);
        l.horizontalSizing(Sizing.fixed(this.theme.labelWidth()));
        return l;
    }

    private FlowLayout spacer() {
        return UIContainers.horizontalFlow(Sizing.expand(100), Sizing.fixed(1));
    }

    private ButtonComponent resetButton() {
        ButtonComponent b = UIComponents.button(Component.literal("↺"), x -> { });
        b.renderer(ButtonComponent.Renderer.flat(0x00000000, 0x33FFFFFF, 0x00000000));
        b.horizontalSizing(Sizing.fixed(16));
        b.verticalSizing(Sizing.fixed(16));
        return b;
    }

    private static String format(double v, int decimals, boolean blocks) {
        String s = decimals <= 0
                ? Long.toString(Math.round(v))
                : String.format(Locale.ROOT, "%." + decimals + "f", v);
        return blocks ? s + "\u202Fb" : s;
    }

    // --- value plumbing ----------------------------------------------------------------------
    //
    // Option<?> erases the value type; the kind switch above guarantees which concrete type each
    // helper sees, so these read through Object and write through a Consumer<Object> cast. The
    // casts are unchecked (the erased boundary the API contract centralises in Option.displayName)
    // but always safe: the value was produced by the same option's getter or factory.

    private static boolean currentBoolean(Option<?> option) {
        Object v = option.binding.getter().get();
        return v instanceof Boolean b && b;
    }

    private static boolean defaultBoolean(Option<?> option) {
        Object v = option.defaultValue.get();
        return v instanceof Boolean b && b;
    }

    private static void writeBoolean(Option<?> option, boolean v) {
        ((Consumer<Object>) option.binding.setter()).accept(v);
    }

    private static double readValue(Option<?> option) {
        Object v = option.binding.getter().get();
        return v instanceof Number n ? n.doubleValue() : 0.0;
    }

    private static double readDefault(Option<?> option) {
        Object v = option.defaultValue.get();
        return v instanceof Number n ? n.doubleValue() : 0.0;
    }

    private static void writeValue(Option<?> option, double v) {
        Object value = option.kind == OptionKind.INT_SLIDER || option.kind == OptionKind.BLOCK_SLIDER
                ? (int) Math.round(v)
                : (float) v;
        ((Consumer<Object>) option.binding.setter()).accept(value);
    }

    private static Enum<?> currentEnum(Option<?> option) {
        Object v = option.binding.getter().get();
        return v instanceof Enum<?> e ? e : null;
    }

    private static Enum<?> defaultEnum(Option<?> option) {
        Object v = option.defaultValue.get();
        return v instanceof Enum<?> e ? e : null;
    }

    private static void advanceEnum(Option<?> option) {
        Enum<?>[] values = option.enumType.getEnumConstants();
        Enum<?> cur = currentEnum(option);
        int next = (cur == null ? -1 : cur.ordinal()) + 1;
        writeEnum(option, values[next % values.length]);
    }

    private static void writeEnum(Option<?> option, Enum<?> value) {
        ((Consumer<Object>) option.binding.setter()).accept(value);
    }

    private static String currentString(Option<?> option) {
        Object v = option.binding.getter().get();
        return v instanceof String s ? s : "";
    }

    private static String defaultString(Option<?> option) {
        Object v = option.defaultValue.get();
        return v instanceof String s ? s : "";
    }

    private static void advanceString(Option<?> option) {
        List<String> values = option.stringValues;
        String cur = currentString(option);
        int i = values.indexOf(cur);
        writeString(option, values.get((i + 1 + values.size()) % values.size()));
    }

    private static void writeString(Option<?> option, String value) {
        ((Consumer<Object>) option.binding.setter()).accept(value);
    }

    private static int currentColor(Option<?> option) {
        Object v = option.binding.getter().get();
        return v instanceof Integer i ? i : 0;
    }

    private static int defaultColor(Option<?> option) {
        Object v = option.defaultValue.get();
        return v instanceof Integer i ? i : 0;
    }

    private static void writeColor(Option<?> option, int v) {
        ((Consumer<Object>) option.binding.setter()).accept(v);
    }

    private static InputConstants.Key currentKey(Option<?> option) {
        Object v = option.binding.getter().get();
        return v instanceof InputConstants.Key k ? k : null;
    }

    private static InputConstants.Key defaultKey(Option<?> option) {
        Object v = option.defaultValue.get();
        return v instanceof InputConstants.Key k ? k : null;
    }

    private static void writeKey(Option<?> option, InputConstants.Key k) {
        ((Consumer<Object>) option.binding.setter()).accept(k);
    }
}