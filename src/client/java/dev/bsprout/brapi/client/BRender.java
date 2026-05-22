package dev.bsprout.brapi.client;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.*;
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

    private record TextCmd(BFont font, String text, float x, float y, float size, int color) {}
    private final List<TextCmd> texts = new ArrayList<>();
    public static final List<TextCmd> PENDING_TEXT = new ArrayList<>();

    private record BlurCmd(int x, int y, int w, int h, float strength) {}
    private final List<BlurCmd> blurs = new ArrayList<>();
    public static final List<BlurCmd> PENDING_BLUR = new ArrayList<>();

    public enum RenderMode { STRETCH, TILE, CROP }

    private record TextureCmd(
            BTexture texture,
            float x, float y, float w, float h,
            float u0, float v0, float u1, float v1,
            int color,
            boolean tile,
            boolean linear
    ) {}

    private record NineSliceCmd(
            NineSlice slice,
            float x, float y, float w, float h,
            int color,
            boolean linear
    ) {}

    private final List<TextureCmd> textures = new ArrayList<>();
    private final List<NineSliceCmd> nineSlices = new ArrayList<>();
    public static final List<TextureCmd> PENDING_TEXTURES = new ArrayList<>();
    public static final List<NineSliceCmd> PENDING_NINE_SLICES = new ArrayList<>();


    private static final int MAX_RECTS = 4096;
    // Persistent native staging buffer for rect data - never freed
    private static final ByteBuffer RECT_DATA_STAGING = MemoryUtil.memAlloc(MAX_RECTS * 48);
    // Persistent per-rect vertex GPU buffers - created once
    private static GpuBuffer[] vertexGpuBufs = null;
    // Single persistent rect data GPU buffer
    private static GpuBuffer rectDataGpuBuf = null;

    private static GpuTexture blurredScreenTex = null;
    private static GpuTextureView blurredScreenView = null;
    private static int lastBlurW = 0, lastBlurH = 0;

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

    // Example: bRender.drawText(myFont, "Hello!", 100, 100, 24, 0xFFFFFFFF);
    public void drawText(BFont font, String text, float x, float y, float size, int color) {
        texts.add(new TextCmd(font, text, x, y, size, color));
    }

    // Example: bRender.blur(50, 50, 200, 100, 5.0f);
    public void blur(int x, int y, int w, int h, float strength) {
        blurs.add(new BlurCmd(x, y, w, h, strength));
    }

    // Stretch texture to fill region
    // Example: bRender.drawTexture(tex, 100, 100, 200, 150, 0xFFFFFFFF);
    public void drawTexture(BTexture texture, float x, float y, float w, float h, int color, boolean linear) {
        textures.add(new TextureCmd(texture, x, y, w, h, 0, 0, 1, 1, color, false, linear));
    }

    // Crop - draw sub-region of texture
    // srcX, srcY, srcW, srcH in pixels
    // Example: bRender.drawTextureCropped(tex, 100, 100, 64, 64, 0, 0, 32, 32, 0xFFFFFFFF);
    public void drawTextureCropped(BTexture texture, float x, float y, float w, float h,
                                   int srcX, int srcY, int srcW, int srcH, int color, boolean linear) {
        float u0 = (float) srcX / texture.width;
        float v0 = (float) srcY / texture.height;
        float u1 = (float) (srcX + srcW) / texture.width;
        float v1 = (float) (srcY + srcH) / texture.height;
        textures.add(new TextureCmd(texture, x, y, w, h, u0, v0, u1, v1, color, false, linear));
    }

    // Tile texture across region
    // Example: bRender.drawTextureTiled(tex, 100, 100, 200, 200, 0xFFFFFFFF);
    public void drawTextureTiled(BTexture texture, float x, float y, float w, float h, int color, boolean linear) {
        float u1 = w / texture.width;
        float v1 = h / texture.height;
        textures.add(new TextureCmd(texture, x, y, w, h, 0, 0, u1, v1, color, true, linear));
    }

    // 9-slice
    // Example: bRender.drawTexture9Slice(slice, 100, 100, 300, 200, 0xFFFFFFFF);
    public void drawTexture9Slice(NineSlice slice, float x, float y, float w, float h, int color, boolean linear) {
        nineSlices.add(new NineSliceCmd(slice, x, y, w, h, color, linear));
    }

    // Helper - create NineSlice from texture and border sizes
    // Example: NineSlice slice = BRender.nineslicify(tex, 8, 8, 8, 8);
    public static NineSlice nineslicify(BTexture texture, int borderTop, int borderRight, int borderBottom, int borderLeft) {
        return new NineSlice(texture, borderTop, borderRight, borderBottom, borderLeft);
    }

    // Call after adding all elements (e.g. rect, round rect)
    public void flush(GuiGraphics graphics) {
        for (RectCmd cmd : rects) {
            graphics.fill(cmd.x, cmd.y, cmd.x + cmd.w, cmd.y + cmd.h, cmd.color);
        }
        rects.clear();
        PENDING.addAll(roundRects);
        roundRects.clear();

        PENDING_TEXT.addAll(texts);
        texts.clear();

        PENDING_BLUR.addAll(blurs);
        blurs.clear();

        PENDING_TEXTURES.addAll(textures);
        PENDING_NINE_SLICES.addAll(nineSlices);

        textures.clear();
        nineSlices.clear();
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

    // flushPending but for text
    public static void flushPendingText() {
        if (PENDING_TEXT.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        RenderTarget renderTarget = mc.getMainRenderTarget();
        Window window = mc.getWindow();

        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .writeTransform(
                        new Matrix4f().setTranslation(0.0F, 0.0F, -11000.0F),
                        new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
                        new Vector3f(),
                        new Matrix4f()
                );

        GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);

        for (TextCmd cmd : PENDING_TEXT) {
            float[][] quads = cmd.font().getQuads(cmd.text(), cmd.x(), cmd.y(), cmd.size());

            // Each char = 4 verts * 20 bytes (POSITION_TEX_COLOR)
            ByteBuffer verts = MemoryUtil.memAlloc(quads.length * 4 * 24);

            for (float[] q : quads) {
                float x0 = q[0], y0 = q[1], x1 = q[2], y1 = q[3];
                float u0 = q[4], v0 = q[5], u1 = q[6], v1 = q[7];
                putTextVertex(verts, x0, y0, u0, v0, cmd.color());
                putTextVertex(verts, x0, y1, u0, v1, cmd.color());
                putTextVertex(verts, x1, y1, u1, v1, cmd.color());
                putTextVertex(verts, x1, y0, u1, v0, cmd.color());
            }
            verts.flip();

            int indexCount = quads.length * 6;
            RenderSystem.AutoStorageIndexBuffer indexStorage =
                    RenderSystem.getSequentialBuffer(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS);
            GpuBuffer indexBuf = indexStorage.getBuffer(indexCount);

            GpuBuffer vertexBuf = RenderSystem.getDevice().createBuffer(
                    () -> "BRender text vertices",
                    GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
                    verts.remaining()
            );
            RenderSystem.getDevice().createCommandEncoder()
                    .writeToBuffer(vertexBuf.slice(), verts);
            MemoryUtil.memFree(verts);

            try (RenderPass pass = RenderSystem.getDevice()
                    .createCommandEncoder()
                    .createRenderPass(
                            () -> "BRender text",
                            renderTarget.getColorTextureView(),
                            OptionalInt.empty(),
                            renderTarget.useDepth ? renderTarget.getDepthTextureView() : null,
                            OptionalDouble.empty()
                    )) {
                pass.setPipeline(Brapi.TEXT_PIPELINE);
                RenderSystem.bindDefaultUniforms(pass);
                pass.setUniform("DynamicTransforms", dynamicTransforms);
                pass.bindTexture("Sampler0", cmd.font().atlasView, sampler);
                pass.setVertexBuffer(0, vertexBuf);
                pass.setIndexBuffer(indexBuf, indexStorage.type());
                pass.drawIndexed(0, 0, indexCount, 1);
            }

            vertexBuf.close();
        }

        PENDING_TEXT.clear();
    }

    public static void flushPendingBlur() {
        if (true) return;
        if (PENDING_BLUR.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        RenderTarget renderTarget = mc.getMainRenderTarget();
        Window window = mc.getWindow();
        int guiScale = window.getGuiScale();
        int screenW = window.getWidth();
        int screenH = window.getHeight();

        if (blurredScreenTex == null || lastBlurW != screenW || lastBlurH != screenH) {
            if (blurredScreenTex != null) {
                blurredScreenView.close();
                blurredScreenTex.close();
            }
            blurredScreenTex = RenderSystem.getDevice().createTexture(
                    "BRender full blur copy",
                    GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_RENDER_ATTACHMENT,
                    TextureFormat.RGBA8,
                    screenW, screenH, 1, 1
            );
            blurredScreenView = RenderSystem.getDevice().createTextureView(blurredScreenTex);
            lastBlurW = screenW;
            lastBlurH = screenH;
        }

        var encoder = RenderSystem.getDevice().createCommandEncoder();

        encoder.copyTextureToTexture(
                renderTarget.getColorTexture(),
                blurredScreenTex,
                0, 0, 0, 0, 0,
                screenW, screenH
        );

        GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .writeTransform(
                        new Matrix4f().setTranslation(0.0F, 0.0F, -11000.0F),
                        new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
                        new Vector3f(),
                        new Matrix4f()
                );

        for (BlurCmd cmd : PENDING_BLUR) {
            int px = cmd.x() * guiScale;
            int py = cmd.y() * guiScale;
            int pw = cmd.w() * guiScale;
            int ph = cmd.h() * guiScale;

            float u0 = (float) px / screenW;
            float v0 = (float) py / screenH;
            float u1 = (float) (px + pw) / screenW;
            float v1 = (float) (py + ph) / screenH;

            int byteBlockSize = 48;
            ByteBuffer blurData = MemoryUtil.memAlloc(byteBlockSize);
            blurData.clear();

            blurData.putFloat((float) screenW);
            blurData.putFloat((float) screenH);
            blurData.putFloat(0.0F);
            blurData.putFloat(0.0F);

            blurData.putFloat((float) px);
            blurData.putFloat((float) (screenH - py - ph));
            blurData.putFloat((float) pw);
            blurData.putFloat((float) ph);

            blurData.putFloat(cmd.strength());
            blurData.putFloat(0.0F);
            blurData.putFloat(0.0F);
            blurData.putFloat(0.0F);

            blurData.position(0);
            blurData.limit(byteBlockSize);

            GpuBuffer blurDataBuf = RenderSystem.getDevice().createBuffer(
                    () -> "BRender BlurData Block",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                    blurData
            );
            MemoryUtil.memFree(blurData);

            float x0 = cmd.x(), y0 = cmd.y();
            float x1 = x0 + cmd.w(), y1 = y0 + cmd.h();

            ByteBuffer verts = MemoryUtil.memAlloc(4 * 24);
            putTextVertex(verts, x0, y0, u0, v0, 0xFFFFFFFF);
            putTextVertex(verts, x0, y1, u0, v1, 0xFFFFFFFF);
            putTextVertex(verts, x1, y1, u1, v1, 0xFFFFFFFF);
            putTextVertex(verts, x1, y0, u1, v0, 0xFFFFFFFF);
            verts.flip();

            GpuBuffer vertexBuf = RenderSystem.getDevice().createBuffer(
                    () -> "BRender Blur Vertices",
                    GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
                    verts.remaining()
            );

            // Write vertex components directly onto our open shared execution encoder
            encoder.writeToBuffer(vertexBuf.slice(), verts);
            MemoryUtil.memFree(verts);

            RenderSystem.AutoStorageIndexBuffer indexStorage =
                    RenderSystem.getSequentialBuffer(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS);
            GpuBuffer indexBuf = indexStorage.getBuffer(6);

            try (RenderPass pass = encoder.createRenderPass(
                    () -> "BRender Blur Quad Pass",
                    renderTarget.getColorTextureView(),
                    OptionalInt.empty(),
                    renderTarget.useDepth ? renderTarget.getDepthTextureView() : null,
                    OptionalDouble.empty()
            )) {
                pass.setPipeline(Brapi.BLUR_PIPELINE);
                RenderSystem.bindDefaultUniforms(pass);
                pass.setUniform("DynamicTransforms", dynamicTransforms);
                pass.setUniform("BlurData", blurDataBuf);
                pass.bindTexture("Sampler0", blurredScreenView, sampler);
                pass.setVertexBuffer(0, vertexBuf);
                pass.setIndexBuffer(indexBuf, indexStorage.type());
                pass.drawIndexed(0, 0, 6, 1);
            }

            vertexBuf.close();
            blurDataBuf.close();
        }

        PENDING_BLUR.clear();
    }

    public static void flushPendingTextures() {
        if (PENDING_TEXTURES.isEmpty() && PENDING_NINE_SLICES.isEmpty()) return;
        GpuSampler samplerClampLinear = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        GpuSampler samplerClampNearest = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
        GpuSampler samplerRepeatLinear = RenderSystem.getSamplerCache().getRepeat(FilterMode.LINEAR);
        GpuSampler samplerRepeatNearest = RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST);

        Minecraft mc = Minecraft.getInstance();
        RenderTarget renderTarget = mc.getMainRenderTarget();

        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .writeTransform(
                        new Matrix4f().setTranslation(0.0F, 0.0F, -11000.0F),
                        new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
                        new Vector3f(),
                        new Matrix4f()
                );

        GpuSampler samplerLinear = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        GpuSampler samplerRepeat = RenderSystem.getSamplerCache().getRepeat(FilterMode.LINEAR);

        // Draw regular textures
        for (TextureCmd cmd : PENDING_TEXTURES) {
            GpuSampler sampler = cmd.tile()
                    ? (cmd.linear() ? samplerRepeatLinear : samplerRepeatNearest)
                    : (cmd.linear() ? samplerClampLinear : samplerClampNearest);
            drawTextureQuad(renderTarget, dynamicTransforms, sampler,
                    cmd.texture().view,
                    cmd.x(), cmd.y(), cmd.w(), cmd.h(),
                    cmd.u0(), cmd.v0(), cmd.u1(), cmd.v1(),
                    cmd.color());
        }

        // Draw 9-slice textures
        for (NineSliceCmd cmd : PENDING_NINE_SLICES) {
            GpuSampler sampler = cmd.linear() ? samplerClampLinear : samplerClampNearest;
            drawNineSlice(renderTarget, dynamicTransforms, sampler, cmd);
        }

        PENDING_TEXTURES.clear();
        PENDING_NINE_SLICES.clear();
    }

    private static void drawNineSlice(RenderTarget renderTarget, GpuBufferSlice dynamicTransforms,
                                      GpuSampler sampler, NineSliceCmd cmd) {
        NineSlice s = cmd.slice();
        BTexture tex = s.texture();
        float x = cmd.x(), y = cmd.y(), w = cmd.w(), h = cmd.h();
        int color = cmd.color();

        int bt = s.borderTop(), br = s.borderRight(), bb = s.borderBottom(), bl = s.borderLeft();

        // UV fractions
        float ubl = (float) bl / tex.width;
        float ubr = (float) (tex.width - br) / tex.width;
        float vbt = (float) bt / tex.height;
        float vbb = (float) (tex.height - bb) / tex.height;

        // Screen positions
        float xbl = x + bl, xbr = x + w - br;
        float ybt = y + bt, ybb = y + h - bb;

        // 9 quads: corners, edges, center
        // Top-left corner
        drawTextureQuad(renderTarget, dynamicTransforms, sampler, tex.view,
                x, y, bl, bt, 0, 0, ubl, vbt, color);
        // Top-right corner
        drawTextureQuad(renderTarget, dynamicTransforms, sampler, tex.view,
                xbr, y, br, bt, ubr, 0, 1, vbt, color);
        // Bottom-left corner
        drawTextureQuad(renderTarget, dynamicTransforms, sampler, tex.view,
                x, ybb, bl, bb, 0, vbb, ubl, 1, color);
        // Bottom-right corner
        drawTextureQuad(renderTarget, dynamicTransforms, sampler, tex.view,
                xbr, ybb, br, bb, ubr, vbb, 1, 1, color);
        // Top edge
        drawTextureQuad(renderTarget, dynamicTransforms, sampler, tex.view,
                xbl, y, w - bl - br, bt, ubl, 0, ubr, vbt, color);
        // Bottom edge
        drawTextureQuad(renderTarget, dynamicTransforms, sampler, tex.view,
                xbl, ybb, w - bl - br, bb, ubl, vbb, ubr, 1, color);
        // Left edge
        drawTextureQuad(renderTarget, dynamicTransforms, sampler, tex.view,
                x, ybt, bl, h - bt - bb, 0, vbt, ubl, vbb, color);
        // Right edge
        drawTextureQuad(renderTarget, dynamicTransforms, sampler, tex.view,
                xbr, ybt, br, h - bt - bb, ubr, vbt, 1, vbb, color);
        // Center
        drawTextureQuad(renderTarget, dynamicTransforms, sampler, tex.view,
                xbl, ybt, w - bl - br, h - bt - bb, ubl, vbt, ubr, vbb, color);
    }

    private static void drawTextureQuad(RenderTarget renderTarget, GpuBufferSlice dynamicTransforms,
                                        GpuSampler sampler, GpuTextureView view,
                                        float x, float y, float w, float h,
                                        float u0, float v0, float u1, float v1,
                                        int color) {
        ByteBuffer verts = MemoryUtil.memAlloc(4 * 24);
        putTextVertex(verts, x,     y,     u0, v0, color);
        putTextVertex(verts, x,     y + h, u0, v1, color);
        putTextVertex(verts, x + w, y + h, u1, v1, color);
        putTextVertex(verts, x + w, y,     u1, v0, color);
        verts.flip();

        GpuBuffer vertexBuf = RenderSystem.getDevice().createBuffer(
                () -> "BRender texture vertices",
                GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
                verts.remaining()
        );
        RenderSystem.getDevice().createCommandEncoder()
                .writeToBuffer(vertexBuf.slice(), verts);
        MemoryUtil.memFree(verts);

        RenderSystem.AutoStorageIndexBuffer indexStorage =
                RenderSystem.getSequentialBuffer(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS);
        GpuBuffer indexBuf = indexStorage.getBuffer(6);

        try (RenderPass pass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(
                        () -> "BRender texture",
                        renderTarget.getColorTextureView(),
                        OptionalInt.empty(),
                        renderTarget.useDepth ? renderTarget.getDepthTextureView() : null,
                        OptionalDouble.empty()
                )) {
            pass.setPipeline(Brapi.TEXTURE_PIPELINE);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", dynamicTransforms);
            pass.bindTexture("Sampler0", view, sampler);
            pass.setVertexBuffer(0, vertexBuf);
            pass.setIndexBuffer(indexBuf, indexStorage.type());
            pass.drawIndexed(0, 0, 6, 1);
        }

        vertexBuf.close();
    }

    private static void putTextVertex(ByteBuffer buf, float x, float y, float u, float v, int color) {
        buf.putFloat(x);
        buf.putFloat(y);
        buf.putFloat(0);
        buf.putFloat(u);
        buf.putFloat(v);
        buf.put((byte)((color >> 16) & 0xFF)); // r
        buf.put((byte)((color >> 8)  & 0xFF)); // g
        buf.put((byte)(color         & 0xFF)); // b
        buf.put((byte)((color >> 24) & 0xFF)); // a
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