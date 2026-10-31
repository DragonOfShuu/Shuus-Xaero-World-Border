package dev.dragonofshuu.shuusxaeroworldborder.overlays.misc;

import dev.dragonofshuu.shuusxaeroworldborder.OverlayConstants;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.BorderStatus;
import net.minecraft.world.level.border.WorldBorder;

/**
 * Immutable snapshot of the client world border, shared by the minimap and world map overlays.
 *
 * <p>Holds the border bounds in world blocks plus everything needed to animate and tint the
 * overlay, so both overlays stay in sync with each other and with vanilla's border colours.</p>
 */
public final class WorldBorderState {
    public final double minX;
    public final double maxX;
    public final double minZ;
    public final double maxZ;
    /**
     * The bounds the border is currently moving towards. While the border is stationary vanilla
     * reports the current size as the lerp target, so these collapse onto {@link #minX} and friends
     * and carry no meaning.
     */
    public final double targetMinX;
    public final double targetMaxX;
    public final double targetMinZ;
    public final double targetMaxZ;
    public final BorderStatus status;
    /** Monotonically increasing offset used to scroll the diagonal stripe pattern. */
    public final long stripePhase;

    private WorldBorderState(
            double minX,
            double maxX,
            double minZ,
            double maxZ,
            double targetMinX,
            double targetMaxX,
            double targetMinZ,
            double targetMaxZ,
            BorderStatus status,
            long stripePhase) {
        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
        this.targetMinX = targetMinX;
        this.targetMaxX = targetMaxX;
        this.targetMinZ = targetMinZ;
        this.targetMaxZ = targetMaxZ;
        this.status = status;
        this.stripePhase = stripePhase;
    }

    /**
     * Reads the border straight from the client level. Returns {@code null} when there is no level.
     */
    public static WorldBorderState fromLevel(Level level) {
        if (level == null) {
            return null;
        }

        WorldBorder border = level.getWorldBorder();
        BorderStatus status = liveStatus(border);
        long stripePhase = System.currentTimeMillis() / OverlayConstants.getStripeAnimation(status);

        // The lerp target is a size, not a rectangle: the border always shrinks and grows towards
        // its own centre, so the destination bounds are derived from the centre the same way
        // MovingBorderExtent derives the current ones.
        double halfTarget = border.getLerpTarget() / 2.0;
        double centerX = border.getCenterX();
        double centerZ = border.getCenterZ();

        return new WorldBorderState(
                border.getMinX(),
                border.getMaxX(),
                border.getMinZ(),
                border.getMaxZ(),
                centerX - halfTarget,
                centerX + halfTarget,
                centerZ - halfTarget,
                centerZ + halfTarget,
                status,
                stripePhase);
    }

    /**
     * The border status, derived from live data instead of {@link WorldBorder#getStatus()}.
     *
     * <p>{@code WorldBorder#getStatus()} is not a query, it is a cached value: it delegates to the
     * current extent, and a {@code MovingBorderExtent} only ever reports GROWING or SHRINKING. The
     * swap back to a static extent - and therefore back to STATIONARY - happens in
     * {@code WorldBorder#tick()}, which on the client is only reached from {@code ClientLevel#tick}.
     * So whenever the client level is not ticking, the status sticks at whatever it was when the
     * border last started moving, even though the border geometry itself keeps animating and
     * settling because the extent interpolates from wall clock time.</p>
     *
     * <p>That is exactly what happens while the world map screen is open: the outline moves and
     * stops correctly, but the colour and stripe speed freeze. Doing the same remaining-time check
     * that {@code MovingBorderExtent#update()} uses, inline and per frame, removes the dependency on
     * ticking and gives vanilla's answer whenever the level <em>is</em> ticking.</p>
     */
    private static BorderStatus liveStatus(WorldBorder border) {
        if (border.getLerpRemainingTime() <= 0L) {
            return BorderStatus.STATIONARY;
        }
        return border.getLerpTarget() < border.getSize()
                ? BorderStatus.SHRINKING
                : BorderStatus.GROWING;
    }

    /** True while the border is actively growing or shrinking towards the {@code target*} bounds. */
    public boolean isMoving() {
        return this.status != BorderStatus.STATIONARY;
    }

    /** True when the block position is inside the border (on the border line counts as inside). */
    public boolean contains(double worldX, double worldZ) {
        return worldX >= this.minX && worldX <= this.maxX && worldZ >= this.minZ && worldZ <= this.maxZ;
    }

    /** True when the block position is outside of the border. */
    public boolean isOutside(double worldX, double worldZ) {
        return !this.contains(worldX, worldZ);
    }
}
