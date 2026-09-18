package dev.orbeyond.uikit.api;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * One rail tab and its drawer panel. The optional {@link Identifier} icon is a 16px texture
 * rendered in the 44x40 rail tab; when null the tab shows the category name as text.
 */
public final class Category {

    public final Component name;
    public final Identifier icon;
    public final List<OptionGroup> groups;

    private Category(Component name, Identifier icon, List<OptionGroup> groups) {
        this.name = name;
        this.icon = icon;
        this.groups = groups;
    }

    public static Builder builder(Component name) {
        return new Builder(name);
    }

    public static final class Builder {
        private final Component name;
        private Identifier icon;
        private final List<OptionGroup> groups = new ArrayList<>();

        private Builder(Component name) {
            this.name = name;
        }

        /** The 16px rail-tab texture; omit for a text tab. */
        public Builder icon(Identifier icon) {
            this.icon = icon;
            return this;
        }

        public Builder group(OptionGroup group) {
            this.groups.add(group);
            return this;
        }

        public Category build() {
            return new Category(this.name, this.icon, List.copyOf(this.groups));
        }
    }
}