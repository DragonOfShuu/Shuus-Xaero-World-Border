package dev.dragonofshuu.shuusxaeroworldborder.overlays.misc;

import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

import org.joml.Matrix4f;

/**
 * Collects flat coloured quads and submits them in one {@link RenderType#gui()} draw call.
 *
 * <p>This is what the overlays use instead of {@link GuiGraphics#fill}, which ends its batch on
 * every single rectangle. The render state is identical to {@code fill}'s, with two consequences
 * worth remembering:</p>
 *
 * <ul>
 *   <li>{@link RenderType#gui()} leaves face culling enabled, so quads must be wound the same way
 *       {@code GuiGraphics.fill} winds them. {@link #rect} already is; {@link #quad} is the
 *       caller's responsibility.</li>
 *   <li>Nothing is drawn until {@link #end()} is called, so anything that has to sit underneath must
 *       be flushed by the caller first.</li>
 * </ul>
 */
public final class GuiQuadBatch {
    private final Matrix4f matrix;
    private final MultiBufferSource.BufferSource buffers;

    private VertexConsumer vertexConsumer;
    private boolean open;

    public GuiQuadBatch(GuiGraphics guiGraphics) {
        this.matrix = guiGraphics.pose().last().pose();
        this.buffers = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
    }

    /** Starts collecting quads. Call {@link #end()} to actually draw them. */
    public void begin() {
        if (this.open) {
            return;
        }
        this.vertexConsumer = this.buffers.getBuffer(RenderType.gui());
        this.open = true;
    }

    /** Submits everything collected since {@link #begin()} and releases the buffer. */
    public void end() {
        if (!this.open) {
            return;
        }
        // Always closed, even when empty, so the shared BufferBuilder is never left mid-build.
        this.buffers.endBatch();
        this.open = false;
    }

    /**
     * Adds an axis-aligned rectangle, wound so it survives {@link RenderType#gui()}'s face culling.
     */
    public void rect(float x0, float y0, float x1, float y1, int color) {
        this.quad(x0, y0, x0, y1, x1, y1, x1, y0, color);
    }

    /**
     * Adds a solid line of the given thickness as a single quad, wound the same way {@link #rect}
     * is. Degenerate lines and zero thickness add nothing.
     */
    public void line(float x0, float y0, float x1, float y1, float thickness, int color) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length <= 0.0f || thickness <= 0.0f) {
            return;
        }

        // Unit normal to the direction of travel, pointing at the same side the left edge of rect()
        // sits on for a horizontal line, which keeps the winding front-facing in every direction.
        float nx = -dy / length * (thickness / 2.0f);
        float ny = dx / length * (thickness / 2.0f);

        this.quad(
                x0 - nx, y0 - ny,
                x0 + nx, y0 + ny,
                x1 + nx, y1 + ny,
                x1 - nx, y1 - ny,
                color);
    }

    /**
     * Adds an arbitrary four point polygon. The corners must be given in front-facing order, which
     * for the flipped GUI projection means the same order {@code GuiGraphics.fill} uses: top-left,
     * bottom-left, bottom-right, top-right.
     */
    public void quad(
            float x0,
            float y0,
            float x1,
            float y1,
            float x2,
            float y2,
            float x3,
            float y3,
            int color) {
        if (!this.open) {
            return;
        }
        this.vertexConsumer.vertex(this.matrix, x0, y0, 0.0f).color(color).endVertex();
        this.vertexConsumer.vertex(this.matrix, x1, y1, 0.0f).color(color).endVertex();
        this.vertexConsumer.vertex(this.matrix, x2, y2, 0.0f).color(color).endVertex();
        this.vertexConsumer.vertex(this.matrix, x3, y3, 0.0f).color(color).endVertex();
    }
}
