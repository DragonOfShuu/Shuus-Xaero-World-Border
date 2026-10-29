package dev.dragonofshuu.shuusxaeroworldborder.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.border.WorldBorder;

// @Mixin(targets = "xaero.minimap.gui.GuiMinimapDraw")
public class MixinGuiMinimapDraw {

    // @Inject(method = "renderMinimapPixels", at = @At("TAIL"))
    // private void drawMinimapBorderStripes(GuiGraphics graphics, double playerX,
    // double playerZ, int mapElementX,
    // int mapElementY, CallbackInfo ci) {
    // Minecraft mc = Minecraft.getInstance();
    // if (mc.level == null)
    // return;

    // WorldBorder border = mc.level.getWorldBorder();

    // // Map relative screen loop offsets back to real-world block coordinates
    // // Note: You will need to extract Xaero's current zoom scale factor
    // int worldBlockX = (int) (playerX + mapElementX);
    // int worldBlockZ = (int) (playerZ + mapElementY);

    // if (worldBlockX < border.getMinX() || worldBlockX > border.getMaxX() ||
    // worldBlockZ < border.getMinZ()
    // || worldBlockZ > border.getMaxZ()) {
    // long time = System.currentTimeMillis() / 150;

    // // Check diagonal stripe rhythm matching the screen grid location
    // if (((mapElementX + mapElementY + time) % 16) < 6) {
    // // Draws translucent shifting stripes over the matching minimap fragment
    // graphics.fill(mapElementX, mapElementY, mapElementX + 1, mapElementY + 1,
    // 0x60FF3030);
    // }
    // }
    // }
}