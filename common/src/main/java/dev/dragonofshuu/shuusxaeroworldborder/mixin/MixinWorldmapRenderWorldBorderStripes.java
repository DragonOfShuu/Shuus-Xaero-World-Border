package dev.dragonofshuu.shuusxaeroworldborder.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.dragonofshuu.shuusxaeroworldborder.overlays.WorldmapBorderOverlay;
import dev.dragonofshuu.shuusxaeroworldborder.overlays.WorldmapBorderOverlayContext;
import dev.dragonofshuu.shuusxaeroworldborder.overlays.misc.WorldBorderVisibility;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import xaero.map.gui.GuiMap;

/**
 * Draws the world border on the full screen world map.
 *
 * <p>Injected right before {@code MapElementRenderHandler.render(...)}, which is the first thing
 * Xaero draws once the map tiles are on screen. Everything else - waypoints, the footstep trail, the
 * player arrow, the compass and coordinate readout, and the widgets drawn by {@code super.render}
 * - comes after it, so all of them stay on top of the overlay.</p>
 */
@Mixin(value = GuiMap.class, remap = false)
public abstract class MixinWorldmapRenderWorldBorderStripes {

    /**
     * The one and only call to {@code MapElementRenderHandler.render} in {@code GuiMap.render}.
     * Written out in pieces only so it stays readable.
     */
    private static final String MAP_ELEMENTS_RENDER =
            "Lxaero/map/element/MapElementRenderHandler;render("
                    + "Lxaero/map/gui/GuiMap;"
                    + "Lnet/minecraft/client/gui/GuiGraphics;"
                    + "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;"
                    + "Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;"
                    + "DDIIDDDDDFZ"
                    + "Lxaero/map/element/HoveredMapElementHolder;"
                    + "Lnet/minecraft/client/Minecraft;F)"
                    + "Lxaero/map/element/HoveredMapElementHolder;";

    /** Block shown at the centre of the screen. */
    @Shadow
    private double cameraX;

    /** Block shown at the centre of the screen. */
    @Shadow
    private double cameraZ;

    /** Window pixels per block for the frame currently being drawn. */
    @Shadow
    private double scale;

    @Inject(
            method = "render",
            at = @At(value = "INVOKE", target = MAP_ELEMENTS_RENDER, shift = At.Shift.BEFORE),
            remap = false)
    private void shuusxaerowb$renderWorldmapWorldBorderStripes(
            GuiGraphics guiGraphics,
            int scaledMouseX,
            int scaledMouseY,
            float delta,
            CallbackInfo ci) {
        if (!WorldBorderVisibility.isVisible()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Window window = mc.getWindow();

        WorldmapBorderOverlayContext context = WorldmapBorderOverlayContext.createRenderContext(
                (GuiMap) (Object) this,
                this.cameraX,
                this.cameraZ,
                this.scale,
                window.getGuiScale(),
                window.getGuiScaledWidth(),
                window.getGuiScaledHeight());
        if (context == null) {
            return;
        }

        drawInGuiPixelSpace(guiGraphics, window, context);
    }

    /**
     * Draws the overlay with the pose stack restored to plain GUI pixels, top-left origin.
     *
     * <p>By the time Xaero renders map elements it has reshaped the pose stack to
     * {@code base * scale(1 / guiScale) * translate(windowWidth / 2, windowHeight / 2) * scale(scale)},
     * i.e. one unit is one block and the origin is the middle of the window. That is inconvenient to
     * draw pixel sized stripes in, and it is also exactly where the map tiles have just landed, so
     * rather than move the injection this undoes that transform for the duration of the overlay.
     * The three calls below are the inverse, applied in reverse order, so the resulting matrix is
     * bit for bit the one {@code GuiGraphics} started with and the overlay draws identically to the
     * way it would anywhere else on screen.</p>
     */
    private void drawInGuiPixelSpace(
            GuiGraphics guiGraphics,
            Window window,
            WorldmapBorderOverlayContext context) {
        PoseStack pose = guiGraphics.pose();

        pose.pushPose();
        try {
            float pixelsPerBlock = (float) this.scale;
            if (pixelsPerBlock > 0.0f) {
                pose.scale(1.0f / pixelsPerBlock, 1.0f / pixelsPerBlock, 1.0f);
            }
            // Xaero uses integer division here, so the inverse has to as well.
            pose.translate(
                    -(float) (window.getWidth() / 2),
                    -(float) (window.getHeight() / 2),
                    0.0f);
            pose.scale((float) window.getGuiScale(), (float) window.getGuiScale(), 1.0f);

            WorldmapBorderOverlay.draw(guiGraphics, context);
        } finally {
            pose.popPose();
        }
    }
}
