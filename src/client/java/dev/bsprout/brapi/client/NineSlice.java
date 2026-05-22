package dev.bsprout.brapi.client;

public record NineSlice(
        BTexture texture,
        int borderTop,
        int borderRight,
        int borderBottom,
        int borderLeft
) {}