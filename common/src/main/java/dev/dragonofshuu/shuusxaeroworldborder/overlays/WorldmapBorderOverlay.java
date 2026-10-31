package dev.dragonofshuu.shuusxaeroworldborder.overlays;

import dev.dragonofshuu.shuusxaeroworldborder.OverlayConstants;
import dev.dragonofshuu.shuusxaeroworldborder.overlays.misc.AntlinePattern;
import dev.dragonofshuu.shuusxaeroworldborder.overlays.misc.GuiQuadBatch;
import dev.dragonofshuu.shuusxaeroworldborder.overlays.types.ScreenRect;
import net.minecraft.client.gui.GuiGraphics;

/**
 * World border overlay for the world map.
 *
 * <p>Same look as {@link MinimapBorderOverlay} — hatching outside the border, solid line on it —
 * but the world map covers the whole screen, so a per pixel approach would cost hundreds of
 * thousands of draw calls. The world map never rotates, so the axis aligned world border is an axis
 * aligned rectangle on screen and the area outside it splits into four rectangles. Each stripe is
 * then a single parallelogram, and the scissor box trims the stripes to the rectangle they belong
 * to.</p>
 */
public final class WorldmapBorderOverlay {
    /** Thickness of the lines drawn from the player to a border, in GUI pixels. */
    private static final float PLAYER_LINE_WIDTH = 2.0f;

    /**
     * Draws the whole overlay, back to front: hatching outside of the border, the border line on it,
     * the marching ants marking where a moving border is heading, then the two guide lines from the
     * player to whichever of those borders the player is outside of. Call this after the map tiles
     * and map elements, but before the screen widgets.
     */
    public static void draw(GuiGraphics guiGraphics, WorldmapBorderOverlayContext context) {
        // Everything Xaero queued through GuiGraphics has to hit the GPU before our batches do.
        guiGraphics.flush();

        drawStripedOutsideBorder(guiGraphics, context);
        drawWorldBorderOutline(guiGraphics, context);
        drawTargetAntlines(guiGraphics, context);
        drawPlayerToBorderLine(guiGraphics, context);
        drawPlayerToTargetLine(guiGraphics, context);
    }

    /**
     * Diagonal hatching covering every part of the screen that lies outside of the border: a flat
     * underlay over the whole area, with the stripes on top of it.
     */
    private static void drawStripedOutsideBorder(GuiGraphics guiGraphics, WorldmapBorderOverlayContext context) {
        int color = OverlayConstants.getStripeColor(context.border);
        long phase = OverlayConstants.stripePhaseOffset(context.border.stripePhase);

        ScreenRect rect = context.borderRect;
        int left = clampToRange(rect.left, context.screenWidth);
        int top = clampToRange(rect.top, context.screenHeight);
        int right = clampToRange(rect.right, context.screenWidth);
        int bottom = clampToRange(rect.bottom, context.screenHeight);

        int width = context.screenWidth;
        int height = context.screenHeight;

        drawUnderlay(guiGraphics, context, left, top, right, bottom);

        GuiQuadBatch batch = new GuiQuadBatch(guiGraphics);

        // The four rectangles that make up "screen minus border".
        drawStripes(batch, guiGraphics, 0, 0, width, top, color, phase);
        drawStripes(batch, guiGraphics, 0, bottom, width, height, color, phase);
        drawStripes(batch, guiGraphics, 0, top, left, bottom, color, phase);
        drawStripes(batch, guiGraphics, right, top, width, bottom, color, phase);
    }

    /**
     * Flat fill under the stripes, covering all of "screen minus border". The same four rectangles
     * the stripes are drawn in, so the two layers line up exactly; four quads in one batch.
     */
    private static void drawUnderlay(
            GuiGraphics guiGraphics,
            WorldmapBorderOverlayContext context,
            int left,
            int top,
            int right,
            int bottom) {
        int color = OverlayConstants.getStripeUnderlayColor(context.border);

        GuiQuadBatch batch = new GuiQuadBatch(guiGraphics);
        batch.begin();
        fillClamped(batch, context, 0, 0, context.screenWidth, top, color);
        fillClamped(batch, context, 0, bottom, context.screenWidth, context.screenHeight, color);
        fillClamped(batch, context, 0, top, left, bottom, color);
        fillClamped(batch, context, right, top, context.screenWidth, bottom, color);
        batch.end();
    }

    /** Solid line following the border, sitting just inside it. */
    private static void drawWorldBorderOutline(GuiGraphics guiGraphics, WorldmapBorderOverlayContext context) {
        int color = OverlayConstants.getBorderOutlineColor(context.border);
        int thickness = OverlayConstants.WORLDMAP_OUTLINE_WIDTH;

        ScreenRect rect = context.borderRect;
        float left = rect.left;
        float top = rect.top;
        float right = rect.right;
        float bottom = rect.bottom;

        GuiQuadBatch batch = new GuiQuadBatch(guiGraphics);
        batch.begin();
        fillClamped(batch, context, left, top, left + thickness, bottom, color);
        fillClamped(batch, context, right - thickness, top, right, bottom, color);
        fillClamped(batch, context, left, top, right, top + thickness, color);
        fillClamped(batch, context, left, bottom - thickness, right, bottom, color);
        batch.end();
    }

    /**
     * Marching ants tracing the border a moving border is growing or shrinking towards, drawn just
     * inside it the same way {@link #drawWorldBorderOutline} sits inside the current border.
     *
     * <p>Drawn last, and therefore on top of the hatching. That matters while the border is growing:
     * the destination lies outside the current border, so it would otherwise be buried under the
     * stripes covering that area.</p>
     */
    private static void drawTargetAntlines(GuiGraphics guiGraphics, WorldmapBorderOverlayContext context) {
        if (!context.border.isMoving()) {
            return;
        }

        ScreenRect target = context.targetRect;
        float left = clampAxis(target.left, context.screenWidth);
        float right = clampAxis(target.right, context.screenWidth);
        float top = clampAxis(target.top, context.screenHeight);
        float bottom = clampAxis(target.bottom, context.screenHeight);
        if (right <= left || bottom <= top) {
            return;
        }

        long offset = AntlinePattern.offset(System.currentTimeMillis());
        int thickness = AntlinePattern.WIDTH;

        GuiQuadBatch batch = new GuiQuadBatch(guiGraphics);
        batch.begin();
        AntlinePattern.horizontal(batch, left, right, top, offset);
        AntlinePattern.horizontal(batch, left, right, bottom - thickness, offset);
        AntlinePattern.vertical(batch, top, bottom, left, offset);
        AntlinePattern.vertical(batch, top, bottom, right - thickness, offset);
        batch.end();
    }

    /**
     * A solid line from the player to the border the player is currently outside of, in the same
     * colour as the border outline. Unlike the destination line this one does not need the border to
     * be moving, the current border always exists.
     */
    private static void drawPlayerToBorderLine(GuiGraphics guiGraphics, WorldmapBorderOverlayContext context) {
        drawPlayerToRectLine(
                guiGraphics,
                context,
                context.borderRect,
                OverlayConstants.getBorderOutlineColor(context.border));
    }

    /**
     * A solid white line from the player to the border the player is moving towards, drawn only
     * while the player is outside of that border.
     *
     * <p>Drawn after {@link #drawPlayerToBorderLine} so the white destination cue stays on top where
     * the two segments overlap on their way out of the same corner.</p>
     */
    private static void drawPlayerToTargetLine(GuiGraphics guiGraphics, WorldmapBorderOverlayContext context) {
        if (!context.border.isMoving()) {
            return;
        }
        drawPlayerToRectLine(guiGraphics, context, context.targetRect, OverlayConstants.DESTINATION_COLOR);
    }

    /**
     * Draws the straight segment from the player to the nearest point of {@code rect}, and nothing
     * at all when the player is already inside it.
     *
     * <p>Clamping the player to the rectangle is exactly its nearest point, so the segment is the
     * shortest way to that rectangle and cannot overshoot the edge - anything past it is already
     * inside.</p>
     *
     * <p>Drawn after the hatching and the border line, so both stay readable underneath it.</p>
     */
    private static void drawPlayerToRectLine(
            GuiGraphics guiGraphics,
            WorldmapBorderOverlayContext context,
            ScreenRect rect,
            int color) {
        if (context.player == null) {
            return;
        }

        float playerX = context.player.x;
        float playerY = context.player.y;

        boolean outside = playerX < rect.left || playerX > rect.right
                || playerY < rect.top || playerY > rect.bottom;
        if (!outside) {
            return;
        }

        float edgeX = Math.min(Math.max(playerX, rect.left), rect.right);
        float edgeY = Math.min(Math.max(playerY, rect.top), rect.bottom);

        GuiQuadBatch batch = new GuiQuadBatch(guiGraphics);
        batch.begin();
        batch.line(playerX, playerY, edgeX, edgeY, PLAYER_LINE_WIDTH, color);
        batch.end();
    }

    /** Adds a border edge, clipped to the screen so a border far off screen costs nothing. */
    private static void fillClamped(
            GuiQuadBatch batch,
            WorldmapBorderOverlayContext context,
            float x0,
            float y0,
            float x1,
            float y1,
            int color) {
        float cx0 = Math.max(Math.min(x0, x1), 0.0f);
        float cy0 = Math.max(Math.min(y0, y1), 0.0f);
        float cx1 = Math.min(Math.max(x0, x1), context.screenWidth);
        float cy1 = Math.min(Math.max(y0, y1), context.screenHeight);

        if (cx1 > cx0 && cy1 > cy0) {
            batch.rect(cx0, cy0, cx1, cy1, color);
        }
    }

    /**
     * Fills one rectangle of parallel stripes, covering exactly the bands that
     * {@link OverlayConstants#isOnStripe} calls a stripe: {@code x + y in [k * period - phase, k *
     * period - phase + width)} for every integer k.
     *
     * <p>Each band is drawn as a parallelogram stretched across the whole horizontal strip
     * {@code y in [y0, y1]}, which fully contains the part of the band that falls inside the
     * rectangle; the scissor box is what actually cuts it down to {@code [x0, x1] x [y0, y1]}.</p>
     */
    private static void drawStripes(
            GuiQuadBatch batch,
            GuiGraphics guiGraphics,
            int x0,
            int y0,
            int x1,
            int y1,
            int color,
            long phase) {
        if (x1 <= x0 || y1 <= y0) {
            return;
        }

        int period = OverlayConstants.STRIPE_PERIOD;
        int stripeWidth = OverlayConstants.STRIPE_WIDTH;
        long firstStripe = Math.floorDiv((long) x0 + y0 + phase - stripeWidth, (long) period);
        long lastStripe = Math.floorDiv((long) x1 + y1 + phase, (long) period) + 1;

        // The batch has to be submitted while the scissor is still active, so it opens and closes
        // inside it rather than around the whole overlay.
        guiGraphics.enableScissor(x0, y0, x1, y1);
        try {
            batch.begin();
            for (long stripe = firstStripe; stripe <= lastStripe; stripe++) {
                float start = stripe * period - phase;
                float end = start + stripeWidth;
                batch.quad(start - y0, y0, start - y1, y1, end - y1, y1, end - y0, y0, color);
            }
            batch.end();
        } finally {
            guiGraphics.disableScissor();
        }
    }

    /** Rounds a screen coordinate into {@code [0, max]}, keeping off-screen edges off-screen. */
    private static int clampToRange(float value, int max) {
        if (value <= 0.0f) {
            return 0;
        }
        if (value >= max) {
            return max;
        }
        return Math.round(value);
    }

    /** Clamps a screen coordinate into {@code [0, max]} without rounding. */
    private static float clampAxis(float value, int max) {
        return Math.min(Math.max(value, 0.0f), (float) max);
    }
}
