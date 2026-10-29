package dev.dragonofshuu.shuusxaeroworldborder.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import xaero.common.HudMod;
import xaero.common.graphics.CustomVertexConsumers;
import xaero.common.minimap.MinimapProcessor;
import xaero.hud.entity.EntityUtils;
import xaero.hud.minimap.common.config.option.MinimapProfiledConfigOptions;
import xaero.hud.minimap.config.util.MinimapConfigClientUtils;
import xaero.lib.client.config.ClientConfigManager;
import xaero.lib.common.config.option.ConfigOption;

@Mixin(value = MinimapProcessor.class, remap = false)
public abstract class MixinGuiMinimapDraw {

    private static final int MINIMAP_INSET = 5;
    private static final int STRIPE_COLOR = 0x903060FF;
    private static final int BORDER_OUTLINE_COLOR = 0xFF6FA0FF;
    private static final int STRIPE_PERIOD = 16;
    private static final int STRIPE_WIDTH = 6;
    private static final long STRIPE_ANIMATION_MS = 150L;

    @Inject(method = "onRender", at = @At("TAIL"), remap = false)
    private void shuusxaerowb$renderWorldBorderStripes(
            GuiGraphics guiGraphics,
            int x,
            int y,
            int width,
            int height,
            double scale,
            int size,
            int boxSize,
            float partial,
            CustomVertexConsumers cvc,
            CallbackInfo ci) {
        MinimapProcessor minimapProcessor = (MinimapProcessor) (Object) this;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        Entity renderEntity = mc.getCameraEntity();
        if (renderEntity == null) {
            return;
        }

        ClientConfigManager configManager = HudMod.INSTANCE.getHudConfigs().getClientConfigManager();
        int displayedMapSize = boxSize - MINIMAP_INSET * 2;
        double zoom = minimapProcessor.getMinimapZoom();
        if (displayedMapSize <= 0 || zoom <= 0.0) {
            return;
        }

        int shape = (Integer) configManager.getEffective((ConfigOption) MinimapProfiledConfigOptions.SHAPE);
        boolean circleShape = shape == 1;
        boolean lockedNorth = MinimapConfigClientUtils.getEffectiveNorthLocked(displayedMapSize / 2, shape);

        int mapLeft = x + MINIMAP_INSET;
        int mapTop = y + MINIMAP_INSET;
        int maxWidth = Math.max(0, width - mapLeft);
        int maxHeight = Math.max(0, height - mapTop);
        displayedMapSize = Math.min(displayedMapSize, Math.min(maxWidth, maxHeight));
        if (displayedMapSize <= 0) {
            return;
        }

        double angle = lockedNorth ? Math.toRadians(90.0)
                : Math.toRadians(-90.0 - mc.gameRenderer.getMainCamera().getYRot());
        double ps = Math.sin(Math.PI - angle);
        double pc = Math.cos(Math.PI - angle);
        double playerX = EntityUtils.getEntityX(renderEntity, partial);
        double playerZ = EntityUtils.getEntityZ(renderEntity, partial);
        double half = displayedMapSize / 2.0;
        double borderMinX = mc.level.getWorldBorder().getMinX();
        double borderMaxX = mc.level.getWorldBorder().getMaxX();
        double borderMinZ = mc.level.getWorldBorder().getMinZ();
        double borderMaxZ = mc.level.getWorldBorder().getMaxZ();
        long stripePhase = System.currentTimeMillis() / STRIPE_ANIMATION_MS;
        guiGraphics.enableScissor(mapLeft, mapTop, mapLeft + displayedMapSize, mapTop + displayedMapSize);
        for (int pixelY = 0; pixelY < displayedMapSize; pixelY++) {
            int runStart = -1;
            for (int pixelX = 0; pixelX < displayedMapSize; pixelX++) {
                double offX = pixelX + 0.5 - half;
                double offY = pixelY + 0.5 - half;
                boolean inCircle = !circleShape || offX * offX + offY * offY <= half * half;

                boolean drawStripe = false;
                if (inCircle) {
                    double zoomedOffX = offX / zoom;
                    double zoomedOffY = offY / zoom;
                    double worldX = playerX + ps * zoomedOffX + pc * zoomedOffY;
                    double worldZ = playerZ + ps * zoomedOffY - pc * zoomedOffX;
                    boolean outsideBorder = worldX < borderMinX || worldX > borderMaxX || worldZ < borderMinZ
                            || worldZ > borderMaxZ;
                    drawStripe = outsideBorder && ((pixelX + pixelY + stripePhase) % STRIPE_PERIOD) < STRIPE_WIDTH;
                }

                if (drawStripe) {
                    if (runStart < 0) {
                        runStart = pixelX;
                    }
                } else if (runStart >= 0) {
                    guiGraphics.fill(mapLeft + runStart, mapTop + pixelY, mapLeft + pixelX, mapTop + pixelY + 1,
                            STRIPE_COLOR);
                    runStart = -1;
                }
            }
            if (runStart >= 0) {
                guiGraphics.fill(mapLeft + runStart, mapTop + pixelY, mapLeft + displayedMapSize, mapTop + pixelY + 1,
                        STRIPE_COLOR);
            }
        }

        for (int pixelY = 0; pixelY < displayedMapSize; pixelY++) {
            for (int pixelX = 0; pixelX < displayedMapSize; pixelX++) {
                double offX = pixelX + 0.5 - half;
                double offY = pixelY + 0.5 - half;
                if (circleShape && offX * offX + offY * offY > half * half) {
                    continue;
                }

                double zoomedOffX = offX / zoom;
                double zoomedOffY = offY / zoom;
                double worldX = playerX + ps * zoomedOffX + pc * zoomedOffY;
                double worldZ = playerZ + ps * zoomedOffY - pc * zoomedOffX;
                boolean outside = worldX < borderMinX || worldX > borderMaxX || worldZ < borderMinZ
                        || worldZ > borderMaxZ;
                if (outside) {
                    continue;
                }

                if (this.hasOutsideNeighbor(pixelX, pixelY, displayedMapSize, half, circleShape, playerX, playerZ, ps,
                        pc, zoom,
                        borderMinX, borderMaxX, borderMinZ, borderMaxZ)) {
                    guiGraphics.fill(mapLeft + pixelX, mapTop + pixelY, mapLeft + pixelX + 1, mapTop + pixelY + 1,
                            BORDER_OUTLINE_COLOR);
                }
            }
        }
        guiGraphics.disableScissor();
    }

    private boolean hasOutsideNeighbor(
            int pixelX,
            int pixelY,
            int displayedMapSize,
            double half,
            boolean circleShape,
            double playerX,
            double playerZ,
            double ps,
            double pc,
            double zoom,
            double borderMinX,
            double borderMaxX,
            double borderMinZ,
            double borderMaxZ) {
        return this.isOutsideOrOffMinimap(pixelX - 1, pixelY, displayedMapSize, half, circleShape, playerX, playerZ, ps,
                pc, zoom, borderMinX, borderMaxX, borderMinZ, borderMaxZ)
                || this.isOutsideOrOffMinimap(pixelX + 1, pixelY, displayedMapSize, half, circleShape, playerX, playerZ,
                        ps, pc, zoom, borderMinX, borderMaxX, borderMinZ, borderMaxZ)
                || this.isOutsideOrOffMinimap(pixelX, pixelY - 1, displayedMapSize, half, circleShape, playerX, playerZ,
                        ps, pc, zoom, borderMinX, borderMaxX, borderMinZ, borderMaxZ)
                || this.isOutsideOrOffMinimap(pixelX, pixelY + 1, displayedMapSize, half, circleShape, playerX, playerZ,
                        ps, pc, zoom, borderMinX, borderMaxX, borderMinZ, borderMaxZ);
    }

    private boolean isOutsideOrOffMinimap(
            int pixelX,
            int pixelY,
            int displayedMapSize,
            double half,
            boolean circleShape,
            double playerX,
            double playerZ,
            double ps,
            double pc,
            double zoom,
            double borderMinX,
            double borderMaxX,
            double borderMinZ,
            double borderMaxZ) {
        if (pixelX < 0 || pixelY < 0 || pixelX >= displayedMapSize || pixelY >= displayedMapSize) {
            return true;
        }

        double offX = pixelX + 0.5 - half;
        double offY = pixelY + 0.5 - half;
        if (circleShape && offX * offX + offY * offY > half * half) {
            return true;
        }

        double zoomedOffX = offX / zoom;
        double zoomedOffY = offY / zoom;
        double worldX = playerX + ps * zoomedOffX + pc * zoomedOffY;
        double worldZ = playerZ + ps * zoomedOffY - pc * zoomedOffX;
        return worldX < borderMinX || worldX > borderMaxX || worldZ < borderMinZ || worldZ > borderMaxZ;
    }
}