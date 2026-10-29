package dev.dragonofshuu.shuusxaeroworldborder;

import dev.dragonofshuu.shuusxaeroworldborder.overlays.MinimapBorderOverlayContext;
import net.minecraft.world.level.border.BorderStatus;

import java.awt.Color;

public class OverlayConstants {
    public static final int STRIPE_PERIOD = 16;
    public static final int STRIPE_WIDTH = 6;

    public static int getStripeColor(MinimapBorderOverlayContext context) {
        int originalColor = context.borderStatus.getColor();
        Color color = new Color((128 << 24) | originalColor, true);
        Color darkerColor = color.darker();
        int darkenedColor = darkerColor.getRGB();
        return darkenedColor;
    }

    public static int getBorderOutlineColor(MinimapBorderOverlayContext context) {
        int originalColor = context.borderStatus.getColor();
        Color color = new Color(originalColor);
        Color brighterColor = color.brighter();
        int lightenedColor = brighterColor.getRGB();
        return lightenedColor;
    }

    public static long getStripeAnimation(MinimapBorderOverlayContext context) {
        return context.borderStatus.equals(BorderStatus.STATIONARY) ? 150L : 50L;
    }

    public static long getStripeAnimation(BorderStatus borderStatus) {
        return borderStatus.equals(BorderStatus.STATIONARY) ? 150L : 30L;
    }
}
