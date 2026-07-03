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

    private float followerX = 0, followerY = 0;
    private float targetX = 0, targetY = 0;
    private int scrollY = 0;
    private static final int SCROLL_STEP = 150;
    private static final int BUTTON_H = 30;
    private static final int BUTTON_W = 80;

    public Test() {
        super(Component.literal("BRender Test Screen"));
    }

    @Override
    protected void init() {
        font = new BFont(Identifier.fromNamespaceAndPath("brapi", "fonts/noto_sans_regular.ttf"));
        texture = new BTexture(Identifier.fromNamespaceAndPath("brapi", "textures/mc_button.png"));
        nineSlice = BUtils.nineslicify(texture, 4, 4, 4, 4);
        followerX = this.width / 2f;
        followerY = this.height / 2f;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        targetX = (float) mouseX;
        targetY = (float) mouseY;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmount, double vAmount) {
        scrollY = Math.max(0, scrollY - (int)(vAmount * 30));
        return true;
    }


    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        float speed = 0.15f;
        followerX += (targetX - followerX) * speed;
        followerY += (targetY - followerY) * speed;

        // All content is offset by -scrollY
        int s = -scrollY;

        // ==================== FILLED SHAPES ====================
        int rowY = 10 + s;
        bRender.drawText(font, "--- Filled Rects ---", 10, rowY, 12, 0xFFAAAAAA, 1);
        rowY += 18;

        bRender.rect(10, rowY, 80, 30, 0xFFFF0000, 1);
        bRender.rect(100, rowY, 80, 30, new Gradient(0xFFFF0000, 0xFF0000FF), GradientDirection.LEFT_RIGHT, 1);
        bRender.rect(190, rowY, 80, 30, new Gradient(0xFF00FF00, 0xFF000000), GradientDirection.TOP_BOTTOM, 1);
        bRender.rect(280, rowY, 80, 30, new Gradient(0xFFFFFF00, 0xFF00FFFF, 0xFFFF00FF), GradientDirection.TOP_LEFT_BOTTOM_RIGHT, 1);
        rowY += 50;

        // ==================== ROUNDED RECTS ====================
        bRender.drawText(font, "--- Rounded Rects ---", 10, rowY, 12, 0xFFAAAAAA, 1);
        rowY += 18;

        bRender.roundRect(10, rowY, 80, 30, 0xFF00FF00, 8, 1);
        bRender.roundRect(100, rowY, 80, 30, new Gradient(0xFF00FF00, 0xFF0000FF), GradientDirection.LEFT_RIGHT, 8, 1);
        bRender.roundRect(190, rowY, 80, 30, 0xFF0000FF, 16, 4, 1);
        bRender.roundRect(280, rowY, 80, 30, new Gradient(0xFFFF8800, 0xFF8800FF), GradientDirection.TOP_BOTTOM, 16, 4, 1);
        bRender.roundRect(370, rowY, 80, 30, 0xFFFFFF00, 0, 8, 16, 4, 1);
        bRender.roundRect(460, rowY, 80, 30, new Gradient(0xFFFF0088, 0xFF00FFFF), GradientDirection.TOP_RIGHT_BOTTOM_LEFT, 0, 8, 16, 4, 1);
        rowY += 50;

        // ==================== CIRCLES ====================
        bRender.drawText(font, "--- Circles ---", 10, rowY, 12, 0xFFAAAAAA, 1);
        rowY += 18;

        bRender.circle(30, rowY + 20, 20, 0xFFFF00FF, 1);
        bRender.circle(90, rowY + 20, 20, new Gradient(0xFFFF0000, 0xFF0000FF), GradientDirection.TOP_BOTTOM, 1);
        bRender.circle(150, rowY + 20, 20, new Gradient(0xFF00FF00, 0xFFFF8800, 0xFF0000FF), GradientDirection.LEFT_RIGHT, 1);
        rowY += 60;

        // ==================== STROKES ====================
        bRender.drawText(font, "--- Strokes ---", 10, rowY, 12, 0xFFAAAAAA, 1);
        rowY += 18;

        bRender.stroke(10, rowY, 80, 30, 0xFFFFFFFF, 2, 1);
        bRender.stroke(100, rowY, 80, 30, new Gradient(0xFFFF0000, 0xFF00FFFF), GradientDirection.LEFT_RIGHT, 2, 1);
        bRender.strokeRounded(190, rowY, 80, 30, 0xFFFFFFFF, 8, 2, 1);
        bRender.strokeRounded(280, rowY, 80, 30, new Gradient(0xFF00FF00, 0xFFFF8800), GradientDirection.TOP_BOTTOM, 8, 2, 1);
        bRender.strokeRounded(370, rowY, 80, 30, 0xFFFFFFFF, 16, 4, 2, 1);
        bRender.strokeRounded(460, rowY, 80, 30, new Gradient(0xFFFF0088, 0xFF8800FF), GradientDirection.LEFT_RIGHT, 0, 8, 16, 4, 2, 1);
        rowY += 50;

        // ==================== FILLED + STROKE ====================
        bRender.drawText(font, "--- Filled + Stroke ---", 10, rowY, 12, 0xFFAAAAAA, 1);
        rowY += 18;

        bRender.roundRectStroked(10, rowY, 80, 30, 0xFF1144AA, 0xFFFFFFFF, 8, 2, 1);
        bRender.roundRectStroked(100, rowY, 80, 30, 0xFF228844, 0xFFFFFF00, 16, 4, 2, 1);
        bRender.roundRectStroked(190, rowY, 80, 30, 0xFF884422, 0xFF00FFFF, 0, 8, 16, 4, 2, 1);
        bRender.roundRectStroked(280, rowY, 80, 30, new Gradient(0xFF0044FF, 0xFF00FFAA), GradientDirection.LEFT_RIGHT, 0xFFFFFFFF, 8, 2, 1);
        rowY += 50;

        // ==================== TEXT ====================
        bRender.drawText(font, "--- Text ---", 10, rowY, 12, 0xFFAAAAAA, 1);
        rowY += 18;

        bRender.drawText(font, "BFont 10px", 10, rowY, 10, 0xFFFFFFFF, 1);
        bRender.drawText(font, "BFont 20px", 120, rowY, 20, 0xFFFFFF00, 1);
        bRender.drawText(font, "BFont 32px", 280, rowY, 32, 0xFFFF8800, 1);
        rowY += 45;

        bRender.drawText(font, "Red", 10, rowY, 16, 0xFFFF0000, 1);
        bRender.drawText(font, "Green", 80, rowY, 16, 0xFF00FF00, 1);
        bRender.drawText(font, "Blue", 170, rowY, 16, 0xFF0088FF, 1);
        bRender.drawTextShadow(font, "Shadowed", 250, rowY, 16, 0xFFFFFFFF, 1);
        rowY += 25;

        bRender.drawText("MC font", 10, rowY, 0xFFFFFFFF, 1);
        bRender.drawTextShadow("MC shadow", 120, rowY, 0xFFFFFF00, 1);
        bRender.drawTextCentered("MC centered", this.width / 2f, rowY, 0xFF88FFFF, 1);
        rowY += 35;

        // ==================== TEXTURES ====================
        bRender.drawText(font, "--- Textures ---", 10, rowY, 12, 0xFFAAAAAA, 1);
        rowY += 18;

        bRender.drawTexture(texture, 10, rowY, 80, 40, 0xFFFFFFFF, false, 1);
        bRender.drawTextureCropped(texture, 100, rowY, 40, 40, 0, 0, 8, 8, 0xFFFFFFFF, false, 1);
        bRender.drawTextureTiled(texture, 150, rowY, 80, 40, 0xFFFFFFFF, false, 1);
        bRender.drawTexture(texture, 240, rowY, 80, 40, 0xFFFF8800, false, 1);
        rowY += 55;

        // ==================== 9-SLICE ====================
        bRender.drawText(font, "--- 9-Slice ---", 10, rowY, 12, 0xFFAAAAAA, 1);
        rowY += 18;

        bRender.drawTexture9Slice(nineSlice, 10, rowY, 100, 50, 0xFFFFFFFF, false, 1);
        bRender.drawTexture9Slice(nineSlice, 120, rowY, 250, 80, 0xFFFFFFFF, false, 1);
        bRender.drawTexture9Slice(nineSlice, 380, rowY, 120, 80, 0xFF88CCFF, false, 1);

        // ==================== MOUSE FOLLOWER (fixed, not scrolled) ====================
        int fw = 60, fh = 60;
        bRender.roundRect(
                (int)(followerX - fw / 2f), (int)(followerY - fh / 2f), fw, fh,
                new Gradient(0xFFFF0066, 0xFF6600FF, 0xFF00CCFF),
                GradientDirection.TOP_LEFT_BOTTOM_RIGHT, 12, 10
        );
        bRender.strokeRounded(
                (int)(followerX - fw / 2f) - 2, (int)(followerY - fh / 2f) - 2,
                fw + 4, fh + 4,
                new Gradient(0xAAFF0066, 0xAA00CCFF),
                GradientDirection.TOP_LEFT_BOTTOM_RIGHT, 14, 2, 10
        );

        // ==================== SCROLL BUTTONS (fixed position) ====================
        int upBtnX = this.width - BUTTON_W - 10;
        int downBtnX = this.width - BUTTON_W - 10;
        int upBtnY = this.height - BUTTON_H * 2 - 15;
        int downBtnY = this.height - BUTTON_H - 10;

        bRender.roundRect(upBtnX, upBtnY, BUTTON_W, BUTTON_H,
                new Gradient(0xFF444444, 0xFF222222), GradientDirection.TOP_BOTTOM, 6, 10);
        bRender.drawText(font, "^ Up", upBtnX + 22, upBtnY + 8, 14, 0xFFFFFFFF, 11);

        bRender.roundRect(downBtnX, downBtnY, BUTTON_W, BUTTON_H,
                new Gradient(0xFF444444, 0xFF222222), GradientDirection.TOP_BOTTOM, 6, 10);
        bRender.drawText(font, "v Down", downBtnX + 14, downBtnY + 8, 14, 0xFFFFFFFF, 11);

        bRender.flush(graphics);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void removed() {
        if (font != null) font.close();
        if (texture != null) texture.close();
    }
}