package dev.bsprout.brapi.client;

public class BUtils {
    // Helper - create NineSlice from texture and border sizes
    // Example: NineSlice slice = BUtils.nineslicify(tex, 8, 8, 8, 8);
    // Deprecated: use new NineSlice(...) Directly!
    // Example: NineSlice slice = new NineSlice(tex, 8, 8, 8, 8);
    @Deprecated
    public static NineSlice nineslicify(BTexture texture, int borderTop, int borderRight, int borderBottom, int borderLeft) {
        return new NineSlice(texture, borderTop, borderRight, borderBottom, borderLeft);
    }
}
