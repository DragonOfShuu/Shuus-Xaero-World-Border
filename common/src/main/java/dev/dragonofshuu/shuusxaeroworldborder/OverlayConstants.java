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
    /**
     * Opaque white, used on both overlays for the border a moving border is heading towards and for
     * the guide line pointing at it. White because it has to stay readable over the hatching.
     */
    public static final int DESTINATION_COLOR = 0xFFFFFFFF;

    /** Stripe opacity, as a fraction of fully opaque. */
    public static final float STRIPE_OPACITY = 0.65f;
    /**
     * Underlay opacity, as a fraction of fully opaque. Lower than {@link #STRIPE_OPACITY} so the
     * stripes still read clearly on top of it.
     */
    public static final float UNDERLAY_OPACITY = 0.40f;
    /** Stripe brightness, as a fraction of vanilla's border colour. Below 1 darkens it. */
    public static final float STRIPE_SHADE = 0.7f;
    /** Underlay brightness - darker than the stripes, so the hatching stays the bright layer. */
    public static final float UNDERLAY_SHADE = 0.49f;

    /** Stripe fill, drawn outside of the border. Darker than vanilla's border colour. */
    public static int getStripeColor(WorldBorderState border) {
        return borderColor(border, STRIPE_SHADE, STRIPE_OPACITY);
    }

    /**
     * Flat fill laid under the stripes, covering all of the area outside the border rather than
     * only the stripes themselves. Darker and fainter than {@link #getStripeColor}, so it softens
     * the map behind the hatching without competing with it.
     */
    public static int getStripeUnderlayColor(WorldBorderState border) {
        return borderColor(border, UNDERLAY_SHADE, UNDERLAY_OPACITY);
    }

    /**
     * Packs vanilla's border colour into an ARGB int, scaled by {@code shade} and given the alpha
     * {@code opacity} describes.
     *
     * <p>{@link BorderStatus#getColor()} returns {@code 0x00RRGGBB} with the alpha byte left empty,
     * so the alpha has to be supplied here. The channels are reassembled rather than OR-ed into the
     * raw value, which keeps that correct whatever vanilla ever puts in the alpha byte.</p>
     */
    private static int borderColor(WorldBorderState border, float shade, float opacity) {
        Color color = new Color(border.status.getColor());
        int alpha = Math.round(255.0f * opacity);
        int red = Math.round(color.getRed() * shade);
        int green = Math.round(color.getGreen() * shade);
        int blue = Math.round(color.getBlue() * shade);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
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
