package dev.dragonofshuu.shuusxaeroworldborder.overlays;

import dev.dragonofshuu.shuusxaeroworldborder.overlays.misc.WorldBorderState;
import dev.dragonofshuu.shuusxaeroworldborder.overlays.types.ScreenPoint;
import dev.dragonofshuu.shuusxaeroworldborder.overlays.types.ScreenRect;
import net.minecraft.client.Minecraft;

import xaero.map.MapProcessor;
import xaero.map.gui.GuiMap;

/**
 * Screen placement of the world border on the world map.
 *
 * <p>The world map never rotates, so the axis-aligned world border is always an axis-aligned
 * rectangle on screen. Everything here is that rectangle, converted to GUI pixels.</p>
 *
 * <p>The rectangle is anchored to the map <em>camera</em>, not the player, so panning the map moves
 * the overlay with the map while walking around leaves it exactly where the map tiles are.</p>
 */
public final class WorldmapBorderOverlayContext {
    public final int screenWidth;
    public final int screenHeight;
    /** Where the border is right now, in GUI pixels. Edges are allowed to fall outside of the screen. */
    public final ScreenRect borderRect;
    /** Where a moving border is heading, in GUI pixels. Only meaningful while the border moves. */
    public final ScreenRect targetRect;
    /** The player in GUI pixels, or {@code null} when there is no client player this frame. */
    public final ScreenPoint player;
    public final WorldBorderState border;

    public WorldmapBorderOverlayContext(
            int screenWidth,
            int screenHeight,
            ScreenRect borderRect,
            ScreenRect targetRect,
            ScreenPoint player,
            WorldBorderState border) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.borderRect = borderRect;
        this.targetRect = targetRect;
        this.player = player;
        this.border = border;
    }

    /**
     * Builds the overlay rectangle for this frame.
     *
     * @param cameraX      map camera X in blocks, i.e. the block shown at the screen centre
     * @param cameraZ      map camera Z in blocks, i.e. the block shown at the screen centre
     * @param mapScale     raw window pixels per block, Xaero's own {@code GuiMap.scale}
     * @param guiScale     window GUI scale, converting raw window pixels to GUI pixels
     * @param screenWidth  screen width in GUI pixels
     * @param screenHeight screen height in GUI pixels
     * @return {@code null} when there is nothing to draw this frame
     */
    public static WorldmapBorderOverlayContext createRenderContext(
            GuiMap guiMap,
            double cameraX,
            double cameraZ,
            double mapScale,
            double guiScale,
            int screenWidth,
            int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || guiScale <= 0.0 || screenWidth <= 0 || screenHeight <= 0) {
            return null;
        }

        // The border belongs to the dimension the player is in, so only draw it while the map is
        // actually showing that dimension (the map can be panned to another one).
        if (!isViewingCurrentDimension(guiMap, mc)) {
            return null;
        }

        WorldBorderState border = WorldBorderState.fromLevel(mc.level);
        if (border == null) {
            return null;
        }

        // Xaero maps the screen centre to (cameraX, cameraZ) at mapScale window pixels per block.
        double pixelsPerBlock = mapScale / guiScale;
        if (pixelsPerBlock <= 0.0) {
            return null;
        }

        ScreenRect borderRect = toScreen(
                border.minX, border.maxX, border.minZ, border.maxZ,
                cameraX, cameraZ, pixelsPerBlock, screenWidth, screenHeight);
        ScreenRect targetRect = toScreen(
                border.targetMinX, border.targetMaxX, border.targetMinZ, border.targetMaxZ,
                cameraX, cameraZ, pixelsPerBlock, screenWidth, screenHeight);

        // The player lives in the same world space as the border, and the map's own dimension
        // divisor is 1 here because the dimension guard above already passed.
        ScreenPoint player = mc.player == null
                ? null
                : new ScreenPoint(
                        toScreenX(mc.player.getX(), cameraX, pixelsPerBlock, screenWidth),
                        toScreenY(mc.player.getZ(), cameraZ, pixelsPerBlock, screenHeight));

        return new WorldmapBorderOverlayContext(
                screenWidth, screenHeight, borderRect, targetRect, player, border);
    }

    /** Converts a world space border rectangle into GUI pixels, anchored on the map camera. */
    private static ScreenRect toScreen(
            double minX,
            double maxX,
            double minZ,
            double maxZ,
            double cameraX,
            double cameraZ,
            double pixelsPerBlock,
            int screenWidth,
            int screenHeight) {
        return new ScreenRect(
                toScreenX(minX, cameraX, pixelsPerBlock, screenWidth),
                toScreenY(minZ, cameraZ, pixelsPerBlock, screenHeight),
                toScreenX(maxX, cameraX, pixelsPerBlock, screenWidth),
                toScreenY(maxZ, cameraZ, pixelsPerBlock, screenHeight));
    }

    private static float toScreenX(
            double worldX, double cameraX, double pixelsPerBlock, int screenWidth) {
        return (float) (screenWidth / 2.0 + (worldX - cameraX) * pixelsPerBlock);
    }

    private static float toScreenY(
            double worldZ, double cameraZ, double pixelsPerBlock, int screenHeight) {
        return (float) (screenHeight / 2.0 + (worldZ - cameraZ) * pixelsPerBlock);
    }

    private static boolean isViewingCurrentDimension(GuiMap guiMap, Minecraft mc) {
        MapProcessor mapProcessor = guiMap.getMapProcessor();
        if (mapProcessor == null || !mapProcessor.isMapWorldUsable()) {
            return false;
        }
        return mapProcessor.getMapWorld().getCurrentDimensionId() != null
                && mapProcessor.getMapWorld().getCurrentDimensionId().equals(mc.level.dimension());
    }
}
