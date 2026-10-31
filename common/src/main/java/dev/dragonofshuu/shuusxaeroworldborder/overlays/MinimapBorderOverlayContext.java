package dev.dragonofshuu.shuusxaeroworldborder.overlays;

import dev.dragonofshuu.shuusxaeroworldborder.overlays.misc.WorldBorderState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import xaero.common.HudMod;
import xaero.common.minimap.MinimapProcessor;
import xaero.hud.entity.EntityUtils;
import xaero.hud.gui.util.GuiUtils;
import xaero.hud.minimap.common.config.option.MinimapProfiledConfigOptions;
import xaero.hud.minimap.config.util.MinimapConfigClientUtils;
import xaero.lib.client.config.ClientConfigManager;
import xaero.lib.common.config.option.ConfigOption;

/**
 * Geometry of the on-screen minimap, plus the world border it shows.
 *
 * <p>Built once per frame from {@link MinimapProcessor} inside the minimap render hook; every
 * value is expressed either in GUI pixels (screen placement) or in world blocks (border bounds).</p>
 */
public final class MinimapBorderOverlayContext {
    public final int displayedMapSize;
    public final int logicalMapSize;
    public final int mapLeft;
    public final int mapTop;
    public final double logicalHalf;
    public final boolean circleShape;
    public final double zoom;
    /** mapScale = renderScale / minimapScale: used to convert overlay pixels to world blocks. */
    public final double mapScale;
    /** In-game GUI scale */
    public final double guiScale;
    public final double ps;
    public final double pc;
    public final double playerX;
    public final double playerZ;
    public final WorldBorderState border;

    public MinimapBorderOverlayContext(
            int displayedMapSize,
            int logicalMapSize,
            int mapLeft,
            int mapTop,
            double logicalHalf,
            boolean circleShape,
            double zoom,
            double mapScale,
            double guiScale,
            double ps,
            double pc,
            double playerX,
            double playerZ,
            WorldBorderState border) {
        this.displayedMapSize = displayedMapSize;
        this.logicalMapSize = logicalMapSize;
        this.mapLeft = mapLeft;
        this.mapTop = mapTop;
        this.logicalHalf = logicalHalf;
        this.circleShape = circleShape;
        this.zoom = zoom;
        this.mapScale = mapScale;
        this.guiScale = guiScale;
        this.ps = ps;
        this.pc = pc;
        this.playerX = playerX;
        this.playerZ = playerZ;
        this.border = border;
    }

    /**
     * Returns {@code null} when there is nothing to draw this frame (no level, no camera entity,
     * or a degenerate minimap).
     */
    public static MinimapBorderOverlayContext createRenderContext(
            MinimapProcessor minimapProcessor,
            int x,
            int y,
            int width,
            int height,
            double scale,
            int size,
            int boxSize,
            float partial) {
        Minecraft mc = Minecraft.getInstance();
        WorldBorderState border = WorldBorderState.fromLevel(mc.level);
        if (border == null) {
            return null;
        }

        Entity renderEntity = mc.getCameraEntity();
        if (renderEntity == null) {
            return null;
        }

        ClientConfigManager configManager = HudMod.INSTANCE.getHudConfigs().getClientConfigManager();

        // mapScale mirrors Xaero's own calculation: (float)(scale / minimapScale).
        // It converts positions in the frame's "scaled" coordinate system to GUI pixels.
        // Entities are drawn at zoom/(2*mapScale) GUI pixels per block, so our
        // pixel-to-block conversion must multiply by 2*mapScale/zoom.
        float minimapScale = GuiUtils.getMinimapScale(configManager);
        double mapScale = scale / minimapScale;

        // The minimap frame occupies 9 units in Xaero's scaledX space on each side.
        // In GUI pixels that is 9/mapScale. Use the inner block-content dimensions directly:
        // inner width = (mapSize/2) / mapScale  (same math Xaero uses for minimapFrameSize).
        int mapSize = minimapProcessor.getMinimapSize();
        int displayedMapSize = (int) Math.round((mapSize / 2.0) / mapScale);
        int logicalMapSize = displayedMapSize;
        double zoom = minimapProcessor.getMinimapZoom();
        if (displayedMapSize <= 0 || logicalMapSize <= 0 || zoom <= 0.0) {
            return null;
        }

        int shape = (Integer) configManager.getEffective((ConfigOption) MinimapProfiledConfigOptions.SHAPE);
        boolean circleShape = shape == 1;
        boolean lockedNorth = MinimapConfigClientUtils.getEffectiveNorthLocked(logicalMapSize / 2, shape);

        // The inner content starts at x + 9/mapScale in GUI pixels.
        int mapLeft = (int) Math.round(x + 9.0 / mapScale);
        int mapTop = (int) Math.round(y + 9.0 / mapScale);

        double angle = lockedNorth ? Math.toRadians(90.0)
                : Math.toRadians(-90.0 - mc.gameRenderer.getMainCamera().getYRot());
        double ps = Math.sin(Math.PI - angle);
        double pc = Math.cos(Math.PI - angle);
        double playerX = EntityUtils.getEntityX(renderEntity, partial);
        double playerZ = EntityUtils.getEntityZ(renderEntity, partial);

        return new MinimapBorderOverlayContext(
                displayedMapSize,
                logicalMapSize,
                mapLeft,
                mapTop,
                logicalMapSize / 2.0,
                circleShape,
                zoom,
                mapScale,
                mc.getWindow().getGuiScale(),
                ps,
                pc,
                playerX,
                playerZ,
                border);
    }
}
