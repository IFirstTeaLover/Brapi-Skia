package dev.bsprout.brapi.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class Test extends Screen {

    // ==================== RENDER STATE ====================
    private final BRender bRender = new BRender();
    private BFont font;
    private BTexture texture;
    private NineSlice nineSlice;

    private long startTimeNanos;
    private float time;

    // ==================== CURSOR ORB ====================
    private float orbX, orbY, orbTargetX, orbTargetY;
    private static final int TRAIL_LENGTH = 8;
    private final float[] trailX = new float[TRAIL_LENGTH];
    private final float[] trailY = new float[TRAIL_LENGTH];

    // ==================== SCROLL ====================
    private float scrollY = 0f;
    private float scrollTarget = 0f;
    private static final float SCROLL_STEP = 60f;
    private int contentHeight = 900; // recalculated every frame from actual layout

    // ==================== LAYOUT ====================
    private static final int SIDEBAR_W = 132;
    private static final int HEADER_H = 118;
    private static final int CONTENT_TOP = HEADER_H + 16;

    private static final String[] SECTIONS = {"Shapes", "Combo", "Typography", "Textures", "Widgets"};
    private final int[] sectionAnchor = new int[SECTIONS.length];
    private int hoveredNav = -1;
    private int hoveredScrollBtn = -1;

    // ==================== PARTICLES ====================
    private static final int PARTICLE_COUNT = 16;
    private final Random rng = new Random(1337);
    private final float[] pX = new float[PARTICLE_COUNT];
    private final float[] pSpeed = new float[PARTICLE_COUNT];
    private final float[] pSize = new float[PARTICLE_COUNT];
    private final float[] pPhase = new float[PARTICLE_COUNT];

    // ==================== PALETTE ====================
    private static final int ACCENT       = 0xFFFF8A00;
    private static final int ACCENT_LIGHT = 0xFFFFC266;
    private static final int ACCENT_DEEP  = 0xFFCC5200;
    private static final int ACCENT_FAINT = 0x33FF8A00;
    private static final int ACCENT_GHOST = 0x14FF8A00;
    private static final int BG_TOP       = 0xFF17110C;
    private static final int BG_BOTTOM    = 0xFF0A0705;
    private static final int PANEL_BG     = 0xE01C1512;
    private static final int PANEL_BG_HOT = 0xE0271A10;
    private static final int PANEL_BORDER = 0x40FF8A00;
    private static final int TEXT_PRIMARY = 0xFFF5EBE0;
    private static final int TEXT_MUTED   = 0xFFA89685;

    private static final int LAYER_BG = 0, LAYER_PARTICLE = 1, LAYER_PANEL = 2,
            LAYER_CONTENT = 3, LAYER_CONTENT_FG = 4, LAYER_SIDEBAR = 6,
            LAYER_SIDEBAR_FG = 7, LAYER_SCROLLBAR = 8, LAYER_ORB = 10, LAYER_UI = 11, LAYER_UI_FG = 12;

    public Test() {
        super(Component.literal("BRender Test Screen"));
    }

    @Override
    protected void init() {
        font = new BFont(Identifier.fromNamespaceAndPath("brapi", "fonts/noto_sans_regular.ttf"));
        texture = new BTexture(Identifier.fromNamespaceAndPath("brapi", "textures/mc_button.png"));
        nineSlice = BUtils.nineslicify(texture, 4, 4, 4, 4);

        startTimeNanos = System.nanoTime();
        orbX = orbTargetX = this.width / 2f;
        orbY = orbTargetY = this.height / 2f;
        for (int i = 0; i < TRAIL_LENGTH; i++) {
            trailX[i] = orbX;
            trailY[i] = orbY;
        }

        for (int i = 0; i < PARTICLE_COUNT; i++) {
            pX[i] = rng.nextFloat();
            pSpeed[i] = 8f + rng.nextFloat() * 18f;
            pSize[i] = 1.5f + rng.nextFloat() * 3.5f;
            pPhase[i] = rng.nextFloat() * (float) (Math.PI * 2);
        }
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        orbTargetX = (float) mouseX;
        orbTargetY = (float) mouseY;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmount, double vAmount) {
        scrollTarget = Math.max(0, Math.min(contentHeight - 200, scrollTarget - (float) (vAmount * SCROLL_STEP)));
        return true;
    }

    private boolean inside(double mx, double my, int[] r) {
        return mx >= r[0] && mx <= r[0] + r[2] && my >= r[1] && my <= r[1] + r[3];
    }

    private int[] navItemBounds(int index) {
        return new int[]{14, HEADER_H + 6 + index * 40, SIDEBAR_W - 24, 32};
    }

    private int[] scrollButtonBounds(boolean up) {
        int size = 30;
        int x = this.width - size - 14;
        int y = up ? this.height - size * 2 - 22 : this.height - size - 14;
        return new int[]{x, y, size, size};
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        time = (System.nanoTime() - startTimeNanos) / 1_000_000_000f;

        // Chase the cursor / scroll target — real-elapsed-time based, not tied to a fixed frame delta.
        orbX += (orbTargetX - orbX) * 0.2f;
        orbY += (orbTargetY - orbY) * 0.2f;
        scrollY += (scrollTarget - scrollY) * 0.18f;

        for (int i = TRAIL_LENGTH - 1; i > 0; i--) {
            trailX[i] = trailX[i - 1];
            trailY[i] = trailY[i - 1];
        }
        trailX[0] = orbX;
        trailY[0] = orbY;

        drawBackground();
        drawParticles();

        int contentX = SIDEBAR_W + 24;
        int logicalY = 0;

        logicalY = drawShapesSection(contentX, logicalY, 0);
        logicalY += 20;
        logicalY = drawComboSection(contentX, logicalY, 1);
        logicalY += 20;
        logicalY = drawTypographySection(contentX, logicalY, 2);
        logicalY += 20;
        logicalY = drawTexturesSection(contentX, logicalY, 3);
        logicalY += 20;
        logicalY = drawWidgetsSection(contentX, logicalY, 4);

        contentHeight = logicalY;

        drawHeader();
        drawSidebar(mouseX, mouseY);
        drawScrollbar();
        drawScrollButtons(mouseX, mouseY);
        drawCursorOrb();

        bRender.flush(graphics);
    }

    // ==================== BACKGROUND ====================

    private void drawBackground() {
        bRender.rect(0, 0, this.width, this.height, new Gradient(BG_TOP, BG_BOTTOM), GradientDirection.TOP_BOTTOM, LAYER_BG);
    }

    private void drawParticles() {
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            float x = pX[i] * this.width;
            float travelled = (time * pSpeed[i]) % (this.height + 40);
            float y = this.height - travelled;
            float size = pSize[i] + (float) Math.sin(time * 1.4f + pPhase[i]) * 0.6f;
            int alpha = (int) (26 + 18 * Math.sin(time + pPhase[i]));
            int color = (alpha << 24) | 0x00FF8A00;
            bRender.circle((int) x, (int) y, Math.max(1, (int) size), color, LAYER_PARTICLE);
        }
    }

    private void drawHeader() {
        int cx = SIDEBAR_W + 90, cy = 46;
        bRender.circle(cx, cy, 130, ACCENT_GHOST, LAYER_BG);
        bRender.circle(cx, cy, 85, 0x20FF8A00, LAYER_BG);
        bRender.circle(cx, cy, 46, 0x2AFF8A00, LAYER_BG);

        bRender.drawTextShadow(font, "BRapi", SIDEBAR_W + 24, 14, 40, ACCENT_LIGHT, LAYER_UI_FG);
        bRender.drawText(font, "Rendering pipeline showcase", SIDEBAR_W + 26, 58, 13, TEXT_MUTED, LAYER_UI_FG);

        float progress = easeOutCubic(Math.min(1f, time / 1.1f));
        int maxW = 220;
        int w = Math.max(4, (int) (maxW * progress));
        bRender.roundRect(SIDEBAR_W + 26, 82, w, 4, new Gradient(ACCENT, ACCENT_DEEP), GradientDirection.LEFT_RIGHT, 2, LAYER_UI_FG);

        bRender.strokeRounded(SIDEBAR_W, HEADER_H - 4, this.width - SIDEBAR_W - 14, 1, ACCENT_FAINT, 0, 1, LAYER_UI);
    }

    private float easeOutCubic(float t) {
        float f = t - 1;
        return f * f * f + 1;
    }

    // ==================== SIDEBAR ====================

    private void drawSidebar(int mouseX, int mouseY) {
        bRender.roundRect(6, HEADER_H, SIDEBAR_W - 12, this.height - HEADER_H - 12, PANEL_BG, 10, LAYER_SIDEBAR);
        bRender.strokeRounded(6, HEADER_H, SIDEBAR_W - 12, this.height - HEADER_H - 12, PANEL_BORDER, 10, 1, LAYER_SIDEBAR);

        hoveredNav = -1;
        for (int i = 0; i < SECTIONS.length; i++) {
            int[] r = navItemBounds(i);
            boolean hot = inside(mouseX, mouseY, r);
            boolean active = scrollY >= sectionAnchor[i] - 40 &&
                    (i == SECTIONS.length - 1 || scrollY < sectionAnchor[i + 1] - 40);
            if (hot) hoveredNav = i;

            if (active) {
                bRender.roundRect(r[0], r[1], r[2], r[3], new Gradient(ACCENT_FAINT, 0x00FF8A00), GradientDirection.LEFT_RIGHT, 6, LAYER_SIDEBAR_FG);
                bRender.rect(r[0], r[1] + 4, 3, r[3] - 8, ACCENT, LAYER_SIDEBAR_FG);
            } else if (hot) {
                bRender.roundRect(r[0], r[1], r[2], r[3], 0x1AFFFFFF, 6, LAYER_SIDEBAR_FG);
            }

            int textColor = active ? ACCENT_LIGHT : (hot ? TEXT_PRIMARY : TEXT_MUTED);
            bRender.drawText(font, SECTIONS[i], r[0] + 14, r[1] + 10, 13, textColor, LAYER_SIDEBAR_FG + 1);
        }
    }

    // ==================== SCROLLBAR ====================

    private void drawScrollbar() {
        int trackX = this.width - 8, trackY = HEADER_H, trackH = this.height - HEADER_H - 60;
        bRender.roundRect(trackX, trackY, 3, trackH, 0x22FFFFFF, 2, LAYER_SCROLLBAR);

        int visible = this.height - HEADER_H - 60;
        float ratio = Math.min(1f, (float) visible / Math.max(1, contentHeight));
        int thumbH = Math.max(24, (int) (trackH * ratio));
        float scrollRatio = contentHeight <= visible ? 0 : scrollY / (float) (contentHeight - visible);
        int thumbY = trackY + (int) ((trackH - thumbH) * Math.min(1f, Math.max(0f, scrollRatio)));

        bRender.roundRect(trackX, thumbY, 3, thumbH, new Gradient(ACCENT, ACCENT_DEEP), GradientDirection.TOP_BOTTOM, 2, LAYER_SCROLLBAR + 1);
    }

    // ==================== SCROLL BUTTONS ====================

    private void drawScrollButtons(int mouseX, int mouseY) {
        int[] up = scrollButtonBounds(true);
        int[] down = scrollButtonBounds(false);
        hoveredScrollBtn = inside(mouseX, mouseY, up) ? 0 : inside(mouseX, mouseY, down) ? 1 : -1;

        drawRoundButton(up, "^", hoveredScrollBtn == 0);
        drawRoundButton(down, "v", hoveredScrollBtn == 1);
    }

    private void drawRoundButton(int[] r, String label, boolean hot) {
        int radius = r[2] / 2;
        if (hot) {
            bRender.roundRect(r[0], r[1], r[2], r[3], new Gradient(ACCENT, ACCENT_DEEP), GradientDirection.TOP_BOTTOM, radius, LAYER_UI);
        } else {
            bRender.roundRect(r[0], r[1], r[2], r[3], PANEL_BG_HOT, radius, LAYER_UI);
            bRender.strokeRounded(r[0], r[1], r[2], r[3], PANEL_BORDER, radius, 1, LAYER_UI);
        }
        int textColor = hot ? 0xFF1A1008 : ACCENT_LIGHT;
        bRender.drawText(font, label, r[0] + r[2] / 2f, r[1] + r[3] / 2f - 6, 10, textColor,  LAYER_UI_FG);
    }

    // ==================== CURSOR ORB ====================

    private void drawCursorOrb() {
        for (int i = TRAIL_LENGTH - 1; i >= 0; i--) {
            float f = 1f - (i / (float) TRAIL_LENGTH);
            int radius = (int) (4 + f * 10);
            int alpha = (int) (10 + f * 50);
            int color = (alpha << 24) | 0x00FF8A00;
            bRender.circle((int) trailX[i], (int) trailY[i], radius, color, LAYER_ORB);
        }

        for (int i = 0; i < 3; i++) {
            double angle = time * 2.2 + i * (Math.PI * 2 / 3);
            int ox = (int) (orbX + Math.cos(angle) * 20);
            int oy = (int) (orbY + Math.sin(angle) * 20);
            bRender.circle(ox, oy, 3, ACCENT_LIGHT, LAYER_ORB + 1);
        }

        bRender.circle((int) orbX, (int) orbY, 9, new Gradient(ACCENT_LIGHT, ACCENT_DEEP), GradientDirection.TOP_LEFT_BOTTOM_RIGHT, LAYER_ORB + 2);
        bRender.strokeRounded((int) orbX - 12, (int) orbY - 12, 24, 24, 0x88FF8A00, 12, 1, LAYER_ORB + 2);
    }

    // ==================== CONTENT SECTIONS ====================

    private int panelY(int logicalY) {
        return CONTENT_TOP + logicalY - (int) scrollY;
    }

    private void panelFrame(int x, int y, int w, int h, String title) {
        bRender.roundRect(x, y, w, h, PANEL_BG, 10, LAYER_PANEL);
        bRender.strokeRounded(x, y, w, h, PANEL_BORDER, 10, 1, LAYER_PANEL);
        bRender.drawText(font, title, x + 16, y + 12, 13, ACCENT_LIGHT, LAYER_CONTENT);
        bRender.rect(x + 16, y + 32, 32, 2, ACCENT, LAYER_CONTENT);
    }

    private int drawShapesSection(int x, int logicalY, int sectionIndex) {
        sectionAnchor[sectionIndex] = logicalY;
        int panelH = 250;
        int y = panelY(logicalY);
        panelFrame(x, y, this.width - x - 20, panelH, "Shapes & Gradients");

        int rowY = y + 46;
        bRender.rect(x + 16, rowY, 80, 30, ACCENT, LAYER_CONTENT_FG);
        bRender.rect(x + 106, rowY, 80, 30, new Gradient(ACCENT, ACCENT_DEEP), GradientDirection.LEFT_RIGHT, LAYER_CONTENT_FG);
        bRender.rect(x + 196, rowY, 80, 30, new Gradient(ACCENT_LIGHT, 0xFF442200), GradientDirection.TOP_BOTTOM, LAYER_CONTENT_FG);
        bRender.rect(x + 286, rowY, 80, 30, new Gradient(ACCENT_LIGHT, ACCENT, ACCENT_DEEP), GradientDirection.TOP_LEFT_BOTTOM_RIGHT, LAYER_CONTENT_FG);
        rowY += 46;

        bRender.roundRect(x + 16, rowY, 80, 30, ACCENT, 8, LAYER_CONTENT_FG);
        bRender.roundRect(x + 106, rowY, 80, 30, new Gradient(ACCENT_LIGHT, ACCENT_DEEP), GradientDirection.LEFT_RIGHT, 8, LAYER_CONTENT_FG);
        bRender.roundRect(x + 196, rowY, 80, 30, ACCENT_DEEP, 16, 4, LAYER_CONTENT_FG);
        bRender.roundRect(x + 286, rowY, 80, 30, new Gradient(ACCENT, ACCENT_LIGHT), GradientDirection.TOP_BOTTOM, 16, 4, LAYER_CONTENT_FG);
        bRender.roundRect(x + 376, rowY, 80, 30, ACCENT_LIGHT, 0, 8, 16, 4, LAYER_CONTENT_FG);
        rowY += 60;

        float pulse = 4 + (float) Math.sin(time * 2.4f) * 3;
        bRender.circle(x + 46, rowY + 20, (int) (20 + pulse), ACCENT_GHOST, LAYER_CONTENT);
        bRender.circle(x + 46, rowY + 20, 20, ACCENT, LAYER_CONTENT_FG);
        bRender.circle(x + 116, rowY + 20, 20, new Gradient(ACCENT_LIGHT, ACCENT_DEEP), GradientDirection.TOP_BOTTOM, LAYER_CONTENT_FG);
        bRender.circle(x + 186, rowY + 20, 20, new Gradient(ACCENT, ACCENT_LIGHT, ACCENT_DEEP), GradientDirection.LEFT_RIGHT, LAYER_CONTENT_FG);
        rowY += 60;

        bRender.stroke(x + 16, rowY, 80, 30, ACCENT_LIGHT, 2, LAYER_CONTENT_FG);
        bRender.stroke(x + 106, rowY, 80, 30, new Gradient(ACCENT, ACCENT_LIGHT), GradientDirection.LEFT_RIGHT, 2, LAYER_CONTENT_FG);
        bRender.strokeRounded(x + 196, rowY, 80, 30, ACCENT_LIGHT, 8, 2, LAYER_CONTENT_FG);
        bRender.strokeRounded(x + 286, rowY, 80, 30, new Gradient(ACCENT, ACCENT_DEEP), GradientDirection.TOP_BOTTOM, 8, 2, LAYER_CONTENT_FG);
        bRender.strokeRounded(x + 376, rowY, 80, 30, ACCENT_LIGHT, 16, 4, 2, LAYER_CONTENT_FG);

        return logicalY + panelH;
    }

    private int drawComboSection(int x, int logicalY, int sectionIndex) {
        sectionAnchor[sectionIndex] = logicalY;
        int panelH = 90;
        int y = panelY(logicalY);
        panelFrame(x, y, this.width - x - 20, panelH, "Filled + Stroke");

        int rowY = y + 46;
        bRender.roundRectStroked(x + 16, rowY, 80, 30, ACCENT_DEEP, ACCENT_LIGHT, 8, 2, LAYER_CONTENT_FG);
        bRender.roundRectStroked(x + 106, rowY, 80, 30, 0xFF3A2A18, ACCENT, 16, 4, 2, LAYER_CONTENT_FG);
        bRender.roundRectStroked(x + 196, rowY, 80, 30, 0xFF241A10, ACCENT_LIGHT, 0, 8, 16, 4, 2, LAYER_CONTENT_FG);
        bRender.roundRectStroked(x + 286, rowY, 80, 30, new Gradient(ACCENT_DEEP, ACCENT), GradientDirection.LEFT_RIGHT, ACCENT_LIGHT, 8, 2, LAYER_CONTENT_FG);

        return logicalY + panelH;
    }

    private int drawTypographySection(int x, int logicalY, int sectionIndex) {
        sectionAnchor[sectionIndex] = logicalY;
        int panelH = 140;
        int y = panelY(logicalY);
        panelFrame(x, y, this.width - x - 20, panelH, "Typography");

        int rowY = y + 48;
        bRender.drawText(font, "BFont 10px", x + 16, rowY, 10, TEXT_PRIMARY, LAYER_CONTENT_FG);
        bRender.drawText(font, "BFont 20px", x + 130, rowY - 4, 20, ACCENT_LIGHT, LAYER_CONTENT_FG);
        bRender.drawText(font, "BFont 32px", x + 290, rowY - 10, 32, ACCENT, LAYER_CONTENT_FG);
        rowY += 46;

        bRender.drawText(font, "Primary", x + 16, rowY, 15, TEXT_PRIMARY, LAYER_CONTENT_FG);
        bRender.drawText(font, "Muted", x + 100, rowY, 15, TEXT_MUTED, LAYER_CONTENT_FG);
        bRender.drawText(font, "Accent", x + 180, rowY, 15, ACCENT, LAYER_CONTENT_FG);
        bRender.drawTextShadow(font, "Shadowed", x + 270, rowY, 15, TEXT_PRIMARY, LAYER_CONTENT_FG);

        return logicalY + panelH;
    }

    private int drawTexturesSection(int x, int logicalY, int sectionIndex) {
        sectionAnchor[sectionIndex] = logicalY;
        int panelH = 210;
        int y = panelY(logicalY);
        panelFrame(x, y, this.width - x - 20, panelH, "Textures & 9-Slice");

        int rowY = y + 48;
        bRender.drawTexture(texture, x + 16, rowY, 80, 40, 0xFFFFFFFF, false, LAYER_CONTENT_FG);
        bRender.drawTextureCropped(texture, x + 106, rowY, 40, 40, 0, 0, 8, 8, ACCENT_LIGHT, false, LAYER_CONTENT_FG);
        bRender.drawTextureTiled(texture, x + 156, rowY, 80, 40, 0xFFFFFFFF, false, LAYER_CONTENT_FG);
        bRender.drawTexture(texture, x + 246, rowY, 80, 40, ACCENT, false, LAYER_CONTENT_FG);
        rowY += 60;

        bRender.drawTexture9Slice(nineSlice, x + 16, rowY, 110, 60, 0xFFFFFFFF, false, LAYER_CONTENT_FG);
        bRender.drawTexture9Slice(nineSlice, x + 136, rowY, 260, 90, 0xFFFFFFFF, false, LAYER_CONTENT_FG);
        bRender.drawText(font, "Info Panel", x + 156, rowY + 14, 14, ACCENT_LIGHT, LAYER_CONTENT_FG + 1);
        bRender.drawText(font, "9-slice scales without stretching corners", x + 156, rowY + 36, 10, TEXT_MUTED, LAYER_CONTENT_FG + 1);
        bRender.drawTexture9Slice(nineSlice, x + 406, rowY, 130, 90, ACCENT_LIGHT, false, LAYER_CONTENT_FG);

        return logicalY + panelH;
    }

    private int drawWidgetsSection(int x, int logicalY, int sectionIndex) {
        sectionAnchor[sectionIndex] = logicalY;
        int panelH = 160;
        int y = panelY(logicalY);
        panelFrame(x, y, this.width - x - 20, panelH, "Widgets");

        int rowY = y + 48;
        for (int i = 0; i < 3; i++) {
            float progress = 0.5f + 0.5f * (float) Math.sin(time * 0.7 + i * 1.3);
            int px = x + 16 + i * 150;
            bRender.roundRect(px, rowY, 130, 10, 0x33FFFFFF, 5, LAYER_CONTENT_FG);
            bRender.roundRect(px, rowY, Math.max(6, (int) (130 * progress)), 10,
                    new Gradient(ACCENT_DEEP, ACCENT), GradientDirection.LEFT_RIGHT, 5, LAYER_CONTENT_FG + 1);
        }
        rowY += 34;

        for (int i = 0; i < 3; i++) {
            float t = 0.5f + 0.5f * (float) Math.sin(time * 1.1 + i * 2.0);
            int tx = x + 16 + i * 150;
            bRender.roundRect(tx, rowY, 44, 20, t > 0.5f ? ACCENT_FAINT : 0x33FFFFFF, 10, LAYER_CONTENT_FG);
            int thumbX = tx + 10 + (int) (t * 24);
            bRender.circle(thumbX, rowY + 10, 8, t > 0.5f ? ACCENT : TEXT_MUTED, LAYER_CONTENT_FG + 1);
        }
        rowY += 40;

        String[] badges = {"NEW", "BETA", "GPU"};
        int bx = x + 16;
        for (String badge : badges) {
            int bw = 46;
            bRender.roundRect(bx, rowY, bw, 22, ACCENT_FAINT, 11, LAYER_CONTENT_FG);
            bRender.strokeRounded(bx, rowY, bw, 22, ACCENT, 11, 1, LAYER_CONTENT_FG);
            bRender.drawText(font, badge, bx + 8, rowY + 6, 10, ACCENT_LIGHT, LAYER_CONTENT_FG + 1);
            bx += bw + 10;
        }

        return logicalY + panelH;
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