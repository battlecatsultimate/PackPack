package mandarin.packpack.supporter.calculation;

import javax.annotation.Nonnull;

public class Variable extends Element {
    @Nonnull
    public final String name;

    public Variable(@Nonnull String name) {
        super(false);

        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
