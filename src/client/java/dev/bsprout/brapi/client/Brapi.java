package dev.bsprout.brapi.client;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import net.fabricmc.api.ClientModInitializer;

public class Brapi implements ClientModInitializer {

    // Here we register a custom render pipeline for rounded rects (and circles !!)
    public static final RenderPipeline ROUNDED_RECT_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("brapi", "pipeline/rounded_rect"))
                    .withVertexShader("core/brapi_rounded_rect")
                    .withFragmentShader("core/brapi_rounded_rect")
                    .withUniform("RectData", UniformType.UNIFORM_BUFFER)
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .build()
    );

	@Override
	public void onInitializeClient() {
        // Adding a simple test command to open our Test screen (dev/bsprout/brapi/client/Test.java)
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(ClientCommandManager.literal("brapitest")
                .executes(context -> {
                    Minecraft mc = Minecraft.getInstance();
                    mc.schedule(() -> {
                        if (mc.player != null) {
                            mc.player.closeContainer();
                        }
                        mc.setScreen(new Test());
                    });
                    return 1;
                })));
	}
}