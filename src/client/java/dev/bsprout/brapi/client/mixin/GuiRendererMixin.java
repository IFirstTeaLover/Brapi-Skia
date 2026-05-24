package dev.bsprout.brapi.client.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import dev.bsprout.brapi.client.BRender;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
public class GuiRendererMixin {
    // Inject after everything rendered and then render brapi
    @Inject(method = "render", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/render/GuiRenderer;draw(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",
            shift = At.Shift.AFTER
    ))
    private void afterDraw(GpuBufferSlice fogSlice, CallbackInfo ci) {
//        BRender.flushPending();
//        BRender.flushPendingText();
//        //BRender.flushPendingBlur();
//        BRender.flushPendingTextures();

        BRender.flushAll();
    }
}