package dev.bsprout.brapi.client;

public class Gradient {
    public final int[] colors;

    public Gradient(int... colors) {
        this.colors = colors;
    }

    // Sample the gradient at t (0.0 - 1.0)
    public int sample(float t) {
        if (colors.length == 1) return colors[0];
        float scaled = t * (colors.length - 1);
        int i = (int) scaled;
        float f = scaled - i;
        if (i >= colors.length - 1) return colors[colors.length - 1];
        return lerpColor(colors[i], colors[i + 1], f);
    }

    public static int lerpColor(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF, aa = (a >> 24) & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF, ba = (b >> 24) & 0xFF;
        int r = (int)(ar + (br - ar) * t);
        int g = (int)(ag + (bg - ag) * t);
        int bl2 = (int)(ab + (bb - ab) * t);
        int alp = (int)(aa + (ba - aa) * t);
        return (alp << 24) | (r << 16) | (g << 8) | bl2;
    }
}