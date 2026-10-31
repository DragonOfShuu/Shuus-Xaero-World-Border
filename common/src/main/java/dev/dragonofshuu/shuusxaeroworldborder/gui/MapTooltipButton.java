package dev.dragonofshuu.shuusxaeroworldborder.gui;
    
import java.util.function.Supplier;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import xaero.lib.client.gui.widget.Tooltip;
import xaero.map.gui.TooltipButton;

public class MapTooltipButton extends TooltipButton {
    /** Width and height of the button, the same size Xaero's icon buttons use. */
    public static final int SIZE = 20;

    /** Width and height of the glyph, in both the button and the texture. */
    public static final int ICON_SIZE = 10;

    /** Shadow offset, in GUI pixels, matching the one baked into Xaero's own icons. */
    public static final int SHADOW_OFFSET = 1;

    /** Shadow brightness, the grey Xaero bakes into its atlas ({@code #3F3F3F}). */
    public static final float SHADOW = 63.0F / 255.0F;

    /** Glyph brightness when idle and when hovered, copied from Xaero's own icon buttons. */
    public static final float IDLE_TINT = 0.9882F;
    public static final float HOVER_TINT = 0.9F;

    private final Supplier<ResourceLocation> textureSupplier;

    public MapTooltipButton(int x, int y, OnPress onPress, Supplier<Tooltip> tooltipSupplier, Supplier<ResourceLocation> textureSupplier) {
        super(
                x,
                y,
                SIZE,
                SIZE,
                Component.empty(),
                onPress,
                tooltipSupplier);
        this.textureSupplier = textureSupplier;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int iconX = getX() + SIZE / 2 - ICON_SIZE / 2;
        // Hovered icons lift a pixel, shadow and glyph together, exactly like Xaero's buttons.
        int iconY = getY() + SIZE / 2 - ICON_SIZE / 2 - (isHovered() ? 1 : 0);
        float tint = isHovered() ? HOVER_TINT : IDLE_TINT;
        ResourceLocation texture = textureSupplier.get();

        RenderSystem.setShaderColor(SHADOW * tint, SHADOW * tint, SHADOW * tint, 1.0F);
        blitGlyph(guiGraphics, texture, iconX + SHADOW_OFFSET, iconY + SHADOW_OFFSET);

        RenderSystem.setShaderColor(tint, tint, tint, 1.0F);
        blitGlyph(guiGraphics, texture, iconX, iconY);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** The glyphs are their own textures rather than atlas sprites, so the texture size is explicit. */
    private static void blitGlyph(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y) {
        guiGraphics.blit(
                texture,
                x,
                y,
                0.0F,
                0.0F,
                ICON_SIZE,
                ICON_SIZE,
                ICON_SIZE,
                ICON_SIZE);
    }
}
