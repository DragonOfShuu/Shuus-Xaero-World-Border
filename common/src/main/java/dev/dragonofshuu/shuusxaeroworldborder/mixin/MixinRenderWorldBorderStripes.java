package dev.dragonofshuu.shuusxaeroworldborder.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.dragonofshuu.shuusxaeroworldborder.overlays.MinimapBorderOverlay;
import dev.dragonofshuu.shuusxaeroworldborder.overlays.MinimapBorderOverlayContext;
import dev.dragonofshuu.shuusxaeroworldborder.overlays.misc.WorldBorderVisibility;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import xaero.common.graphics.CustomVertexConsumers;
import xaero.common.minimap.MinimapProcessor;
import xaero.common.minimap.render.MinimapRenderer;
import xaero.hud.minimap.module.MinimapSession;

/**
 * Draws the world border on the minimap.
 *
 * <p>Injected into {@code MinimapRenderer.renderMinimap} rather than {@code MinimapProcessor.onRender}
 * so the overlay sits between the map chunks and the minimap's own chrome. {@code onRender} only
 * delegates to the renderer, so injecting at its tail put the overlay on top of the frame, the
 * compass, the entity icons and the player dot.</p>
 *
 * <p>The anchor is the one call to {@code RenderSystem.defaultBlendFunc()} in {@code renderMinimap},
 * which is where the square and the round branch join up after blitting the map - the first moment
 * the chunks are actually on screen. Anchoring on {@code renderChunks} itself would be too early:
 * in FBO mode that only fills an off-screen framebuffer, and the map is blitted afterwards, so it
 * would paint straight over the overlay.</p>
 *
 * <p>Note how remapping is split on this class, the mirror image of
 * {@link MixinWorldmapRenderWorldBorderStripes}. {@code MinimapRenderer} is Xaero's own class, so
 * {@code renderMinimap} keeps its name in every environment and {@code remap = false} on the
 * injector leaves it alone. The {@code @At} target is Minecraft's, so {@code remap = true} lets the
 * annotation processor look it up and record the resolved name in the refmap.</p>
 *
 * <p>The build prints "Unable to locate method mapping for @At(INVOKE.&lt;target&gt;)" for that target
 * and it can be ignored, for the same reason it can be ignored on the world map mixin: blaze3d method
 * names are not renamed by either loader, so {@code defaultBlendFunc} is already correct in the dev
 * environment, on production Forge and on production Fabric, and there is nothing to look up. The
 * lookup still has to be attempted, because the target names a Minecraft class.</p>
 *
 * <p>The injected method has to mirror {@code renderMinimap}'s whole parameter list, including the
 * {@code session} and {@code cvc} this class ignores: Mixin checks the handler's descriptor against
 * the target's and rejects anything but the complete list plus {@code CallbackInfo}.</p>
 */
@Mixin(value = MinimapRenderer.class, remap = false)
public abstract class MixinRenderWorldBorderStripes {

    @Inject(
            // Not remapped: Xaero's own method, named the same in every environment.
            method = "renderMinimap",
            at = @At(
                    value = "INVOKE",
                    // Remapped: RenderSystem is Minecraft's.
                    target = "Lcom/mojang/blaze3d/systems/RenderSystem;defaultBlendFunc()V",
                    shift = At.Shift.AFTER,
                    ordinal = 0,
                    remap = true))
    private void shuusxaerowb$renderWorldBorderStripes(
            MinimapSession session,
            GuiGraphics guiGraphics,
            MinimapProcessor minimapProcessor,
            int x,
            int y,
            int width,
            int height,
            double scale,
            int size,
            float partial,
            CustomVertexConsumers cvc,
            CallbackInfo ci) {
        if (!WorldBorderVisibility.isVisible()) {
            return;
        }

        MinimapBorderOverlayContext context =
                MinimapBorderOverlayContext.createRenderContext(minimapProcessor, x, y, scale, partial);
        if (context == null) {
            return;
        }

        drawInGuiPixelSpace(guiGraphics, context);
    }

    /**
     * Draws the overlay with the pose stack restored to plain GUI pixels, top-left origin.
     *
     * <p>By the injection point Xaero has pushed a pose and scaled it by {@code 1 / mapScale}, so one
     * unit is one unit of its own scaled coordinate system rather than one GUI pixel - which is also
     * what the {@code x} and {@code y} handed to {@code renderMinimap} are measured in. Undoing that
     * single scale puts the overlay exactly over the map it belongs to, and lets the scissor, which
     * {@code GuiGraphics} measures in GUI pixels whatever the pose says, clip it correctly.</p>
     */
    private void drawInGuiPixelSpace(
            GuiGraphics guiGraphics,
            MinimapBorderOverlayContext context) {
        PoseStack pose = guiGraphics.pose();

        pose.pushPose();
        try {
            pose.scale((float) context.mapScale, (float) context.mapScale, 1.0f);

            guiGraphics.enableScissor(
                    context.mapLeft,
                    context.mapTop,
                    context.mapLeft + context.displayedMapSize,
                    context.mapTop + context.displayedMapSize);
            try {
                MinimapBorderOverlay.drawStripedOutsideBorder(guiGraphics, context);
                MinimapBorderOverlay.drawWorldBorderOutline(guiGraphics, context);
                MinimapBorderOverlay.drawTargetBorderOutline(guiGraphics, context);
                MinimapBorderOverlay.drawPlayerToBorderLine(guiGraphics, context);
                MinimapBorderOverlay.drawPlayerToTargetLine(guiGraphics, context);
            } finally {
                guiGraphics.disableScissor();
            }
        } finally {
            pose.popPose();
        }
    }
}
