package dev.bsprout.brapi.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryUtil;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class BTexture {
    public final GpuTexture texture;
    public final GpuTextureView view;
    public final int width;
    public final int height;

    public BTexture(Identifier location) {
        try {
            InputStream stream = Minecraft.getInstance()
                    .getResourceManager()
                    .open(location);
            byte[] bytes = stream.readAllBytes();
            stream.close();

            ByteBuffer rawBuf = MemoryUtil.memAlloc(bytes.length);
            rawBuf.put(bytes).flip();

            IntBuffer w = MemoryUtil.memAllocInt(1);
            IntBuffer h = MemoryUtil.memAllocInt(1);
            IntBuffer channels = MemoryUtil.memAllocInt(1);

            ByteBuffer pixels = STBImage.stbi_load_from_memory(rawBuf, w, h, channels, 4);
            if (pixels == null) throw new RuntimeException("Failed to load image: " + STBImage.stbi_failure_reason());

            width = w.get(0);
            height = h.get(0);
            MemoryUtil.memFree(rawBuf);
            MemoryUtil.memFree(w);
            MemoryUtil.memFree(h);
            MemoryUtil.memFree(channels);
            System.out.println("Loaded texture: " + location + " size: " + width + "x" + height);
            texture = RenderSystem.getDevice().createTexture(
                    location.toString(),
                    GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST,
                    TextureFormat.RGBA8,
                    width, height, 1, 1
            );

            RenderSystem.getDevice().createCommandEncoder()
                    .writeToTexture(texture, pixels, NativeImage.Format.RGBA,
                            0, 0, 0, 0, width, height);

            STBImage.stbi_image_free(pixels);

            view = RenderSystem.getDevice().createTextureView(texture);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load texture: " + location, e);
        }
    }

    public void close() {
        view.close();
        texture.close();
    }
}