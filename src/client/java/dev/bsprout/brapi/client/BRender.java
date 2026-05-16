package dev.bsprout.brapi.client;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
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
    public record RoundRectCmd(int x, int y, int w, int h, int color, int radius) {}
    private record RectCmd(int x, int y, int w, int h, int color) {}

    private final List<RoundRectCmd> roundRects = new ArrayList<>();
    private final List<RectCmd> rects = new ArrayList<>();
    public static final List<RoundRectCmd> PENDING = new ArrayList<>();

    // Example: bRender.roundRect(100, 100, 100, 100, 0xFFFF0000, 10);
    public void roundRect(int x, int y, int w, int h, int color, int radius) {
        roundRects.add(new RoundRectCmd(x, y, w, h, color, radius));
    }

    // Example: bRender.circle(100, 100, 50, 0xFFFF0000);
    public void circle(int x, int y, int radius, int color) {
        roundRects.add(new RoundRectCmd(x, y, radius * 2, radius * 2, color, radius));
    }

    // Example: bRender.rect(100, 100, 100, 100, 0xFFFF0000);
    public void rect(int x, int y, int w, int h, int color) {
        rects.add(new RectCmd(x, y, w, h, color));
    }

    // Call after adding all elements (e.g rect, round rect)
    public void flush(GuiGraphics graphics) {
        for (RectCmd cmd : rects) {
            graphics.fill(cmd.x, cmd.y, cmd.x + cmd.w, cmd.y + cmd.h, cmd.color);
        }
        rects.clear();
        PENDING.addAll(roundRects);
        roundRects.clear();
    }

    // Actually render everything (you shouldn't run this, its ran automagically)
    public static void flushPending() {
        if (PENDING.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();

        RenderTarget renderTarget = mc.getMainRenderTarget();
        Window window = mc.getWindow();

        int guiScale = window.getGuiScale();
        float screenH = window.getHeight();

        RenderSystem.AutoStorageIndexBuffer indexStorage = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        GpuBuffer indexBuf = indexStorage.getBuffer(6);

        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .writeTransform(
                        new Matrix4f().setTranslation(0.0F, 0.0F, -11000.0F),
                        new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
                        new Vector3f(),
                        new Matrix4f()
                );

        for (RoundRectCmd cmd : PENDING) {
            ByteBufferBuilder allocator = new ByteBufferBuilder(4 * 16);
            BufferBuilder builder = new BufferBuilder(allocator, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

            builder.addVertex(cmd.x,         cmd.y,         0).setColor(cmd.color);
            builder.addVertex(cmd.x,         cmd.y + cmd.h, 0).setColor(cmd.color);
            builder.addVertex(cmd.x + cmd.w, cmd.y + cmd.h, 0).setColor(cmd.color);
            builder.addVertex(cmd.x + cmd.w, cmd.y,         0).setColor(cmd.color);

            MeshData mesh = builder.buildOrThrow();
            MeshData.DrawState draw = mesh.drawState();

            GpuBuffer vertexBuf = RenderSystem.getDevice().createBuffer(
                    () -> "BRender vertices",
                    GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
                    mesh.vertexBuffer().remaining()
            );
            RenderSystem.getDevice().createCommandEncoder()
                    .writeToBuffer(vertexBuf.slice(), mesh.vertexBuffer());
            mesh.close();
            allocator.close();

            ByteBuffer rectData = MemoryUtil.memAlloc(32);
            rectData.putFloat(cmd.x * guiScale);
            rectData.putFloat(screenH - (cmd.y + cmd.h) * guiScale);
            rectData.putFloat(cmd.w * guiScale);
            rectData.putFloat(cmd.h * guiScale);
            rectData.putFloat(cmd.radius * guiScale);
            rectData.putFloat(0).putFloat(0).putFloat(0);
            rectData.flip();

            GpuBuffer rectDataBuf = RenderSystem.getDevice().createBuffer(
                    () -> "BRender RectData",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                    rectData
            );
            MemoryUtil.memFree(rectData);

            try (RenderPass pass = RenderSystem.getDevice()
                    .createCommandEncoder()
                    .createRenderPass(
                            () -> "BRender rounded rect",
                            renderTarget.getColorTextureView(),
                            OptionalInt.empty(),
                            renderTarget.useDepth ? renderTarget.getDepthTextureView() : null,
                            OptionalDouble.empty()
                    )) {
                pass.setPipeline(Brapi.ROUNDED_RECT_PIPELINE);
                RenderSystem.bindDefaultUniforms(pass);
                pass.setUniform("DynamicTransforms", dynamicTransforms);
                pass.setUniform("RectData", rectDataBuf);
                pass.setVertexBuffer(0, vertexBuf);
                pass.setIndexBuffer(indexBuf, indexStorage.type());
                pass.drawIndexed(0, 0, draw.indexCount(), 1);
            }

            vertexBuf.close();
            rectDataBuf.close();
        }

        PENDING.clear();
    }
}