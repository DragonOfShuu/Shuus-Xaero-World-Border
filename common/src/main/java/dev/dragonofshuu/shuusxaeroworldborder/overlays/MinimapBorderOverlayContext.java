package dev.dragonofshuu.shuusxaeroworldborder.overlays;

import dev.dragonofshuu.shuusxaeroworldborder.OverlayConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.border.BorderStatus;
import xaero.common.HudMod;
import xaero.common.minimap.MinimapProcessor;
import xaero.hud.entity.EntityUtils;
import xaero.hud.minimap.common.config.option.MinimapProfiledConfigOptions;
import xaero.hud.minimap.config.util.MinimapConfigClientUtils;
import xaero.lib.client.config.ClientConfigManager;
import xaero.lib.common.config.option.ConfigOption;

public final class MinimapBorderOverlayContext {
    public final int displayedMapSize;
    public final int mapLeft;
    public final int mapTop;
    public final double half;
    public final boolean circleShape;
    public final double zoom;
    public final double ps;
    public final double pc;
    public final double playerX;
    public final double playerZ;
    public final double borderMinX;
    public final double borderMaxX;
    public final double borderMinZ;
    public final double borderMaxZ;
    public final long stripePhase;
    public final BorderStatus borderStatus;

    public MinimapBorderOverlayContext(
            int displayedMapSize,
            int mapLeft,
            int mapTop,
            double half,
            boolean circleShape,
            double zoom,
            double ps,
            double pc,
            double playerX,
            double playerZ,
            double borderMinX,
            double borderMaxX,
            double borderMinZ,
            double borderMaxZ,
            long stripePhase,
            BorderStatus borderStatus) {
        this.displayedMapSize = displayedMapSize;
        this.mapLeft = mapLeft;
        this.mapTop = mapTop;
        this.half = half;
        this.circleShape = circleShape;
        this.zoom = zoom;
        this.ps = ps;
        this.pc = pc;
        this.playerX = playerX;
        this.playerZ = playerZ;
        this.borderMinX = borderMinX;
        this.borderMaxX = borderMaxX;
        this.borderMinZ = borderMinZ;
        this.borderMaxZ = borderMaxZ;
        this.stripePhase = stripePhase;
        this.borderStatus = borderStatus;
    }

    public static MinimapBorderOverlayContext createRenderContext(
            MinimapProcessor minimapProcessor,
            int x,
            int y,
            int width,
            int height,
            int boxSize,
            float partial) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        }

        Entity renderEntity = mc.getCameraEntity();
        if (renderEntity == null) {
            return null;
        }

        int displayedMapSize = boxSize - OverlayConstants.MINIMAP_INSET * 2;
        double zoom = minimapProcessor.getMinimapZoom();
        if (displayedMapSize <= 0 || zoom <= 0.0) {
            return null;
        }

        ClientConfigManager configManager = HudMod.INSTANCE.getHudConfigs().getClientConfigManager();
        int shape = (Integer) configManager.getEffective((ConfigOption) MinimapProfiledConfigOptions.SHAPE);
        boolean circleShape = shape == 1;
        boolean lockedNorth = MinimapConfigClientUtils.getEffectiveNorthLocked(displayedMapSize / 2, shape);

        int mapLeft = x + OverlayConstants.MINIMAP_INSET;
        int mapTop = y + OverlayConstants.MINIMAP_INSET;
        int maxWidth = Math.max(0, width - mapLeft);
        int maxHeight = Math.max(0, height - mapTop);
        displayedMapSize = Math.min(displayedMapSize, Math.min(maxWidth, maxHeight));
        if (displayedMapSize <= 0) {
            return null;
        }

        double angle = lockedNorth ? Math.toRadians(90.0)
                : Math.toRadians(-90.0 - mc.gameRenderer.getMainCamera().getYRot());
        double ps = Math.sin(Math.PI - angle);
        double pc = Math.cos(Math.PI - angle);
        double playerX = EntityUtils.getEntityX(renderEntity, partial);
        double playerZ = EntityUtils.getEntityZ(renderEntity, partial);
        double borderMinX = mc.level.getWorldBorder().getMinX();
        double borderMaxX = mc.level.getWorldBorder().getMaxX();
        double borderMinZ = mc.level.getWorldBorder().getMinZ();
        double borderMaxZ = mc.level.getWorldBorder().getMaxZ();
        long stripePhase = System.currentTimeMillis() / OverlayConstants.getStripeAnimation(mc.level.getWorldBorder().getStatus());

        var borderStatus = mc.level.getWorldBorder().getStatus();

        return new MinimapBorderOverlayContext(
                displayedMapSize,
                mapLeft,
                mapTop,
                displayedMapSize / 2.0,
                circleShape,
                zoom,
                ps,
                pc,
                playerX,
                playerZ,
                borderMinX,
                borderMaxX,
                borderMinZ,
                borderMaxZ,
                stripePhase,
                borderStatus);
    }
}