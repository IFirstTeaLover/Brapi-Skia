package dev.bsprout.brapi.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryUtil;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class BFont {
    private static final int ATLAS_SIZE = 1024;
    private static final int FIRST_CHAR = 32;
    private static final int NUM_CHARS = 96;
    private static final int SDF_PIXEL_DIST = 8;
    private static final int GLYPH_SIZE = 64;

    public float ascent;

    private record GlyphData(short x0, short y0, short x1, short y1, float xoff, float yoff, float xadvance) {}
    public record FormattedQuads(float[][] quads, int[] colors) {}
    private final GlyphData[] charData = new GlyphData[NUM_CHARS];
    private final GpuTexture atlasTexture;
    final GpuTextureView atlasView;
    private final float bakeSize = GLYPH_SIZE;

    public BFont(Identifier fontPath) {
        try {
            InputStream stream = Minecraft.getInstance()
                    .getResourceManager().open(fontPath);
            byte[] ttfBytes = stream.readAllBytes();
            stream.close();

            ByteBuffer ttfBuf = MemoryUtil.memAlloc(ttfBytes.length);
            ttfBuf.put(ttfBytes).flip();

            STBTTFontinfo fontInfo = STBTTFontinfo.malloc();
            STBTruetype.stbtt_InitFont(fontInfo, ttfBuf, 0);
            float scale = STBTruetype.stbtt_ScaleForPixelHeight(fontInfo, GLYPH_SIZE);

            int[] ascent = new int[1], descent = new int[1], lineGap = new int[1];
            STBTruetype.stbtt_GetFontVMetrics(fontInfo, ascent, descent, lineGap);
            this.ascent = ascent[0] * scale;

            ByteBuffer bitmap = MemoryUtil.memAlloc(ATLAS_SIZE * ATLAS_SIZE);

            int penX = 0, penY = 0, rowH = 0;

            for (int i = 0; i < NUM_CHARS; i++) {
                int codepoint = FIRST_CHAR + i;
                int[] w = new int[1], h = new int[1], xoff = new int[1], yoff = new int[1];

                ByteBuffer sdf = STBTruetype.stbtt_GetCodepointSDF(
                        fontInfo, scale, codepoint,
                        SDF_PIXEL_DIST, (byte) 128,
                        (float) SDF_PIXEL_DIST / GLYPH_SIZE * 255f,
                        w, h, xoff, yoff
                );

                int[] advance = new int[1], lsb = new int[1];
                STBTruetype.stbtt_GetCodepointHMetrics(fontInfo, codepoint, advance, lsb);

                if (sdf == null || w[0] == 0 || h[0] == 0) {
                    charData[i] = new GlyphData((short)0, (short)0, (short)0, (short)0,
                            xoff[0], yoff[0], advance[0] * scale);
                    continue;
                }

                if (penX + w[0] > ATLAS_SIZE) {
                    penX = 0;
                    penY += rowH + 1;
                    rowH = 0;
                }

                for (int row = 0; row < h[0]; row++) {
                    int srcOff = row * w[0];
                    int dstOff = (penY + row) * ATLAS_SIZE + penX;
                    for (int col = 0; col < w[0]; col++) {
                        bitmap.put(dstOff + col, sdf.get(srcOff + col));
                    }
                }

                charData[i] = new GlyphData(
                        (short) penX, (short) penY,
                        (short)(penX + w[0]), (short)(penY + h[0]),
                        xoff[0], yoff[0],
                        advance[0] * scale
                );

                penX += w[0] + 1;
                rowH = Math.max(rowH, h[0]);
                STBTruetype.stbtt_FreeSDF(sdf);
            }

            fontInfo.free();
            MemoryUtil.memFree(ttfBuf);

            atlasTexture = RenderSystem.getDevice().createTexture(
                    "BFont SDF atlas",
                    GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST,
                    TextureFormat.RED8,
                    ATLAS_SIZE, ATLAS_SIZE, 1, 1
            );
            RenderSystem.getDevice().createCommandEncoder()
                    .writeToTexture(atlasTexture, bitmap,
                            NativeImage.Format.LUMINANCE,
                            0, 0, 0, 0, ATLAS_SIZE, ATLAS_SIZE);
            MemoryUtil.memFree(bitmap);

            atlasView = RenderSystem.getDevice().createTextureView(atlasTexture);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load font: " + fontPath, e);
        }
    }

    public float[][] getQuads(String text, float x, float y, float size) {
        float scale = size / bakeSize;
        float curX = x;
        float[][] quads = new float[text.length()][8];

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < FIRST_CHAR || c >= FIRST_CHAR + NUM_CHARS) continue;
            GlyphData bc = charData[c - FIRST_CHAR];
            if (bc == null) continue;

            float x0 = curX + bc.xoff() * scale;
            float y0 = y    + bc.yoff() * scale;
            float x1 = x0  + (bc.x1() - bc.x0()) * scale;
            float y1 = y0  + (bc.y1() - bc.y0()) * scale;

            quads[i][0] = x0; quads[i][1] = y0;
            quads[i][2] = x1; quads[i][3] = y1;
            quads[i][4] = (float) (bc.x0() + 1) / ATLAS_SIZE;
            quads[i][5] = (float) (bc.y0() + 1) / ATLAS_SIZE;
            quads[i][6] = (float) (bc.x1() - 1) / ATLAS_SIZE;
            quads[i][7] = (float) (bc.y1() - 1) / ATLAS_SIZE;

            curX += bc.xadvance() * scale;
        }
        return quads;
    }

    public void close() {
        atlasView.close();
        atlasTexture.close();
    }

    public float getLineHeight(float size) {
        return size;
    }

    public float getAscent(float size) {
        return ascent * (size / bakeSize);
    }

    public float textSize(String text, float size) {
        float scale = size / bakeSize;
        float width = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < FIRST_CHAR || c >= FIRST_CHAR + NUM_CHARS) continue;
            GlyphData bc = charData[c - FIRST_CHAR];
            if (bc == null) continue;
            width += bc.xadvance() * scale;
        }
        return width;
    }

    public FormattedQuads getQuadsFormatted(FormattedCharSequence text, float x, float y, float size, int defaultColor) {
        List<float[]> quadList = new ArrayList<>();
        List<Integer> colorList = new ArrayList<>();
        float[] curX = { x };

        text.accept((index, style, codePoint) -> {
            int color = style.getColor() != null
                    ? (0xFF000000 | style.getColor().getValue())
                    : defaultColor;
            char c = (char) codePoint;
            if (c >= FIRST_CHAR && c < FIRST_CHAR + NUM_CHARS) {
                GlyphData bc = charData[c - FIRST_CHAR];
                float scale = size / bakeSize;
                if (bc != null && !(bc.x1() == 0 && bc.y1() == 0)) {
                    float x0 = curX[0] + bc.xoff() * scale;
                    float y0 = y + bc.yoff() * scale;
                    float x1 = x0 + (bc.x1() - bc.x0()) * scale;
                    float y1 = y0 + (bc.y1() - bc.y0()) * scale;
                    float[] q = {
                            x0, y0, x1, y1,
                            (float)(bc.x0() + 1) / ATLAS_SIZE,
                            (float)(bc.y0() + 1) / ATLAS_SIZE,
                            (float)(bc.x1() - 1) / ATLAS_SIZE,
                            (float)(bc.y1() - 1) / ATLAS_SIZE
                    };
                    quadList.add(q);
                    colorList.add(color);
                }
                curX[0] += bc != null ? bc.xadvance() * scale : 0;
            }
            return true;
        });

        int[] colors = colorList.stream().mapToInt(Integer::intValue).toArray();
        return new FormattedQuads(quadList.toArray(new float[0][]), colors);
    }
}