package dev.orbeyond.uikit.api;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.chat.Component;

/**
 * A named accordion group of options inside a {@link Category}. Optionally carries an
 * {@link OptionBinding} for a master enable/disable boolean, rendered as a checkbox in the group's
 * header (a group whose header checkbox drives a master boolean).
 */
public final class OptionGroup {

    public final Component name;
    public final List<Option<?>> options;
    public final OptionBinding<Boolean> enableToggle;

    private OptionGroup(Component name, List<Option<?>> options, OptionBinding<Boolean> enableToggle) {
        this.name = name;
        this.options = options;
        this.enableToggle = enableToggle;
    }

    public static Builder builder(Component name) {
        return new Builder(name);
    }

    public static final class Builder {
        private final Component name;
        private final List<Option<?>> options = new ArrayList<>();
        private OptionBinding<Boolean> enableToggle;

        private Builder(Component name) {
            this.name = name;
        }

        public Builder option(Option<?> option) {
            this.options.add(option);
            return this;
        }

        /** Give the group a header checkbox driving this master boolean. */
        public Builder withToggle(OptionBinding<Boolean> enableToggle) {
            this.enableToggle = enableToggle;
            return this;
        }

        public OptionGroup build() {
            return new OptionGroup(this.name, List.copyOf(this.options), this.enableToggle);
        }
    }
}