package dev.dragonofshuu.shuusxaeroworldborder.overlays.misc;

/**
 * A marching-ants dash pattern: white dashes that scroll along a line.
 *
 * <p>Used on the world map to mark the border a moving world border is heading for, which is why it
 * is white and deliberately high contrast - it has to stay readable on top of the coloured border
 * hatching.</p>
 *
 * <p>The dashes are laid out on a grid anchored to the start of each edge, so a line only needs its
 * own length and the current {@link #offset(long) offset} to be drawn. Callers open a
 * {@link GuiQuadBatch}, emit as many edges as they like, then close it; the whole pattern is one
 * draw call no matter how many dashes fit.</p>
 */
public final class AntlinePattern {
    /** Length of a single dash, in GUI pixels. */
    public static final int DASH_LENGTH = 4;
    /** Gap between two consecutive dashes, in GUI pixels. */
    public static final int GAP_LENGTH = 4;
    /** Thickness of the dashed line, in GUI pixels. */
    public static final int WIDTH = 2;
    /** Opaque white. */
    public static final int COLOR = 0xFFFFFFFF;
    /** Milliseconds per pixel of scroll, so one full dash plus gap cycle takes eight times this. */
    public static final long STEP_MS = 60L;

    /** Scroll offset for this frame, in GUI pixels along the line, folded into one dash cycle. */
    public static long offset(long millis) {
        return Math.floorMod(millis / STEP_MS, DASH_LENGTH + GAP_LENGTH);
    }

    /**
     * Adds one horizontal run of dashes spanning {@code [x0, x1)} with its top edge at {@code y}.
     * Spans that are empty or entirely off the drawn area add nothing.
     */
    public static void horizontal(GuiQuadBatch batch, float x0, float x1, float y, long offset) {
        if (x1 <= x0) {
            return;
        }
        int period = DASH_LENGTH + GAP_LENGTH;
        // Starting a full period early guarantees the dash overlapping x0 is not skipped.
        for (float d = x0 - period + offset; d < x1; d += period) {
            float start = Math.max(d, x0);
            float end = Math.min(d + DASH_LENGTH, x1);
            if (end > start) {
                batch.rect(start, y, end, y + WIDTH, COLOR);
            }
        }
    }

    /**
     * Adds one vertical run of dashes spanning {@code [y0, y1)} with its left edge at {@code x}.
     */
    public static void vertical(GuiQuadBatch batch, float y0, float y1, float x, long offset) {
        if (y1 <= y0) {
            return;
        }
        int period = DASH_LENGTH + GAP_LENGTH;
        for (float d = y0 - period + offset; d < y1; d += period) {
            float start = Math.max(d, y0);
            float end = Math.min(d + DASH_LENGTH, y1);
            if (end > start) {
                batch.rect(x, start, x + WIDTH, end, COLOR);
            }
        }
    }
}
