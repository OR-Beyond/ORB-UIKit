# ORB-UIKit

ORB-UIKit is a client-side configuration screen library for Fabric mods, built on owo-ui. It offers a YACL-style fluent builder (config, category, group, option) that renders a brutalist screen: a left rail of category tabs, a slide-in drawer of collapsible option groups, and a Save/Cancel state pipeline with dirty tracking and full rollback. The design system is extracted 1:1 from the Vantage (Shader-Mod) configuration UI by TylerBits and redistributed here under MIT with attribution. The library itself declares no fabric-api dependency. It is a UI library; consumers bring their own fabric-api if they want keybinding or tick conveniences.

Toolchain: Minecraft 1.21.11, Mojang official mappings, Java 21, fabric-loader 0.19.4, loom 1.17.20, owo-lib 0.13.0+1.21.11. Coordinate: `dev.orbeyond:orb-uikit:0.1.0` (mod id `orb-uikit`).

## Features

- YACL-style fluent builder: `UikitConfig.create` -> `title` -> `category` -> `group` -> `option`.
- 10 option kinds: toggle, int slider, float slider, block slider, enum cycler, enum dropdown, string cycler, string dropdown, color, keybind.
- Brutalist theme extracted 1:1 from Vantage (Shader-Mod) by TylerBits; every visual constant lives in one `UikitTheme` record.
- Independent UI scaling: `targetGuiScale` keeps the workspace compact at any Minecraft GUI scale, and never enlarges it.
- Slide-in/out panel animation with configurable timings.
- Dirty tracking with Save/Cancel and full rollback to a baseline snapshot.
- Rail + drawer layout with optional per-category 16px icons; text tabs when icons are omitted.
- Built on owo-lib; no fabric-api dependency.
- Client-only library.

## Installation

### Composite build (dev against a checkout)

In the consumer's `settings.gradle`:

```groovy
includeBuild("/path/to/ORB-UIKit")
```

Then in `build.gradle`:

```groovy
dependencies {
    modImplementation("dev.orbeyond:orb-uikit:0.1.0")
}
```

Requires Gradle 9.x and loom 1.17.20 in the consumer.

### Maven coordinate (once published)

```groovy
dependencies {
    modImplementation("dev.orbeyond:orb-uikit:0.1.0")
}
```

owo-lib is a transitive dependency, so consumers must also add the Wisp Forest maven, plus Shedaniel's maven for cloth-config and JitPack for kdl4j, exactly as the library's own `build.gradle` declares:

```groovy
repositories {
    maven { name = 'Shedaniel'; url = 'https://maven.shedaniel.me/' }
    maven { name = 'Wisp Forest'; url = 'https://maven.wispforest.io/releases' }
    maven {
        name = 'JitPack'
        url = 'https://jitpack.io'
        content { includeGroup 'com.github.kdl-org' }
    }
}
```

The mod metadata declares `fabricloader >=0.19.0`, `minecraft ~1.21.11`, `java >=21`, and `owo >=0.13.0`.

## Quick start

```java
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import dev.orbeyond.uikit.api.Category;
import dev.orbeyond.uikit.api.Option;
import dev.orbeyond.uikit.api.OptionBinding;
import dev.orbeyond.uikit.api.OptionGroup;
import dev.orbeyond.uikit.api.UikitConfig;

UikitConfig config = UikitConfig.create(builder -> builder
        .title(Component.literal("My Mod"))
        .category(Category.builder(Component.literal("General"))
                .group(OptionGroup.builder(Component.literal("Rendering"))
                        .option(Option.toggle(Component.literal("Enabled"),
                                new OptionBinding<>(() -> MyConfig.enabled, v -> MyConfig.enabled = v),
                                () -> true))
                        .option(Option.intSlider(Component.literal("Render Distance"),
                                2, 32,
                                new OptionBinding<>(() -> MyConfig.renderDistance, v -> MyConfig.renderDistance = v),
                                () -> 8))
                        .option(Option.dropdownEnum(Component.literal("Quality"),
                                Quality.class, q -> Component.literal(q.name()),
                                new OptionBinding<>(() -> MyConfig.quality, v -> MyConfig.quality = v),
                                () -> Quality.MEDIUM))
                        .option(Option.color(Component.literal("Accent Color"),
                                new OptionBinding<>(() -> MyConfig.accentColor, v -> MyConfig.accentColor = v),
                                () -> 0x6E6E6E))
                        .build())
                .build())
        .saveConsumer(() -> MyConfig.save())
        .cancelConsumer(() -> MyConfig.load()));

Screen screen = config.generateScreen(parent);
```

## Option reference

All 10 factories. Parameter order is consistent: display name first, then kind-specific arguments, then the `OptionBinding`, then the default supplier.

| Factory | Signature |
|---|---|
| `toggle` | `Option<Boolean> toggle(Component name, OptionBinding<Boolean> binding, Supplier<Boolean> defaultValue)` |
| `intSlider` | `Option<Integer> intSlider(Component name, int min, int max, OptionBinding<Integer> binding, Supplier<Integer> defaultValue)` |
| `blockSlider` | `Option<Integer> blockSlider(Component name, int min, int max, OptionBinding<Integer> binding, Supplier<Integer> defaultValue)` |
| `floatSlider` | `Option<Float> floatSlider(Component name, float min, float max, int decimals, OptionBinding<Float> binding, Supplier<Float> defaultValue)` |
| `cyclingEnum` | `<T extends Enum<T>> Option<T> cyclingEnum(Component name, Class<T> enumType, Function<T, Component> display, OptionBinding<T> binding, Supplier<T> defaultValue)` |
| `dropdownEnum` | `<T extends Enum<T>> Option<T> dropdownEnum(Component name, Class<T> enumType, Function<T, Component> display, OptionBinding<T> binding, Supplier<T> defaultValue)` |
| `cyclingString` | `Option<String> cyclingString(Component name, List<String> values, Function<String, Component> display, OptionBinding<String> binding, Supplier<String> defaultValue)` |
| `dropdownString` | `Option<String> dropdownString(Component name, List<String> values, Function<String, Component> display, OptionBinding<String> binding, Supplier<String> defaultValue)` |
| `color` | `Option<Integer> color(Component name, OptionBinding<Integer> binding, Supplier<Integer> defaultValue)` |
| `keybind` | `Option<InputConstants.Key> keybind(Component name, OptionBinding<InputConstants.Key> binding, Supplier<InputConstants.Key> defaultValue)` |

Notes:

- The block slider readout shows "N b" (a narrow no-break space before the b).
- Float slider `decimals` map to a step size of `10^-decimals`.
- Color is a packed `0xRRGGBB` int.
- Keybind values are `InputConstants.Key`.
- The enum and string variants take a display `Function<value, Component>` that renders the current value in the widget.
- A cycler and a dropdown may intentionally share one `OptionBinding`: both read the live getter, so they stay in sync. The demo exercises this.

## Group master toggle

`OptionGroup.builder(...).withToggle(OptionBinding<Boolean>)` renders a checkbox in the accordion header that drives the master boolean. Groups are collapsible.

```java
OptionGroup.builder(Component.literal("Rendering"))
        .withToggle(new OptionBinding<>(() -> MyConfig.renderingEnabled, v -> MyConfig.renderingEnabled = v))
        .option(Option.toggle(...))
        .build();
```

## Theme customization

`UikitTheme` is a record of 27 fields. Colors are packed ARGB ints, sizes are pixels, timings are milliseconds. The values in `UikitTheme.DEFAULT` are extracted verbatim from the Vantage (Shader-Mod) brutalist UI.

| Group | Fields |
|---|---|
| Colors (ARGB) | `panel`, `rail`, `drawer`, `drawerHeader`, `drawerFooter`, `collapsibleHeader`, `collapsibleBody`, `outline`, `sliderTrack`, `sliderFill`, `sliderThumb`, `hoverWash`, `menu`, `overlayDim`, `dirtyHint`, `marker` |
| Sizes (px) | `railWidth`, `drawerWidth`, `rowHeight`, `labelWidth`, `readoutWidth`, `swatchSize`, `sliderThumbWidth` |
| Scale | `targetGuiScale` (float) |
| Timings (ms) | `panelInMs`, `panelOutMs`, `markerMs` |

Pass a custom theme through the config builder:

```java
UikitConfig.create(builder -> builder
        .theme(theme)
        .title(Component.literal("My Mod"))
        .category(...)
        .build());
```

Start from `UikitTheme.DEFAULT` as the base: copy its values and change only what you need. The record has no copy-with, so a custom theme is a full 27-argument constructor call.

## Migrating from YACL

| YACL | ORB-UIKit |
|---|---|
| `YetAnotherConfigLib.createBuilder().title().category()` | `UikitConfig.create(...).title().category()` |
| `Option.createBuilder(T).name().binding().getDefault()` | the per-kind `Option.<kind>(...)` factories |
| `OptionBinding<T>` | `OptionBinding<T>` (same shape: `(Supplier<T> getter, Consumer<T> setter)`) |
| `Option.toggle` / `booleanSlider` | `Option.toggle` |
| `Option.intSlider` / `floatSlider` | `Option.intSlider` / `Option.floatSlider` |
| `enumDropdown` / `enumCycling` | `Option.dropdownEnum` / `Option.cyclingEnum` |
| `stringDropdown` / `stringCycling` | `Option.dropdownString` / `Option.cyclingString` |
| `Option.color` | `Option.color` (packed `0xRRGGBB`) |
| `keyBindAction` | `Option.keybind` (`InputConstants.Key` value) |
| `OptionGroup` (CyclicListController-style master toggle) | `OptionGroup.builder().withToggle(...)` |
| Screen generation | `UikitConfig.generateScreen(parent)` |

Note: ORB-UIKit puts the display name FIRST and the binding/default LAST, unlike YACL's convenience factories.

## Demo

The `example/` directory is a separate composite Gradle build that consumes this library and is the canonical full example. Run it with:

```
cd example && JAVA_HOME=<a JDK 21> PATH=... ../gradlew runClient
```

Press G in-game to open the screen. It demonstrates two categories (one icon tab, one text tab), a group master toggle, all 10 option kinds, and Gson persistence at `config/orb-uikit-example.json`.

## Credits and license

MIT. Copyright (c) 2026 OR-Beyond. Design and code derived from the configuration UI of Shader-Mod (Vantage) by TylerBits, also MIT. UI framework: owo-lib by Wisp Forest.