# <div align="center">Brapi</div>
A UI rendering library for Fabric mods. Supports rounded rectangles, text, gradients, textures, and 9-slice sprites with a simple API.

## 📦 Setup
**Adding as dependency**

Add the repository and dependency to your `build.gradle`:
```gradle
repositories {
    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = "https://api.modrinth.com/maven"
            }
        }
        filter {
            includeGroup "maven.modrinth"
        }
    }
}
dependencies {
    modImplementation "maven.modrinth:brapi:mc1.21.11-0.25"
}
```
`fabric.mod.json`:
```json
"depends": {
    "brapi": "*"
}
```

## Using in code
Create a `BRender` instance, add draw calls, then call `flush(graphics)` to submit. Everything renders at end of frame sorted by layer. 

🎨 All colors are ARGB: `0xFFFF0000` = opaque red, `0x80FF0000` = 50% transparent red.<br>
📐 Coordinates are in GuiScale units: (100, 100) at scale 3 = (300, 300) pixels.

## 🟥 Filled shapes
- `bRender.rect(100, 100, 100, 100, 0xFFFF0000, layer);` - red 100x100 rectangle
- `bRender.roundRect(100, 100, 100, 100, 0xFFFF0000, 10, layer);` - rounded rectangle, 10px radius
- `bRender.roundRect(100, 100, 100, 100, 0xFFFF0000, 10, 5, layer);` - 10px TL/BR, 5px TR/BL radius
- `bRender.roundRect(100, 100, 100, 100, 0xFFFF0000, 10, 5, 20, 30, layer);` - individual corner radii
- `bRender.circle(100, 100, 50, 0xFFFF0000, layer);` - circle, 50px radius

## 🎨 Gradients
All filled shapes support gradients via `Gradient` and `GradientDirection`:
- `bRender.roundRect(100, 100, 100, 100, gradient, GradientDirection.LEFT_RIGHT, 10, layer);`

Directions: `LEFT_RIGHT`, `TOP_BOTTOM`, `TOP_LEFT_BOTTOM_RIGHT`, `TOP_RIGHT_BOTTOM_LEFT`

## ✏️ Strokes (outline only)
- `bRender.stroke(100, 100, 100, 100, 0xFFFF0000, 2, layer);` - 2px outline rectangle
- `bRender.strokeRounded(100, 100, 100, 100, 0xFFFF0000, 10, 2, layer);` - 2px rounded outline, 10px radius
- `bRender.strokeRounded(100, 100, 100, 100, 0xFFFF0000, 10, 5, 2, layer);` - 10px TL/BR, 5px TR/BL
- `bRender.strokeRounded(100, 100, 100, 100, 0xFFFF0000, 10, 5, 20, 30, 2, layer);` - individual radii

## 🖊️ Filled + stroke
- `bRender.roundRectStroked(100, 100, 100, 100, 0xFF0000FF, 0xFFFF0000, 10, 2, layer);` - blue fill, 2px red stroke

## 🔤 Text
- `bRender.drawText(font, "Hello!", 100, 100, 24, 0xFFFFFFFF, layer);` - BFont text
- `bRender.drawText(font, formattedCharSequence, 100, 100, 24, 0xFFFFFFFF, layer);` - formatted text with per-character colors
- `bRender.drawTextShadow(font, "Hello!", 100, 100, 24, 0xFFFFFFFF, layer);` - with shadow
- `bRender.drawText("Hello!", 100, 100, 0xFFFFFFFF, layer);` - MC font fallback

## 🖼️ Textures
- `bRender.drawTexture(tex, 100, 100, 200, 150, 0xFFFFFFFF, linear, layer);` - stretch to fill
- `bRender.drawTextureCropped(tex, 100, 100, 64, 64, 0, 0, 32, 32, 0xFFFFFFFF, linear, layer);` - crop
- `bRender.drawTextureTiled(tex, 100, 100, 200, 200, 0xFFFFFFFF, linear, layer);` - tile
- `bRender.drawTexture9Slice(slice, 100, 100, 300, 200, 0xFFFFFFFF, linear, layer);` - 9-slice

## 🔄 Flushing
```java
BRender r = new BRender();
r.roundRect(...);
r.flush(graphics); // submit to draw list
```
