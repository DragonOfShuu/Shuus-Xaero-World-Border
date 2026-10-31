package dev.dragonofshuu.shuusxaeroworldborder.overlays;

import dev.dragonofshuu.shuusxaeroworldborder.OverlayConstants;
import dev.dragonofshuu.shuusxaeroworldborder.overlays.misc.GuiQuadBatch;
import dev.dragonofshuu.shuusxaeroworldborder.overlays.misc.WorldBorderState;
import net.minecraft.client.gui.GuiGraphics;

/**
 * World border overlay for the minimap.
 *
 * <p>The minimap can be round and can be rotated with the player, so the border is not an axis
 * aligned rectangle here the way it is on the world map. Rather than try to express that as GPU
 * geometry, the exact per pixel mask (on the minimap shape, outside the border) is evaluated on the
 * CPU where it costs nothing, then collapsed into horizontal runs so each run becomes a single quad
 * in one shared batch.</p>
 */
public class MinimapBorderOverlay {
    /** Thickness of the border outlines on the minimap, in device pixels. */
    private static final float OUTLINE_WIDTH = 3.0f;
    /** Thickness of the line drawn from the player to the border, in device pixels. */
    private static final float PLAYER_LINE_WIDTH = 3.0f;

    /**
     * Diagonal hatching covering every minimap pixel that falls outside of the border: a flat
     * underlay across the whole area, with the stripes on top of it.
     *
     * <p>Both layers come out of a single scan. The shape and border tests are the only expensive
     * part of a pixel and they run once, rather than once per layer as two separate scans would do,
     * and everything goes into one batch. Submission order is the z-order here: in any row the
     * underlay run is queued before the stripes sitting on it and runs in a row never overlap, so
     * the stripes always land on top without needing a second pass.</p>
     */
    public static void drawStripedOutsideBorder(GuiGraphics guiGraphics, MinimapBorderOverlayContext context) {
        // Nothing to hatch once the border lies beyond the minimap, which is the common case. Worth
        // testing up front, so that case skips the scan entirely instead of running it to empty.
        if (borderCoversMinimap(context)) {
            return;
        }

        // Anything Xaero queued through GuiGraphics has to hit the GPU before our batch does.
        guiGraphics.flush();

        int stripeColor = OverlayConstants.getStripeColor(context.border);
        int underlayColor = OverlayConstants.getStripeUnderlayColor(context.border);

        GuiQuadBatch batch = new GuiQuadBatch(guiGraphics);
        batch.begin();
        for (int pixelY = 0; pixelY < context.displayedMapSize; pixelY++) {
            fillRow(batch, pixelY, context, stripeColor, underlayColor);
        }
        batch.end();
    }

    /** Solid line following the border, drawn on it and trimmed to the minimap shape. */
    public static void drawWorldBorderOutline(GuiGraphics guiGraphics, MinimapBorderOverlayContext context) {
        WorldBorderState border = context.border;
        drawBorderRectOutline(
                guiGraphics,
                context,
                border.minX, border.maxX, border.minZ, border.maxZ,
                OverlayConstants.getBorderOutlineColor(border));
    }

    /**
     * Solid white line following the border a moving border is heading for. This is the minimap
     * counterpart of the world map's marching ants, which the rotating map makes impractical here.
     */
    public static void drawTargetBorderOutline(GuiGraphics guiGraphics, MinimapBorderOverlayContext context) {
        WorldBorderState border = context.border;
        if (!border.isMoving()) {
            return;
        }
        drawBorderRectOutline(
                guiGraphics,
                context,
                border.targetMinX, border.targetMaxX, border.targetMinZ, border.targetMaxZ,
                OverlayConstants.DESTINATION_COLOR);
    }

    /**
     * Strokes the outline of the world space rectangle given by {@code minX} to {@code maxZ}.
     *
     * <p>Stroked rather than derived from the per pixel mask, for two reasons. A stroked edge can be
     * a single device pixel thick whatever the GUI scale, which the one-minimap-pixel granularity of
     * the mask could never reach; and it follows the border exactly instead of stepping along the
     * pixel grid, so a rotated border comes out as a clean diagonal rather than a staircase.</p>
     *
     * <p>The rectangle is axis aligned in the world but not on the minimap, which rotates, so its
     * four corners are projected individually and the edges drawn between them.</p>
     */
    private static void drawBorderRectOutline(
            GuiGraphics guiGraphics,
            MinimapBorderOverlayContext context,
            double minX,
            double maxX,
            double minZ,
            double maxZ,
            int color) {
        float[] cornerX = {
                toScreenX(minX, minZ, context), toScreenX(minX, maxZ, context),
                toScreenX(maxX, maxZ, context), toScreenX(maxX, minZ, context),
        };
        float[] cornerY = {
                toScreenY(minX, minZ, context), toScreenY(minX, maxZ, context),
                toScreenY(maxX, maxZ, context), toScreenY(maxX, minZ, context),
        };
        float width = devicePixels(OUTLINE_WIDTH, context);

        // Anything Xaero or an earlier layer queued has to hit the GPU before this batch does.
        guiGraphics.flush();

        GuiQuadBatch batch = new GuiQuadBatch(guiGraphics);
        batch.begin();
        for (int corner = 0; corner < 4; corner++) {
            int next = (corner + 1) % 4;
            addEdgeInsideShape(batch, context,
                    cornerX[corner], cornerY[corner], cornerX[next], cornerY[next], width, color);
        }
        batch.end();
    }

    /**
     * Adds one edge of a border rectangle, shortened to the part still inside the minimap.
     *
     * <p>The scissor Xaero sets is a square, so on a round minimap an edge running past the map would
     * otherwise be drawn out into the corners. A straight segment meets a circle in at most two
     * places, so whatever survives is always a single shorter segment, found by solving the usual
     * quadratic for {@code |start + t * direction| = radius} and clamping the roots to the segment.</p>
     */
    private static void addEdgeInsideShape(
            GuiQuadBatch batch,
            MinimapBorderOverlayContext context,
            float x0,
            float y0,
            float x1,
            float y1,
            float width,
            int color) {
        if (!context.circleShape) {
            batch.line(x0, y0, x1, y1, width, color);
            return;
        }

        float dx = x1 - x0;
        float dy = y1 - y0;
        float a = dx * dx + dy * dy;
        if (a <= 0.0f) {
            return;
        }

        float fx = x0 - centreX(context);
        float fy = y0 - centreY(context);
        float radius = circleReach(context);
        float b = 2.0f * (fx * dx + fy * dy);
        float c = fx * fx + fy * fy - radius * radius;

        float discriminant = b * b - 4.0f * a * c;
        if (discriminant <= 0.0f) {
            return; // misses the circle entirely, or only grazes it
        }

        float spread = (float) Math.sqrt(discriminant);
        float from = Math.max(0.0f, (-b - spread) / (2.0f * a));
        float to = Math.min(1.0f, (-b + spread) / (2.0f * a));
        if (to > from) {
            batch.line(x0 + dx * from, y0 + dy * from, x0 + dx * to, y0 + dy * to, width, color);
        }
    }

    /**
     * A solid line from the player, at the centre of the minimap, to the border the player is
     * currently outside of, in the same colour as the border outline.
     */
    public static void drawPlayerToBorderLine(GuiGraphics guiGraphics, MinimapBorderOverlayContext context) {
        WorldBorderState border = context.border;
        drawPlayerToRectLine(
                guiGraphics,
                context,
                border.minX, border.maxX, border.minZ, border.maxZ,
                OverlayConstants.getBorderOutlineColor(border));
    }

    /**
     * A solid white line from the player to the border the player is moving towards, drawn only while
     * the player is outside of it. The minimap counterpart of the world map's destination line.
     */
    public static void drawPlayerToTargetLine(GuiGraphics guiGraphics, MinimapBorderOverlayContext context) {
        WorldBorderState border = context.border;
        if (!border.isMoving()) {
            return;
        }
        drawPlayerToRectLine(
                guiGraphics,
                context,
                border.targetMinX, border.targetMaxX, border.targetMinZ, border.targetMaxZ,
                OverlayConstants.DESTINATION_COLOR);
    }

    /**
     * Draws the straight segment from the player to the nearest edge of the world space rectangle
     * given by {@code minX} to {@code maxZ}, and nothing at all when the player is already inside it.
     *
     * <p>The nearest point is found in world space, where the rectangle is axis aligned, and only then
     * converted to screen space. The minimap transform is a rotation plus a uniform scale, so the
     * segment survives the trip as the same straight shortest path, simply rotated with the map.
     * Clamping to the rectangle is exactly that nearest point, so the line cannot overshoot the
     * edge.</p>
     */
    private static void drawPlayerToRectLine(
            GuiGraphics guiGraphics,
            MinimapBorderOverlayContext context,
            double minX,
            double maxX,
            double minZ,
            double maxZ,
            int color) {
        double playerX = context.playerX;
        double playerZ = context.playerZ;
        if (playerX >= minX && playerX <= maxX && playerZ >= minZ && playerZ <= maxZ) {
            return;
        }

        // Anything Xaero or an earlier layer queued has to hit the GPU before this batch does.
        guiGraphics.flush();

        double edgeX = Math.min(Math.max(playerX, minX), maxX);
        double edgeZ = Math.min(Math.max(playerZ, minZ), maxZ);

        float startX = toScreenX(playerX, playerZ, context);
        float startY = toScreenY(playerX, playerZ, context);
        float endX = toScreenX(edgeX, edgeZ, context);
        float endY = toScreenY(edgeX, edgeZ, context);

        // The scissor is a square, so on a round minimap the line has to be pulled back inside the
        // circle itself whenever the border is further out than the minimap reaches.
        if (context.circleShape) {
            float dx = endX - startX;
            float dy = endY - startY;
            float length = (float) Math.sqrt(dx * dx + dy * dy);
            float reach = circleReach(context);
            if (length > reach && length > 0.0f) {
                float scale = reach / length;
                endX = startX + dx * scale;
                endY = startY + dy * scale;
            }
        }

        GuiQuadBatch batch = new GuiQuadBatch(guiGraphics);
        batch.begin();
        batch.line(startX, startY, endX, endY, devicePixels(PLAYER_LINE_WIDTH, context), color);
        batch.end();
    }

    /**
     * True when the border lies beyond the edge of the minimap, so no pixel of the minimap falls
     * outside it and there is nothing to hatch.
     *
     * <p>The minimap transform is a rotation about the player plus a uniform scale, so every pixel
     * lands within {@code logicalHalf * blocksPerPixel} blocks of the player on each axis, grown by
     * {@code |ps| + |pc|} to cover a square minimap at any rotation. A border at least that far out
     * on all four sides therefore hides nothing, and the whole per pixel scan can be skipped.</p>
     */
    private static boolean borderCoversMinimap(MinimapBorderOverlayContext context) {
        double blocksPerPixel = 2.0 * context.mapScale / context.zoom;
        double reach =
                context.logicalHalf * blocksPerPixel * (Math.abs(context.ps) + Math.abs(context.pc));
        WorldBorderState border = context.border;
        return border.minX <= context.playerX - reach
                && border.maxX >= context.playerX + reach
                && border.minZ <= context.playerZ - reach
                && border.maxZ >= context.playerZ + reach;
    }

    /**
     * Scans one row for pixels outside of the border and collapses each stretch of them into a
     * single run, so a row of hatching costs a handful of quads instead of one per pixel.
     */
    private static void fillRow(
            GuiQuadBatch batch,
            int pixelY,
            MinimapBorderOverlayContext context,
            int stripeColor,
            int underlayColor) {
        int width = context.displayedMapSize;
        int pixelX = 0;
        int runStart = -1;

        while (pixelX < width) {
            if (isOutsideBorderPixel(pixelX, pixelY, context)) {
                if (runStart < 0) {
                    runStart = pixelX;
                }
            } else if (runStart >= 0) {
                addRun(batch, runStart, pixelX, pixelY, context, stripeColor, underlayColor);
                runStart = -1;
            }
            pixelX++;
        }

        if (runStart >= 0) {
            addRun(batch, runStart, width, pixelY, context, stripeColor, underlayColor);
        }
    }

    /**
     * Queues one run of hatching: the flat underlay first, then the stripes sitting on it.
     *
     * <p>The stripes are walked in whole-gap steps with {@link OverlayConstants#stripeRunLength}
     * rather than pixel by pixel, so each gap costs one step instead of a test per pixel.</p>
     */
    private static void addRun(
            GuiQuadBatch batch,
            int fromX,
            int toX,
            int pixelY,
            MinimapBorderOverlayContext context,
            int stripeColor,
            int underlayColor) {
        batch.rect(
                context.mapLeft + fromX,
                context.mapTop + pixelY,
                context.mapLeft + toX,
                context.mapTop + pixelY + 1,
                underlayColor);

        long phase = context.border.stripePhase;
        int pixelX = fromX;
        while (pixelX < toX) {
            int runEnd = Math.min(
                    pixelX + OverlayConstants.stripeRunLength(pixelX, pixelY, phase), toX);
            if (OverlayConstants.isOnStripe(pixelX + pixelY, phase)) {
                batch.rect(
                        context.mapLeft + pixelX,
                        context.mapTop + pixelY,
                        context.mapLeft + runEnd,
                        context.mapTop + pixelY + 1,
                        stripeColor);
            }
            pixelX = runEnd;
        }
    }

    /** True when the pixel is on the minimap and outside of the border - the hatched area. */
    private static boolean isOutsideBorderPixel(int pixelX, int pixelY, MinimapBorderOverlayContext context) {
        return isPixelOnMinimapShape(pixelX, pixelY, context)
                && isOutsideWorldBorder(pixelX, pixelY, context);
    }

    private static boolean isPixelOnMinimapShape(int pixelX, int pixelY, MinimapBorderOverlayContext context) {
        if (!context.circleShape) {
            return true;
        }
        double offX = toLogicalOffsetX(pixelX, context);
        double offY = toLogicalOffsetY(pixelY, context);
        return offX * offX + offY * offY <= context.logicalHalf * context.logicalHalf;
    }

    private static boolean isOutsideWorldBorder(int pixelX, int pixelY, MinimapBorderOverlayContext context) {
        return context.border.isOutside(toWorldX(pixelX, pixelY, context), toWorldZ(pixelX, pixelY, context));
    }

    private static double toWorldX(int pixelX, int pixelY, MinimapBorderOverlayContext context) {
        double offX = toLogicalOffsetX(pixelX, context);
        double offY = toLogicalOffsetY(pixelY, context);
        double blockOffX = offX * 2.0 * context.mapScale / context.zoom;
        double blockOffY = offY * 2.0 * context.mapScale / context.zoom;
        return context.playerX + context.ps * blockOffX + context.pc * blockOffY;
    }

    private static double toWorldZ(int pixelX, int pixelY, MinimapBorderOverlayContext context) {
        double offX = toLogicalOffsetX(pixelX, context);
        double offY = toLogicalOffsetY(pixelY, context);
        double blockOffX = offX * 2.0 * context.mapScale / context.zoom;
        double blockOffY = offY * 2.0 * context.mapScale / context.zoom;
        return context.playerZ + context.ps * blockOffY - context.pc * blockOffX;
    }

    private static double toLogicalOffsetX(int pixelX, MinimapBorderOverlayContext context) {
        double logicalX = (pixelX + 0.5) * context.logicalMapSize / context.displayedMapSize;
        return logicalX - context.logicalHalf;
    }

    private static double toLogicalOffsetY(int pixelY, MinimapBorderOverlayContext context) {
        double logicalY = (pixelY + 0.5) * context.logicalMapSize / context.displayedMapSize;
        return logicalY - context.logicalHalf;
    }

    /**
     * Converts a size given in device pixels into GUI pixels for this frame. Everything on screen is
     * measured in GUI pixels, and a GUI pixel is {@code guiScale} device pixels wide, so dividing by
     * the GUI scale keeps a line exactly as many real pixels thick however the GUI scale is set.
     */
    private static float devicePixels(float amount, MinimapBorderOverlayContext context) {
        return (float) (amount / Math.max(1.0, context.guiScale));
    }

    /** Screen pixel column at the centre of the minimap, which is also where the player is drawn. */
    private static float centreX(MinimapBorderOverlayContext context) {
        return toScreenX(context.playerX, context.playerZ, context);
    }

    /** Screen pixel row at the centre of the minimap. */
    private static float centreY(MinimapBorderOverlayContext context) {
        return toScreenY(context.playerX, context.playerZ, context);
    }

    /** Radius of a round minimap in screen pixels; meaningless when the minimap is square. */
    private static float circleReach(MinimapBorderOverlayContext context) {
        return (float) (context.logicalHalf * context.displayedMapSize / context.logicalMapSize);
    }

    /**
     * Screen pixel column of a world position, the exact inverse of {@link #toWorldX} and
     * {@link #toWorldZ}. Their rotation matrix has determinant {@code ps * ps + pc * pc = 1}, so
     * inverting it is just its transpose.
     */
    private static float toScreenX(double worldX, double worldZ, MinimapBorderOverlayContext context) {
        double dx = worldX - context.playerX;
        double dz = worldZ - context.playerZ;
        double blockOffX = context.ps * dx - context.pc * dz;
        double offX = blockOffX * context.zoom / (2.0 * context.mapScale);
        return context.mapLeft
                + (float) ((offX + context.logicalHalf) * context.displayedMapSize / context.logicalMapSize)
                - 0.5f;
    }

    /** Screen pixel row of a world position, the exact inverse of {@link #toWorldZ}. */
    private static float toScreenY(double worldX, double worldZ, MinimapBorderOverlayContext context) {
        double dx = worldX - context.playerX;
        double dz = worldZ - context.playerZ;
        double blockOffY = context.pc * dx + context.ps * dz;
        double offY = blockOffY * context.zoom / (2.0 * context.mapScale);
        return context.mapTop
                + (float) ((offY + context.logicalHalf) * context.displayedMapSize / context.logicalMapSize)
                - 0.5f;
    }
}
