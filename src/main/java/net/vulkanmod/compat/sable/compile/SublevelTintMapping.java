package net.vulkanmod.compat.sable.compile;

public final class SublevelTintMapping {
    private SublevelTintMapping() {
    }

    public static Result unavailable(int sublevelColor) {
        return new Result(sublevelColor, Source.SUBLEVEL);
    }

    public static Result transformed(int mainWorldColor) {
        return new Result(mainWorldColor, Source.TRANSFORMED_MAIN_WORLD);
    }

    public enum Source {
        SUBLEVEL,
        TRANSFORMED_MAIN_WORLD
    }

    public record Result(int color, Source source) {
    }
}
