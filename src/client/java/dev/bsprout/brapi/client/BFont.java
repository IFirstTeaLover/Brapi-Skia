package dev.bsprout.brapi.client;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryUtil;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class BFont {
    private static final int ATLAS_SIZE = 1024;
    private static final int BAKE_SIZE = 128;
    private static final int FIRST_CHAR = 32;
    private static final int NUM_CHARS = 96; // ASCII 32-128

    private final STBTTBakedChar.Buffer charData;
    private final GpuTexture atlasTexture;
    final GpuTextureView atlasView;

    public BFont(Identifier fontPath) {
        try {
            // 1. Read TTF bytes
            InputStream stream = Minecraft.getInstance()
                    .getResourceManager()
                    .open(fontPath);
            byte[] ttfBytes = stream.readAllBytes();
            stream.close();

            ByteBuffer ttfBuf = MemoryUtil.memAlloc(ttfBytes.length);
            ttfBuf.put(ttfBytes).flip();

            // 2. Bake at 128px into a single-channel bitmap
            ByteBuffer bitmap = MemoryUtil.memAlloc(ATLAS_SIZE * ATLAS_SIZE);
            charData = STBTTBakedChar.malloc(NUM_CHARS);
            STBTruetype.stbtt_BakeFontBitmap(
                    ttfBuf, BAKE_SIZE,
                    bitmap,
                    ATLAS_SIZE, ATLAS_SIZE,
                    FIRST_CHAR, charData
            );
            MemoryUtil.memFree(ttfBuf);

            // 3. Upload to GPU as RED8 (single channel grayscale)
            atlasTexture = RenderSystem.getDevice().createTexture(
                    "BFont atlas",
                    GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST,
                    TextureFormat.RED8,
                    ATLAS_SIZE, ATLAS_SIZE, 1, 1
            );

            RenderSystem.getDevice().createCommandEncoder()
                    .writeToTexture(
                            atlasTexture,
                            bitmap,
                            NativeImage.Format.LUMINANCE,
                            0, 0,          // src x, y
                            0, 0,          // dst x, y
                            ATLAS_SIZE,    // width
                            ATLAS_SIZE     // height
                    );
            MemoryUtil.memFree(bitmap);

            atlasView = RenderSystem.getDevice().createTextureView(atlasTexture);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load font: " + fontPath, e);
        }
    }

    // Returns glyph quads for the given text at the given size
    // Each quad = x0,y0,x1,y1,u0,v0,u1,v1
    public float[][] getQuads(String text, float x, float y, float size) {
        float scale = size / BAKE_SIZE;
        float[] xpos = {x};
        float[] ypos = {y};

        float[][] quads = new float[text.length()][8];
        STBTTAlignedQuad quad = STBTTAlignedQuad.malloc();
        FloatBuffer xbuf = MemoryUtil.memAllocFloat(1);
        FloatBuffer ybuf = MemoryUtil.memAllocFloat(1);
        xbuf.put(0, x / scale);
        ybuf.put(0, (y + size) / scale);

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < FIRST_CHAR || c >= FIRST_CHAR + NUM_CHARS) continue;

            STBTruetype.stbtt_GetBakedQuad(
                    charData, ATLAS_SIZE, ATLAS_SIZE,
                    c - FIRST_CHAR,
                    xbuf, ybuf, quad, true
            );

            // Scale quad positions back up
            quads[i][0] = quad.x0() * scale;
            quads[i][1] = quad.y0() * scale;
            quads[i][2] = quad.x1() * scale;
            quads[i][3] = quad.y1() * scale;
            // UV coords are already normalized 0-1
            quads[i][4] = quad.s0();
            quads[i][5] = quad.t0();
            quads[i][6] = quad.s1();
            quads[i][7] = quad.t1();
        }

        MemoryUtil.memFree(xbuf);
        MemoryUtil.memFree(ybuf);
        quad.free();
        return quads;
    }

    public void close() {
        charData.free();
        atlasView.close();
        atlasTexture.close();
    }
}