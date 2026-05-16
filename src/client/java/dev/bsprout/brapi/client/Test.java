package dev.bsprout.brapi.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class Test extends Screen {
    private final BRender bRender = new BRender();

    public Test() {
        super(Component.literal("BRender Test Screen"));
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick); // first, render the actual minecraft gui

        bRender.roundRect(50, 50, 100, 30, 0xFFFF0000, 5); // render 100x30 rounded rectangle with 5 px rounding
        bRender.flush(graphics); // call after super
    }

    @Override
    public boolean isPauseScreen() {
        return false; // true = pause the game when the screen is open (singleplayer only)
    }

    /*
    Code ran when screen is opened
    You may want to add some buttons here...
     */
    @Override
    protected void init() {
        System.out.println("Test Screen Initialized!");
    }

    @Override
    public void removed() {
        System.out.println("Test Screen Closed!");
    }
}