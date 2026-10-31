package dev.dragonofshuu.shuusxaeroworldborder.overlays.misc;

/**
 * Whether the mod draws its world border overlays at all, toggled from the world map icon.
 *
 * <p>Deliberately a plain static field that is never written to disk: the choice is meant to last
 * for the current game session and nothing more, so every new session starts with the overlays on.
 */
public final class WorldBorderVisibility {
    private static boolean visible = true;

    private WorldBorderVisibility() {
    }

    /** True while the overlays should be drawn, which is also when the icon shows its enabled glyph. */
    public static boolean isVisible() {
        return visible;
    }

    /** Flips the visibility, for the rest of the session. */
    public static void toggle() {
        visible = !visible;
    }
}
