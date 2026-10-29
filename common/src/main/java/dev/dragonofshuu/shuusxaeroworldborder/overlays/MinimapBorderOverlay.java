package dev.dragonofshuu.shuusxaeroworldborder.overlays;

import dev.dragonofshuu.shuusxaeroworldborder.OverlayConstants;
import net.minecraft.client.gui.GuiGraphics;

public class MinimapBorderOverlay {
    public static void drawStripedOutsideBorder(GuiGraphics guiGraphics, MinimapBorderOverlayContext context) {
        for (int pixelY = 0; pixelY < context.displayedMapSize; pixelY++) {
            for (int pixelX = 0; pixelX < context.displayedMapSize; pixelX++) {
                if (MinimapBorderOverlay.shouldDrawPixelAsStripe(pixelX, pixelY, context)) {
                    MinimapBorderOverlay.drawPixelAsStripe(guiGraphics, pixelX, pixelY, context);
                }
            }
        }
    }

    private static void drawPixelAsStripe(GuiGraphics guiGraphics, int pixelX, int pixelY, MinimapBorderOverlayContext context) {
        guiGraphics.fill(
                context.mapLeft + pixelX,
                context.mapTop + pixelY,
                context.mapLeft + pixelX + 1,
                context.mapTop + pixelY + 1,
                OverlayConstants.getStripeColor(context));
    }

    private static boolean shouldDrawPixelAsStripe(int pixelX, int pixelY, MinimapBorderOverlayContext context) {
        if (!MinimapBorderOverlay.isPixelOnMinimapShape(pixelX, pixelY, context)) {
            return false;
        }
        return MinimapBorderOverlay.isOutsideWorldBorder(pixelX, pixelY, context) && MinimapBorderOverlay.isStripePhase(pixelX, pixelY, context);
    }

    private static boolean isStripePhase(int pixelX, int pixelY, MinimapBorderOverlayContext context) {
        return ((pixelX + pixelY + context.stripePhase) % OverlayConstants.STRIPE_PERIOD) < OverlayConstants.STRIPE_WIDTH;
    }

    public static void drawWorldBorderOutline(GuiGraphics guiGraphics, MinimapBorderOverlayContext context) {
        for (int pixelY = 0; pixelY < context.displayedMapSize; pixelY++) {
            for (int pixelX = 0; pixelX < context.displayedMapSize; pixelX++) {
                if (!MinimapBorderOverlay.shouldDrawBorderOutlinePixel(pixelX, pixelY, context)) {
                    continue;
                }
                guiGraphics.fill(
                        context.mapLeft + pixelX,
                        context.mapTop + pixelY,
                        context.mapLeft + pixelX + 1,
                        context.mapTop + pixelY + 1,
                        OverlayConstants.getBorderOutlineColor(context));
            }
        }
    }

    private static boolean shouldDrawBorderOutlinePixel(int pixelX, int pixelY, MinimapBorderOverlayContext context) {
        if (!MinimapBorderOverlay.isPixelOnMinimapShape(pixelX, pixelY, context)) {
            return false;
        }
        if (MinimapBorderOverlay.isOutsideWorldBorder(pixelX, pixelY, context)) {
            return false;
        }
        return MinimapBorderOverlay.hasOutsideNeighbor(pixelX, pixelY, context);
    }

    private static boolean hasOutsideNeighbor(int pixelX, int pixelY, MinimapBorderOverlayContext context) {
        return MinimapBorderOverlay.isOutsideOrOffMinimap(pixelX - 1, pixelY, context)
                || MinimapBorderOverlay.isOutsideOrOffMinimap(pixelX + 1, pixelY, context)
                || MinimapBorderOverlay.isOutsideOrOffMinimap(pixelX, pixelY - 1, context)
                || MinimapBorderOverlay.isOutsideOrOffMinimap(pixelX, pixelY + 1, context);
    }

    private static boolean isPixelOnMinimapShape(int pixelX, int pixelY, MinimapBorderOverlayContext context) {
        if (!context.circleShape) {
            return true;
        }
        double offX = MinimapBorderOverlay.toLogicalOffsetX(pixelX, context);
        double offY = MinimapBorderOverlay.toLogicalOffsetY(pixelY, context);
        return offX * offX + offY * offY <= context.logicalHalf * context.logicalHalf;
    }

    private static boolean isOutsideWorldBorder(int pixelX, int pixelY, MinimapBorderOverlayContext context) {
        double worldX = MinimapBorderOverlay.toWorldX(pixelX, pixelY, context);
        double worldZ = MinimapBorderOverlay.toWorldZ(pixelX, pixelY, context);
        return worldX < context.borderMinX || worldX > context.borderMaxX || worldZ < context.borderMinZ || worldZ > context.borderMaxZ;
    }

    private static double toWorldX(int pixelX, int pixelY, MinimapBorderOverlayContext context) {
        double offX = MinimapBorderOverlay.toLogicalOffsetX(pixelX, context);
        double offY = MinimapBorderOverlay.toLogicalOffsetY(pixelY, context);
        double blockOffX = offX * 2.0 * context.mapScale / context.zoom;
        double blockOffY = offY * 2.0 * context.mapScale / context.zoom;
        return context.playerX + context.ps * blockOffX + context.pc * blockOffY;
    }

    private static double toWorldZ(int pixelX, int pixelY, MinimapBorderOverlayContext context) {
        double offX = MinimapBorderOverlay.toLogicalOffsetX(pixelX, context);
        double offY = MinimapBorderOverlay.toLogicalOffsetY(pixelY, context);
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

    private static boolean isOutsideOrOffMinimap(
            int pixelX,
            int pixelY,
            MinimapBorderOverlayContext context) {
        return MinimapBorderOverlay.isOutsideWorldBorder(pixelX, pixelY, context);
    }
}
