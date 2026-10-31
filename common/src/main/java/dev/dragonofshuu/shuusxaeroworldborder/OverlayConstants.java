package dev.dragonofshuu.shuusxaeroworldborder;

import net.minecraft.world.level.border.BorderStatus;

import java.awt.Color;

import dev.dragonofshuu.shuusxaeroworldborder.overlays.misc.WorldBorderState;

/**
 * Visual constants shared by every world border overlay (minimap and world map).
 */
public class OverlayConstants {
    /** Distance between two stripes, in GUI pixels. */
    public static final int STRIPE_PERIOD = 16;
    /** Thickness of a single stripe, in GUI pixels. */
    public static final int STRIPE_WIDTH = 6;
    /** Thickness of the solid line drawn along the border itself on the world map, in GUI pixels. */
    public static final int WORLDMAP_OUTLINE_WIDTH = 2;

    /** Stripe fill, drawn outside of the border. Darker than vanilla's border colour. */
    public static int getStripeColor(WorldBorderState border) {
        Color color = new Color((128 << 24) | border.status.getColor(), true);
        return color.darker().getRGB();
    }

    /** Solid outline, drawn on the border itself. Brighter than vanilla's border colour. */
    public static int getBorderOutlineColor(WorldBorderState border) {
        Color color = new Color(border.status.getColor());
        return color.brighter().getRGB();
    }

    /** Milliseconds per stripe animation step; shrinking/growing borders animate faster. */
    public static long getStripeAnimation(BorderStatus borderStatus) {
        return borderStatus.equals(BorderStatus.STATIONARY) ? 150L : 30L;
    }

    /** The stripe phase folded into a single stripe period, so band maths stays small. */
    public static long stripePhaseOffset(long stripePhase) {
        return Math.floorMod(stripePhase, STRIPE_PERIOD);
    }

    /**
     * True when the 45 degree stripe pattern covers the screen coordinate sum {@code x + y}.
     * Both overlays share this so the hatching looks identical on the minimap and the world map.
     */
    public static boolean isOnStripe(long sum, long stripePhase) {
        return Math.floorMod(sum + stripePhaseOffset(stripePhase), STRIPE_PERIOD) < STRIPE_WIDTH;
    }

    /**
     * How many consecutive pixels, starting at {@code pixelX} along a row, keep the same stripe
     * state as {@code pixelX} itself. Lets a scanline skip straight over the gaps between stripes
     * instead of testing every pixel in them.
     */
    public static int stripeRunLength(int pixelX, int pixelY, long stripePhase) {
        long offset = Math.floorMod(pixelX + pixelY + stripePhaseOffset(stripePhase), STRIPE_PERIOD);
        return offset < STRIPE_WIDTH
                ? (int) (STRIPE_WIDTH - offset)
                : (int) (STRIPE_PERIOD - offset);
    }
}
