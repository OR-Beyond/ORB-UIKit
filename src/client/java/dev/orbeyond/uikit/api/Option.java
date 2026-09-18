package dev.orbeyond.uikit.api;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.network.chat.Component;

/**
 * One configurable value: a widget kind, a display name, the binding it reads and writes, and the
 * default it resets to. Built exclusively through the static fluent factories below - the
 * constructor is private so every instance is well-formed for its kind.
 *
 * <p>Per-kind extras: slider kinds carry {@code min}/{@code max}/{@code decimals}; enum kinds carry
 * {@code enumType} plus a display function; string kinds carry {@code stringValues} plus a display
 * function. The display function is stored erased and re-typed by {@link #displayName()}.
 */
public final class Option<T> {

    public final OptionKind kind;
    public final Component name;
    public final OptionBinding<T> binding;
    public final Supplier<T> defaultValue;

    /** Slider kinds: the value range and the readout's decimal places. */
    public final double min;
    public final double max;
    public final int decimals;

    /** Enum kinds: the constant class the cycler/dropdown iterates. */
    public final Class<? extends Enum<?>> enumType;

    /** String kinds: the values the cycler/dropdown iterates. */
    public final List<String> stringValues;

    /** Enum/string kinds: how a value renders in the widget. Null for the other kinds. */
    private final Function<?, Component> display;

    private Option(OptionKind kind, Component name, OptionBinding<T> binding, Supplier<T> defaultValue,
                   double min, double max, int decimals, Class<? extends Enum<?>> enumType,
                   List<String> stringValues, Function<?, Component> display) {
        this.kind = kind;
        this.name = name;
        this.binding = binding;
        this.defaultValue = defaultValue;
        this.min = min;
        this.max = max;
        this.decimals = decimals;
        this.enumType = enumType;
        this.stringValues = stringValues;
        this.display = display;
    }

    /**
     * The display function, typed to whatever value the caller is about to pass in. This is the
     * single unchecked cast in the codebase: the stored function is {@code Function<T, Component>}
     * for this option's value type, and the binder only ever applies it to values of that type.
     */
    @SuppressWarnings("unchecked")
    public <X> Function<X, Component> displayName() {
        return (Function<X, Component>) this.display;
    }

    // --- factories ---------------------------------------------------------------------------

    public static Option<Boolean> toggle(Component name, OptionBinding<Boolean> binding, Supplier<Boolean> defaultValue) {
        return new Option<>(OptionKind.TOGGLE, name, binding, defaultValue, 0, 0, 0, null, null, null);
    }

    public static Option<Integer> intSlider(Component name, int min, int max, OptionBinding<Integer> binding, Supplier<Integer> defaultValue) {
        return new Option<>(OptionKind.INT_SLIDER, name, binding, defaultValue, min, max, 0, null, null, null);
    }

    /** An integer slider whose readout shows the value in blocks ("N b"). */
    public static Option<Integer> blockSlider(Component name, int min, int max, OptionBinding<Integer> binding, Supplier<Integer> defaultValue) {
        return new Option<>(OptionKind.BLOCK_SLIDER, name, binding, defaultValue, min, max, 0, null, null, null);
    }

    public static Option<Float> floatSlider(Component name, float min, float max, int decimals, OptionBinding<Float> binding, Supplier<Float> defaultValue) {
        return new Option<>(OptionKind.FLOAT_SLIDER, name, binding, defaultValue, min, max, decimals, null, null, null);
    }

    public static <T extends Enum<T>> Option<T> cyclingEnum(Component name, Class<T> enumType, Function<T, Component> display, OptionBinding<T> binding, Supplier<T> defaultValue) {
        return new Option<>(OptionKind.ENUM_CYCLER, name, binding, defaultValue, 0, 0, 0, enumType, null, display);
    }

    public static <T extends Enum<T>> Option<T> dropdownEnum(Component name, Class<T> enumType, Function<T, Component> display, OptionBinding<T> binding, Supplier<T> defaultValue) {
        return new Option<>(OptionKind.ENUM_DROPDOWN, name, binding, defaultValue, 0, 0, 0, enumType, null, display);
    }

    public static Option<String> cyclingString(Component name, List<String> values, Function<String, Component> display, OptionBinding<String> binding, Supplier<String> defaultValue) {
        return new Option<>(OptionKind.STRING_CYCLER, name, binding, defaultValue, 0, 0, 0, null, values, display);
    }

    public static Option<String> dropdownString(Component name, List<String> values, Function<String, Component> display, OptionBinding<String> binding, Supplier<String> defaultValue) {
        return new Option<>(OptionKind.STRING_DROPDOWN, name, binding, defaultValue, 0, 0, 0, null, values, display);
    }

    /** A packed {@code 0xRRGGBB} colour. */
    public static Option<Integer> color(Component name, OptionBinding<Integer> binding, Supplier<Integer> defaultValue) {
        return new Option<>(OptionKind.COLOR, name, binding, defaultValue, 0, 0, 0, null, null, null);
    }

    public static Option<InputConstants.Key> keybind(Component name, OptionBinding<InputConstants.Key> binding, Supplier<InputConstants.Key> defaultValue) {
        return new Option<>(OptionKind.KEYBIND, name, binding, defaultValue, 0, 0, 0, null, null, null);
    }
}