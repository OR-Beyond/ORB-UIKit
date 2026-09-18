package dev.orbeyond.uikit.api;

/**
 * The widget family an {@link Option} renders as. The binder switches on this to build the row.
 */
public enum OptionKind {
    TOGGLE,
    INT_SLIDER,
    FLOAT_SLIDER,
    BLOCK_SLIDER,
    ENUM_CYCLER,
    ENUM_DROPDOWN,
    STRING_CYCLER,
    STRING_DROPDOWN,
    COLOR,
    KEYBIND
}