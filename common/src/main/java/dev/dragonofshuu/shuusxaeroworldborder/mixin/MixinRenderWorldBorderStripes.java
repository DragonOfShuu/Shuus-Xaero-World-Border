package dev.dragonofshuu.shuusxaeroworldborder.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.dragonofshuu.shuusxaeroworldborder.overlays.MinimapBorderOverlay;
import dev.dragonofshuu.shuusxaeroworldborder.overlays.MinimapBorderOverlayContext;
import net.minecraft.client.gui.GuiGraphics;
import xaero.common.graphics.CustomVertexConsumers;
import xaero.common.minimap.MinimapProcessor;

@Mixin(value = MinimapProcessor.class, remap = false)
public abstract class MixinRenderWorldBorderStripes {
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
        MinimapBorderOverlayContext context = MinimapBorderOverlayContext.createRenderContext((MinimapProcessor) (Object) this, x, y, width, height, scale, size, boxSize, partial);
        if (context == null) {
            return;
        }

        guiGraphics.enableScissor(context.mapLeft, context.mapTop, context.mapLeft + context.displayedMapSize, context.mapTop + context.displayedMapSize);
        try {
            MinimapBorderOverlay.drawStripedOutsideBorder(guiGraphics, context);
            MinimapBorderOverlay.drawWorldBorderOutline(guiGraphics, context);
        } finally {
            guiGraphics.disableScissor();
        }
    }


}
