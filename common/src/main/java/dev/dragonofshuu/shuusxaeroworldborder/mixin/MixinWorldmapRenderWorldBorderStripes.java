package dev.dragonofshuu.shuusxaeroworldborder.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphics;
import xaero.map.gui.GuiMap;

@Mixin(value = GuiMap.class, remap = false)
public class MixinWorldmapRenderWorldBorderStripes {
    @Inject(method = "render", at = @At("HEAD"), remap = false)
    private void shuusxaerowb$worldmapRenderWorldBorderStripes(GuiGraphics guiGraphics, int scaledMouseX, int scaledMouseY, float delta, CallbackInfo ci) {
        guiGraphics.fill(scaledMouseX, scaledMouseY, scaledMouseX + 10, scaledMouseY + 10, 0xFFFFFFFF);
    }
}
