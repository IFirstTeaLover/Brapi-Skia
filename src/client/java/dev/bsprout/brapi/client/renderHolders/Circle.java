package dev.bsprout.brapi.client.renderHolders;

public class Circle {

    public int x, y;
    public int width, height;

    public int color;

    public Circle(
            int x, int y,
            int width, int height,
            int color
    ) {
        this.x = x;
        this.y = y;

        this.width = width;
        this.height = height;

        this.color = color;
    }
}