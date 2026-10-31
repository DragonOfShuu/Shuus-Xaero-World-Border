package dev.dragonofshuu.shuusxaeroworldborder.overlays.types;

/**
 * An axis aligned rectangle in GUI pixels.
 *
 * <p>The world map never rotates, so every world border rectangle - the current one and the one it
 * is moving towards - is axis aligned on screen and can be carried around as one of these.</p>
 *
 * <p>Edges are deliberately allowed to fall outside of the screen; each renderer clips as it draws
 * so that a border far off screen costs nothing.</p>
 */
public final class ScreenRect {
    public final float left;
    public final float top;
    public final float right;
    public final float bottom;

    public ScreenRect(float left, float top, float right, float bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public float width() {
        return this.right - this.left;
    }

    public float height() {
        return this.bottom - this.top;
    }
}
