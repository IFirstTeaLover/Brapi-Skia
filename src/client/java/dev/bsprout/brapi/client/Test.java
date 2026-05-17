package dev.bsprout.brapi.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class Test extends Screen {
    private final BRender bRender = new BRender();
    private int rectCount = 10;

    public Test() {
        super(Component.literal("BRender Test Screen"));
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.literal("10 rects"), btn -> rectCount = 10)
                .pos(10, 10).size(80, 20).build());
        addRenderableWidget(Button.builder(Component.literal("100 rects"), btn -> rectCount = 100)
                .pos(100, 10).size(80, 20).build());
        addRenderableWidget(Button.builder(Component.literal("1000 rects"), btn -> rectCount = 1000)
                .pos(190, 10).size(80, 20).build());
        addRenderableWidget(Button.builder(Component.literal("10000 rects"), btn -> rectCount = 10000)
                .pos(280, 10).size(80, 20).build());
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        int cols = 1920 / 13;
        int rows = 1080 / 13;
        int total = cols * rows;

        for (int i = 0; i < rectCount; i++) {
            int x = (i % cols) * 13;
            int y = 40 + (i / cols) * 13;
            int color = 0xFF000000 | (i * 7 % 256) << 16 | (i * 13 % 256) << 8 | (i * 19 % 256);
            bRender.roundRect(x, y, 12, 12, color, 3);
        }

        bRender.flush(graphics);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        System.out.println("Test Screen Closed!");
    }
}