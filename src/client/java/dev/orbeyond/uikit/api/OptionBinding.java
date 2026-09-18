package dev.orbeyond.uikit.api;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The read/write pair an {@link Option} is bound to. The getter reads the live value; the setter
 * writes a new one. The binder calls the setter immediately on every widget change and the getter
 * whenever a widget needs re-seeding (Cancel, reset, tab switches).
 */
public record OptionBinding<T>(Supplier<T> getter, Consumer<T> setter) {
}