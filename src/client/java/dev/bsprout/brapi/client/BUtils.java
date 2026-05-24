package dev.bsprout.brapi.client;

public class BUtils {
    // Helper - create NineSlice from texture and border sizes
    // Example: NineSlice slice = BRender.nineslicify(tex, 8, 8, 8, 8);
    public static NineSlice nineslicify(BTexture texture, int borderTop, int borderRight, int borderBottom, int borderLeft) {
        return new NineSlice(texture, borderTop, borderRight, borderBottom, borderLeft);
    }
}
