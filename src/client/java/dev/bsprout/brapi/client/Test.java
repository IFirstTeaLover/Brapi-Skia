package dev.bsprout.brapi.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public class Test extends Screen {
    private final BRender bRender = new BRender();
    private BFont font;
    private BTexture texture;
    private NineSlice nineSlice;

    public Test() {
        super(Component.literal("BRender Test Screen"));
    }

    @Override
    protected void init() {
        font = new BFont(Identifier.fromNamespaceAndPath("brapi", "fonts/noto_sans_regular.ttf"));
        texture = new BTexture(Identifier.fromNamespaceAndPath("brapi", "textures/mc_button.png"));
        nineSlice = BRender.nineslicify(texture, 4, 4, 4, 4);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        // --- Filled shapes ---

        // Plain rect
        bRender.rect(10, 10, 80, 30, 0xFFFF0000);

        // Rounded rect - uniform radius
        bRender.roundRect(100, 10, 80, 30, 0xFF00FF00, 8);

        // Rounded rect - two radii (top-left/bottom-right, top-right/bottom-left)
        bRender.roundRect(190, 10, 80, 30, 0xFF0000FF, 16, 4);

        // Rounded rect - four individual radii
        bRender.roundRect(280, 10, 80, 30, 0xFFFFFF00, 0, 8, 16, 4);

        // Circle
        bRender.circle(380, 10, 30, 0xFFFF00FF);

        // Semi-transparent rect
        bRender.roundRect(420, 10, 80, 30, 0x80FF8800, 6);

        // --- Strokes ---

        // Plain stroke
        bRender.stroke(10, 60, 80, 30, 0xFFFFFFFF, 2);

        // Rounded stroke - uniform radius
        bRender.strokeRounded(100, 60, 80, 30, 0xFFFFFFFF, 8, 2);

        // Rounded stroke - two radii
        bRender.strokeRounded(190, 60, 80, 30, 0xFFFFFFFF, 16, 4, 2);

        // Rounded stroke - four radii
        bRender.strokeRounded(280, 60, 80, 30, 0xFFFFFFFF, 0, 8, 16, 4, 2);

        // --- Filled + stroke ---

        // roundRectStroked - uniform radius
        bRender.roundRectStroked(10, 110, 80, 30, 0xFF1144AA, 0xFFFFFFFF, 8, 2);

        // roundRectStroked - two radii
        bRender.roundRectStroked(100, 110, 80, 30, 0xFF228844, 0xFFFFFF00, 16, 4, 2);

        // roundRectStroked - four radii
        bRender.roundRectStroked(190, 110, 80, 30, 0xFF884422, 0xFF00FFFF, 0, 8, 16, 4, 2);

        // --- Text ---

        // Small text
        bRender.drawText(font, "Text at 10px", 10, 160, 10, 0xFFFFFFFF);

        // Medium text
        bRender.drawText(font, "Text at 20px", 10, 180, 20, 0xFFFFFF00);

        // Large text
        bRender.drawText(font, "Text at 40px", 10, 210, 40, 0xFFFF8800);

        // Colored text
        bRender.drawText(font, "Red 16px", 10, 265, 16, 0xFFFF0000);
        bRender.drawText(font, "Green 16px", 120, 265, 16, 0xFF00FF00);
        bRender.drawText(font, "Blue 16px", 240, 265, 16, 0xFF0088FF);

        // --- Textures ---

        // Stretched
        bRender.drawTexture(texture, 10, 290, 80, 40, 0xFFFFFFFF, false);

        // Cropped - top-left 8x8 of texture
        bRender.drawTextureCropped(texture, 100, 290, 40, 40, 0, 0, 8, 8, 0xFFFFFFFF, false);

        // Tiled
        bRender.drawTextureTiled(texture, 150, 290, 80, 40, 0xFFFFFFFF, false);

        // Tinted texture
        bRender.drawTexture(texture, 240, 290, 80, 40, 0xFFFF8800, false);

        // 9-sliced small
        bRender.drawTexture9Slice(nineSlice, 10, 350, 100, 50, 0xFFFFFFFF, false);

        // 9-sliced large
        bRender.drawTexture9Slice(nineSlice, 120, 350, 250, 80, 0xFFFFFFFF, false);

        // 9-sliced tinted
        bRender.drawTexture9Slice(nineSlice, 380, 350, 120, 80, 0xFF88CCFF, false);

        bRender.flush(graphics);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        if (font != null) font.close();
        if (texture != null) texture.close();
    }
}