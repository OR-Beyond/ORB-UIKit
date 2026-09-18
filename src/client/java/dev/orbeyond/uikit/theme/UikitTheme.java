package dev.orbeyond.uikit.theme;

/**
 * Central theme for the ORB-UIKit brutalist widget set.
 *
 * <p>All colors are packed ARGB ints, all sizes are pixels, all timings are
 * milliseconds. The values below are extracted verbatim from the Vantage
 * (Shader-Mod) brutalist UI by tkedickson; the widget code in this package
 * reads every visual constant from this record so the look can be retuned in
 * one place.
 */
public record UikitTheme(
        int panel,
        int rail,
        int drawer,
        int drawerHeader,
        int drawerFooter,
        int collapsibleHeader,
        int collapsibleBody,
        int outline,
        int sliderTrack,
        int sliderFill,
        int sliderThumb,
        int hoverWash,
        int menu,
        int overlayDim,
        int dirtyHint,
        int marker,
        int railWidth,
        int drawerWidth,
        int rowHeight,
        int labelWidth,
        int readoutWidth,
        int swatchSize,
        int sliderThumbWidth,
        float targetGuiScale,
        int panelInMs,
        int panelOutMs,
        int markerMs
) {

    public static final UikitTheme DEFAULT = new UikitTheme(
            0xFF121212, // panel
            0xFF0E0E0E, // rail
            0xFF161616, // drawer
            0xFF1A1A1A, // drawerHeader
            0xFF1A1A1A, // drawerFooter
            0xFF1E1E1E, // collapsibleHeader
            0xFF141414, // collapsibleBody
            0xFF333333, // outline
            0xFF333333, // sliderTrack
            0xFF6E6E6E, // sliderFill
            0xFFFFFFFF, // sliderThumb
            0x18FFFFFF, // hoverWash
            0xFF1A1A1A, // menu
            0x88000000, // overlayDim
            0xFFD479,   // dirtyHint
            0xFFFFFFFF, // marker
            48,         // railWidth
            248,        // drawerWidth
            20,         // rowHeight
            96,         // labelWidth
            46,         // readoutWidth
            20,         // swatchSize
            4,          // sliderThumbWidth
            2.0f,       // targetGuiScale
            250,        // panelInMs
            220,        // panelOutMs
            160         // markerMs
    );
}