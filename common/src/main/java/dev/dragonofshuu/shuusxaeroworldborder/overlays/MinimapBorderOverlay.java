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
    /** Thickness of the line drawn from the player to the border, in GUI pixels. */
    private static final float PLAYER_LINE_WIDTH = 1.0f;

    /** Decides whether one pixel of the minimap belongs to the layer being drawn. */
    @FunctionalInterface
    private interface PixelFilter {
        boolean test(int pixelX, int pixelY, MinimapBorderOverlayContext context);
    }

    /** Diagonal hatching covering every minimap pixel that falls outside of the border. */
    public static void drawStripedOutsideBorder(GuiGraphics guiGraphics, MinimapBorderOverlayContext context) {
        fillRuns(guiGraphics, context, OverlayConstants.getStripeColor(context.border),
                MinimapBorderOverlay::isStripePixel, true);
    }

    /** Solid line following the border, drawn just inside it and clipped to the minimap shape. */
    public static void drawWorldBorderOutline(GuiGraphics guiGraphics, MinimapBorderOverlayContext context) {
        fillRuns(guiGraphics, context, OverlayConstants.getBorderOutlineColor(context.border),
                MinimapBorderOverlay::isOutlinePixel, false);
    }

    /**
     * A solid line from the player, at the centre of the minimap, straight to the border the player
     * is outside of, in the same colour as the border outline. The minimap counterpart of the world
     * map guide line.
     *
     * <p>The nearest point is found in world space, where the border is axis aligned, and only then
     * converted to screen space. The minimap transform is a rotation plus a uniform scale, so the
     * segment survives the trip as the same straight shortest path, simply rotated with the map.</p>
     */
    public static void drawPlayerToBorderLine(GuiGraphics guiGraphics, MinimapBorderOverlayContext context) {
        WorldBorderState border = context.border;
        if (border.contains(context.playerX, context.playerZ)) {
            return;
        }

        // Anything Xaero or an earlier layer queued has to hit the GPU before this batch does.
        guiGraphics.flush();

        // Clamping in world space is the nearest point on the border and cannot overshoot the edge.
        double edgeX = Math.min(Math.max(context.playerX, border.minX), border.maxX);
        double edgeZ = Math.min(Math.max(context.playerZ, border.minZ), border.maxZ);

        float startX = toScreenX(context.playerX, context.playerZ, context);
        float startY = toScreenY(context.playerX, context.playerZ, context);
        float endX = toScreenX(edgeX, edgeZ, context);
        float endY = toScreenY(edgeX, edgeZ, context);

        // The scissor is a square, so on a round minimap the line has to be pulled back inside the
        // circle itself whenever the border is further out than the minimap reaches.
        if (context.circleShape) {
            float dx = endX - startX;
            float dy = endY - startY;
            double length = Math.sqrt(dx * dx + dy * dy);
            double reach = context.logicalHalf * context.displayedMapSize / context.logicalMapSize;
            if (length > reach && length > 0.0) {
                double scale = reach / length;
                endX = startX + (float) (dx * scale);
                endY = startY + (float) (dy * scale);
            }
        }

        GuiQuadBatch batch = new GuiQuadBatch(guiGraphics);
        batch.begin();
        batch.line(startX, startY, endX, endY, PLAYER_LINE_WIDTH,
                OverlayConstants.getBorderOutlineColor(border));
        batch.end();
    }

    /**
     * Scans the minimap row by row, collapses the pixels accepted by {@code filter} into horizontal
     * runs and submits each run as one quad.
     *
     * @param stripeAlignedOnly true when the filter can only accept pixels that are already on a
     *     stripe, which lets the scan jump whole stripe gaps in one step instead of testing them
     */
    private static void fillRuns(
            GuiGraphics guiGraphics,
            MinimapBorderOverlayContext context,
            int color,
            PixelFilter filter,
            boolean stripeAlignedOnly) {
        // Anything Xaero queued through GuiGraphics has to hit the GPU before our batch does.
        guiGraphics.flush();

        GuiQuadBatch batch = new GuiQuadBatch(guiGraphics);
        batch.begin();
        for (int pixelY = 0; pixelY < context.displayedMapSize; pixelY++) {
            fillRow(batch, pixelY, context, color, filter, stripeAlignedOnly);
        }
        batch.end();
    }

    private static void fillRow(
            GuiQuadBatch batch,
            int pixelY,
            MinimapBorderOverlayContext context,
            int color,
            PixelFilter filter,
            boolean stripeAlignedOnly) {
        int width = context.displayedMapSize;
        int pixelX = 0;
        int runStart = -1;

        while (pixelX < width) {
            int runEnd = width;
            if (stripeAlignedOnly) {
                runEnd = Math.min(
                        pixelX + OverlayConstants.stripeRunLength(pixelX, pixelY, context.border.stripePhase),
                        width);
                if (!OverlayConstants.isOnStripe(pixelX + pixelY, context.border.stripePhase)) {
                    pixelX = runEnd;
                    continue;
                }
            }

            while (pixelX < runEnd) {
                if (filter.test(pixelX, pixelY, context)) {
                    if (runStart < 0) {
                        runStart = pixelX;
                    }
                } else if (runStart >= 0) {
                    addRun(batch, runStart, pixelX, pixelY, context, color);
                    runStart = -1;
                }
                pixelX++;
            }

            if (runStart >= 0) {
                addRun(batch, runStart, pixelX, pixelY, context, color);
                runStart = -1;
            }
        }
    }

    private static void addRun(
            GuiQuadBatch batch,
            int fromX,
            int toX,
            int pixelY,
            MinimapBorderOverlayContext context,
            int color) {
        batch.rect(
                context.mapLeft + fromX,
                context.mapTop + pixelY,
                context.mapLeft + toX,
                context.mapTop + pixelY + 1,
                color);
    }

    private static boolean isStripePixel(int pixelX, int pixelY, MinimapBorderOverlayContext context) {
        if (!isPixelOnMinimapShape(pixelX, pixelY, context)) {
            return false;
        }
        return isOutsideWorldBorder(pixelX, pixelY, context)
                && OverlayConstants.isOnStripe(pixelX + pixelY, context.border.stripePhase);
    }

    private static boolean isOutlinePixel(int pixelX, int pixelY, MinimapBorderOverlayContext context) {
        if (!isPixelOnMinimapShape(pixelX, pixelY, context) || isOutsideWorldBorder(pixelX, pixelY, context)) {
            return false;
        }
        return hasOutsideNeighbor(pixelX, pixelY, context);
    }

    private static boolean hasOutsideNeighbor(int pixelX, int pixelY, MinimapBorderOverlayContext context) {
        return isOutsideWorldBorder(pixelX - 1, pixelY, context)
                || isOutsideWorldBorder(pixelX + 1, pixelY, context)
                || isOutsideWorldBorder(pixelX, pixelY - 1, context)
                || isOutsideWorldBorder(pixelX, pixelY + 1, context);
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
