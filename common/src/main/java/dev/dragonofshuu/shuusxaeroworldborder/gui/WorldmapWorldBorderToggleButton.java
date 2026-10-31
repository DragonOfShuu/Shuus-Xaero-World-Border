package dev.dragonofshuu.shuusxaeroworldborder.gui;

import dev.dragonofshuu.shuusxaeroworldborder.Constants;
import dev.dragonofshuu.shuusxaeroworldborder.overlays.misc.WorldBorderVisibility;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;

import xaero.lib.client.gui.widget.Tooltip;

/**
 * The world map button that turns the world border overlays on and off.
 *
 * <p>It is a plain Xaero widget rather than something drawn by hand, so everything the icons around it
 * do comes for free: {@code xaero.lib.client.gui.ScreenBase#renderTooltips} picks up the tooltip from
 * {@link #getXaero_tooltip()}, {@code GuiMap} routes clicks through it like any other button, and it
 * is drawn and hidden together with the rest of the interface.</p>
 *
 * <p>Xaero bakes the drop shadow of its own glyphs into the atlas. The glyphs here are flat white, so
 * the shadow is drawn at runtime instead as a darkened copy offset down and to the right, which is
 * where the atlas puts it.</p>
 */
public final class WorldmapWorldBorderToggleButton extends MapTooltipButton {
    /**
     * GUI pixels between the bottom of the screen and the bottom of the button. Xaero stacks its own
     * bottom right icons in 20 pixel steps up to {@code height - 180}, the zoom in button, so this
     * parks ours in the next free slot straight above the column.
     */
    private static final int BOTTOM_INSET = 180;

    private static final ResourceLocation ENABLED_TEXTURE =
            new ResourceLocation(Constants.MOD_ID, "textures/gui/border_enabled_icon.png");
    private static final ResourceLocation DISABLED_TEXTURE =
            new ResourceLocation(Constants.MOD_ID, "textures/gui/border_disabled_icon.png");

    public WorldmapWorldBorderToggleButton() {
        super(
            Minecraft.getInstance().getWindow().getGuiScaledWidth() - MapTooltipButton.SIZE,
            Minecraft.getInstance().getWindow().getGuiScaledHeight() - BOTTOM_INSET - MapTooltipButton.SIZE,
            button -> WorldBorderVisibility.toggle(),
            () -> new Tooltip(Component.translatable("gui." + Constants.MOD_ID + ".toggle_world_border_visibility")),
            () -> WorldBorderVisibility.isVisible() ? ENABLED_TEXTURE : DISABLED_TEXTURE
        );
    }
}
