package dev.bsprout.brapi.client;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.minecraft.client.renderer.ShaderInstance;

public class Brapi implements ClientModInitializer {
    public static ShaderInstance roundedRectShader;

	@Override
	public void onInitializeClient() {
        CoreShaderRegistrationCallback.EVENT.register(context -> {
            context.register(ResourceLocation.fromNamespaceAndPath("brapi", "rounded_rect"), DefaultVertexFormat.POSITION_TEX, program -> {
                roundedRectShader = program;
                System.out.println("Brapi Shader Loaded Successfully!");
            });
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("brapitest")
                    .executes(context -> {
                        Minecraft mc = Minecraft.getInstance();
                        // Using tell() ensures it runs at the start of the next frame
                        mc.tell(() -> {
                            if (mc.player != null) {
                                mc.player.closeContainer(); // Close any open chat/containers first
                            }
                            mc.setScreen(new Test());
                        });
                        return 1;
                    }));
        });
	}
}