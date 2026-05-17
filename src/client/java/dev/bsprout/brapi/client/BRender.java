package dev.bsprout.brapi.client;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public class BRender {
    public record RoundRectCmd(int x, int y, int w, int h, int color, int r1, int r2, int r3, int r4, int strokeWidth) {}
    private record RectCmd(int x, int y, int w, int h, int color) {}

    private final List<RoundRectCmd> roundRects = new ArrayList<>();
    private final List<RectCmd> rects = new ArrayList<>();
    public static final List<RoundRectCmd> PENDING = new ArrayList<>();

    private static final int MAX_RECTS = 4096;
    // Persistent native staging buffer for rect data - never freed
    private static final ByteBuffer RECT_DATA_STAGING = MemoryUtil.memAlloc(MAX_RECTS * 48);
    // Persistent per-rect vertex GPU buffers - created once
    private static GpuBuffer[] vertexGpuBufs = null;
    // Single persistent rect data GPU buffer
    private static GpuBuffer rectDataGpuBuf = null;

    // Example: bRender.roundRect(100, 100, 100, 100, 0xFFFF0000, 10);
    public void roundRect(int x, int y, int w, int h, int color, int radius) {
        roundRects.add(new RoundRectCmd(x, y, w, h, color, radius, radius, radius, radius, 0));
    }

    // Example: bRender.roundRect(100, 100, 100, 100, 0xFFFF0000, 10, 5, 20, 30);
    public void roundRect(int x, int y, int w, int h, int color, int r1, int r2, int r3, int r4) {
        roundRects.add(new RoundRectCmd(x, y, w, h, color, r1, r2, r3, r4, 0));
    }

    // R1 = top-left/bottom-right, R2 = top-right/bottom-left
    // Example: bRender.roundRect(100, 100, 100, 100, 0xFFFF0000, 10, 20);
    public void roundRect(int x, int y, int w, int h, int color, int r1, int r2) {
        roundRects.add(new RoundRectCmd(x, y, w, h, color, r1, r2, r1, r2, 0));
    }

    // Example: bRender.circle(100, 100, 50, 0xFFFF0000);
    public void circle(int x, int y, int radius, int color) {
        roundRects.add(new RoundRectCmd(x, y, radius * 2, radius * 2, color, radius, radius, radius, radius, 0));
    }

    // Example: bRender.rect(100, 100, 100, 100, 0xFFFF0000);
    public void rect(int x, int y, int w, int h, int color) {
        rects.add(new RectCmd(x, y, w, h, color));
    }

    // Example: bRender.stroke(100, 100, 100, 100, 0xFFFF0000, 2);
    public void stroke(int x, int y, int w, int h, int color, int strokeWidth) {
        rects.add(new RectCmd(x, y, w, strokeWidth, color));
        rects.add(new RectCmd(x, y + h - strokeWidth, w, strokeWidth, color));
        rects.add(new RectCmd(x, y + strokeWidth, strokeWidth, h - strokeWidth * 2, color));
        rects.add(new RectCmd(x + w - strokeWidth, y + strokeWidth, strokeWidth, h - strokeWidth * 2, color));
    }

    // Example: bRender.strokeRounded(100, 100, 100, 100, 0xFFFF0000, 10, 2);
    public void strokeRounded(int x, int y, int w, int h, int color, int radius, int strokeWidth) {
        roundRects.add(new RoundRectCmd(x, y, w, h, color, radius, radius, radius, radius, strokeWidth));
    }

    // R1 = top-left/bottom-right, R2 = top-right/bottom-left
    // Example: bRender.strokeRounded(100, 100, 100, 100, 0xFFFF0000, 10, 5, 2);
    public void strokeRounded(int x, int y, int w, int h, int color, int r1, int r2, int strokeWidth) {
        roundRects.add(new RoundRectCmd(x, y, w, h, color, r1, r2, r1, r2, strokeWidth));
    }

    // Example: bRender.strokeRounded(100, 100, 100, 100, 0xFFFF0000, 10, 5, 20, 40, 2);
    public void strokeRounded(int x, int y, int w, int h, int color, int r1, int r2, int r3, int r4, int strokeWidth) {
        roundRects.add(new RoundRectCmd(x, y, w, h, color, r1, r2, r3, r4, strokeWidth));
    }

    // Example: bRender.roundRectStroked(100, 100, 100, 100, 0xFF0000FF, 0xFFFF0000, 10, 2);
    public void roundRectStroked(int x, int y, int w, int h, int fillColor, int strokeColor, int radius, int strokeWidth) {
        roundRects.add(new RoundRectCmd(x, y, w, h, fillColor, radius, radius, radius, radius, 0));
        roundRects.add(new RoundRectCmd(x, y, w, h, strokeColor, radius, radius, radius, radius, strokeWidth));
    }

    // R1 = top-left/bottom-right, R2 = top-right/bottom-left
    // Example: bRender.roundRectStroked(100, 100, 100, 100, 0xFF0000FF, 0xFFFF0000, 10, 5, 2);
    public void roundRectStroked(int x, int y, int w, int h, int fillColor, int strokeColor, int r1, int r2, int strokeWidth) {
        roundRects.add(new RoundRectCmd(x, y, w, h, fillColor, r1, r2, r1, r2, 0));
        roundRects.add(new RoundRectCmd(x, y, w, h, strokeColor, r1, r2, r1, r2, strokeWidth));
    }

    // Example: bRender.roundRectStroked(100, 100, 100, 100, 0xFF0000FF, 0xFFFF0000, 10, 5, 20, 40, 2);
    public void roundRectStroked(int x, int y, int w, int h, int fillColor, int strokeColor, int r1, int r2, int r3, int r4, int strokeWidth) {
        roundRects.add(new RoundRectCmd(x, y, w, h, fillColor, r1, r2, r3, r4, 0));
        roundRects.add(new RoundRectCmd(x, y, w, h, strokeColor, r1, r2, r3, r4, strokeWidth));
    }

    // Call after adding all elements (e.g. rect, round rect)
    public void flush(GuiGraphics graphics) {
        for (RectCmd cmd : rects) {
            graphics.fill(cmd.x, cmd.y, cmd.x + cmd.w, cmd.y + cmd.h, cmd.color);
        }
        rects.clear();
        PENDING.addAll(roundRects);
        roundRects.clear();
    }

    /* Actually render everything (you shouldn't run this, its ran automagically)
       if you want you can steal this for your custom needs...
     */
    public static void flushPending() {
        if (PENDING.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        RenderTarget renderTarget = mc.getMainRenderTarget();
        Window window = mc.getWindow();
        int guiScale = window.getGuiScale();
        float screenH = window.getHeight();

        int count = Math.min(PENDING.size(), MAX_RECTS);

        // Lazily create persistent GPU buffers
        if (vertexGpuBufs == null) {
            vertexGpuBufs = new GpuBuffer[MAX_RECTS];
            for (int j = 0; j < MAX_RECTS; j++) {
                vertexGpuBufs[j] = RenderSystem.getDevice().createBuffer(
                        () -> "BRender vertex buffer",
                        GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
                        (long) 4 * 16
                );
            }
        }
        if (rectDataGpuBuf == null) {
            rectDataGpuBuf = RenderSystem.getDevice().createBuffer(
                    () -> "BRender RectData buffer",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                    (long) MAX_RECTS * 48
            );
        }

        RECT_DATA_STAGING.clear();

        RenderSystem.AutoStorageIndexBuffer indexStorage = RenderSystem.getSequentialBuffer(
                com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS);
        GpuBuffer indexBuf = indexStorage.getBuffer(6);

        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .writeTransform(
                        new Matrix4f().setTranslation(0.0F, 0.0F, -11000.0F),
                        new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
                        new Vector3f(),
                        new Matrix4f()
                );

        List<RenderPass.Draw<Void>> draws = new ArrayList<>(count);

        // Reusable per-rect vertex staging buffer
        ByteBuffer verts = MemoryUtil.memAlloc(4 * 16);

        for (int i = 0; i < count; i++) {
            RoundRectCmd cmd = PENDING.get(i);

            // Write vertices into reused staging buffer
            verts.clear();
            putVertex(verts, cmd.x,         cmd.y,         cmd.color);
            putVertex(verts, cmd.x,         cmd.y + cmd.h, cmd.color);
            putVertex(verts, cmd.x + cmd.w, cmd.y + cmd.h, cmd.color);
            putVertex(verts, cmd.x + cmd.w, cmd.y,         cmd.color);
            verts.flip();
            RenderSystem.getDevice().createCommandEncoder()
                    .writeToBuffer(vertexGpuBufs[i].slice(), verts);

            // Write rect data into staging
            RECT_DATA_STAGING.putFloat(cmd.x * guiScale);
            RECT_DATA_STAGING.putFloat(screenH - (cmd.y + cmd.h) * guiScale);
            RECT_DATA_STAGING.putFloat(cmd.w * guiScale);
            RECT_DATA_STAGING.putFloat(cmd.h * guiScale);
            RECT_DATA_STAGING.putFloat(cmd.r1 * guiScale);
            RECT_DATA_STAGING.putFloat(cmd.r2 * guiScale);
            RECT_DATA_STAGING.putFloat(cmd.r3 * guiScale);
            RECT_DATA_STAGING.putFloat(cmd.r4 * guiScale);
            RECT_DATA_STAGING.putFloat(cmd.strokeWidth * guiScale);
            RECT_DATA_STAGING.putFloat(0).putFloat(0).putFloat(0);

            final int rectDataOffset = i * 48;
            draws.add(new RenderPass.Draw<>(
                    0,
                    vertexGpuBufs[i],
                    indexBuf,
                    indexStorage.type(),
                    0,
                    6,
                    (unused, uploader) -> uploader.upload(
                            "RectData",
                            rectDataGpuBuf.slice(rectDataOffset, 48)
                    )
            ));
        }

        MemoryUtil.memFree(verts);

        // Upload rect data staging to GPU
        RECT_DATA_STAGING.flip();
        RenderSystem.getDevice().createCommandEncoder()
                .writeToBuffer(rectDataGpuBuf.slice(0, (long) count * 48), RECT_DATA_STAGING);

        try (RenderPass pass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(
                        () -> "BRender rounded rects",
                        renderTarget.getColorTextureView(),
                        OptionalInt.empty(),
                        renderTarget.useDepth ? renderTarget.getDepthTextureView() : null,
                        OptionalDouble.empty()
                )) {
            pass.setPipeline(Brapi.ROUNDED_RECT_PIPELINE);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", dynamicTransforms);
            pass.drawMultipleIndexed(draws, null, null, List.of("RectData"), null);
        }

        PENDING.clear();
    }

    private static void putVertex(ByteBuffer buf, int x, int y, int color) {
        buf.putFloat(x);
        buf.putFloat(y);
        buf.putFloat(0);
        buf.put((byte)((color >> 16) & 0xFF)); // r
        buf.put((byte)((color >> 8)  & 0xFF)); // g
        buf.put((byte)(color         & 0xFF)); // b
        buf.put((byte)((color >> 24) & 0xFF)); // a
    }
}