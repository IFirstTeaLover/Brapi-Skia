package dev.bsprout.brapi.client.renderHolders;

public class RoundRect {

    public int x, y;
    public int width, height;

    public int color;

    public int r1, r2, r3, r4;

    public RoundRect(
            int x, int y,
            int width, int height,
            int color,
            int r1, int r2,
            int r3, int r4
    ) {
        this.x = x;
        this.y = y;

        this.width = width;
        this.height = height;

        this.color = color;

        this.r1 = r1;
        this.r2 = r2;
        this.r3 = r3;
        this.r4 = r4;
    }
}