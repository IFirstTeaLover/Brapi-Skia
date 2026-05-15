package dev.bsprout.brapi.client;


import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.bsprout.brapi.client.renderHolders.Circle;
import dev.bsprout.brapi.client.renderHolders.RoundRect;
import dev.bsprout.brapi.client.renderHolders.Rect;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

import static dev.bsprout.brapi.client.Brapi.roundedRectShader;

public class BRender {

    private final List<RoundRect> roundRects =
            new ArrayList<>();

    private final List<Rect> rects = new ArrayList<>();
    private final List<Circle> circles = new ArrayList<>();

    public void roundRect(
            int x, int y,
            int width, int height,
            int color,
            int r1, int r2,
            int r3, int r4
    ) {

        roundRects.add(
                new RoundRect(
                        x, y,
                        width, height,
                        color,
                        r1, r2, r3, r4
                )
        );
    }

    public void rect(
            int x, int y,
            int width, int height,
            int color
    ) {

        rects.add(
                new Rect(
                        x, y,
                        width, height,
                        color
                )
        );
    }

    public void circle(
            int x, int y,
            int width, int height,
            int color
    ) {

        circles.add(
                new Circle(
                        x, y,
                        width, height,
                        color
                )
        );
    }

    public void flush(GuiGraphics graphics) {

        for (RoundRect cmd : roundRects) {

            renderRoundRect(
                    graphics,

                    cmd.x,
                    cmd.y,

                    cmd.width,
                    cmd.height,

                    cmd.color,

                    cmd.r1,
                    cmd.r2,
                    cmd.r3,
                    cmd.r4
            );
        }

        for (Rect cmd : rects){
            renderRect(
                    graphics,

                    cmd.x,
                    cmd.y,

                    cmd.width,
                    cmd.height,

                    cmd.color
            );
        }

        for (Circle cmd : circles){
            renderRoundRect(
                    graphics,

                    cmd.x,
                    cmd.y,

                    cmd.width,
                    cmd.height,

                    cmd.color, cmd.height / 2, cmd.height / 2, cmd.height / 2, cmd.height / 2
            );
        }


        roundRects.clear();
        rects.clear();
        circles.clear();
    }

    private void renderRoundRect(
            GuiGraphics graphics,

            int x, int y,
            int width, int height,

            int color,

            int r1, int r2,
            int r3, int r4
    ) {
        float a = (float) (color >> 24 & 255) / 255.0f;
        float r = (float) (color >> 16 & 255) / 255.0f;
        float g = (float) (color >> 8 & 255) / 255.0f;
        float b = (float) (color & 255) / 255.0f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(() -> roundedRectShader);

        if (roundedRectShader.getUniform("RectSize") != null) {
            setUniform("RectSize", (float)width, (float)height);
        }
        if (roundedRectShader.getUniform("Radius") != null) {
            setUniform("Radius", (float)r1);
        }
        if (roundedRectShader.getUniform("Color") != null) {
            setUniform("Color", r, g, b, a);
        }

        Matrix4f matrix = graphics.pose().last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        bufferBuilder.addVertex(matrix, x, y, 0).setUv(0, 0);
        bufferBuilder.addVertex(matrix, x, y + height, 0).setUv(0, 1);
        bufferBuilder.addVertex(matrix, x + width, y + height, 0).setUv(1, 1);
        bufferBuilder.addVertex(matrix, x + width, y, 0).setUv(1, 0);

        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());

        RenderSystem.disableBlend();
    }

    private void renderRect(GuiGraphics graphics,

                            int x, int y,
                            int width, int height,

                            int color){
        graphics.fill(x, y, x + width, y + height, color);
    }


    private void setUniform(String name, float... values) {
        var uniform = roundedRectShader.getUniform(name);
        if (uniform != null) {
            uniform.set(values);
        }
    }
}