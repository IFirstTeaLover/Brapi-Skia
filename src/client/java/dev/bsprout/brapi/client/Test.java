package dev.bsprout.brapi.client;

import dev.bsprout.brapi.client.BRender;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class Test extends Screen {
    private final BRender bRender = new BRender();

    public Test() {
        super(Component.literal("BRender Test Screen"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        bRender.roundRect(50, 50, 100, 30, 0xFF4444FF, 5, 5, 5, 5);
        bRender.roundRect(mouseX - 10, mouseY - 10, 20, 20, 0xFFFFFFFF, 4, 4, 4, 4);

        bRender.flush(graphics);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
    @Override
    protected void init() {
        System.out.println("Test Screen Initialized!");
    }

    @Override
    public void removed() {
        System.out.println("Test Screen Closed!");
    }
}