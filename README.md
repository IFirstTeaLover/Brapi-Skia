# BRAPI

## Setup

**Adding as dependency**

you can't lol

well actually you can do jar in jar but it's not on maven central yet

**Using in code**

It's pretty straightforward: you can read the main render class (`dev.bsprout.brapi.client.BRender`) and it will have all draw methods and examples.

All colors are in ARGB format, e.g. `0xFFFF0000` = opaque red, `0x80FF0000` = 50% transparent red.

Also, everything is scaled in Minecraft's GuiScale, so (100, 100) will be (300, 300) on ui scale set to 3

**Filled shapes**

- `bRender.rect(100, 100, 100, 100, 0xFFFF0000);` — draws a red 100x100 rectangle at (100, 100)

- `bRender.roundRect(100, 100, 100, 100, 0xFFFF0000, 10);` — draws a 100x100 rounded rectangle at (100, 100) with 10px corner radius

- `bRender.roundRect(100, 100, 100, 100, 0xFFFF0000, 10, 5);` — rounded rectangle with 10px top-left/bottom-right radius and 5px top-right/bottom-left radius

- `bRender.roundRect(100, 100, 100, 100, 0xFFFF0000, 10, 5, 20, 30);` — rounded rectangle with individual corner radii (top-left, top-right, bottom-right, bottom-left)

- `bRender.circle(100, 100, 50, 0xFFFF0000);` — draws a circle with 50px radius at top-left (100, 100)

**Strokes (outline only)**

- `bRender.stroke(100, 100, 100, 100, 0xFFFF0000, 2);` — draws a 2px red outline rectangle at (100, 100)

- `bRender.strokeRounded(100, 100, 100, 100, 0xFFFF0000, 10, 2);` — draws a 2px red rounded outline with 10px corner radius

- `bRender.strokeRounded(100, 100, 100, 100, 0xFFFF0000, 10, 5, 2);` — rounded outline with 10px top-left/bottom-right radius and 5px top-right/bottom-left radius, 2px stroke

- `bRender.strokeRounded(100, 100, 100, 100, 0xFFFF0000, 10, 5, 20, 30, 2);` — rounded outline with individual corner radii, 2px stroke

**Filled + stroke**

- `bRender.roundRectStroked(100, 100, 100, 100, 0xFF0000FF, 0xFFFF0000, 10, 2);` — blue fill with 2px red stroke, 10px radius

- `bRender.roundRectStroked(100, 100, 100, 100, 0xFF0000FF, 0xFFFF0000, 10, 5, 2);` — blue fill with 2px red stroke, 10px top-left/bottom-right radius and 5px top-right/bottom-left radius

- `bRender.roundRectStroked(100, 100, 100, 100, 0xFF0000FF, 0xFFFF0000, 10, 5, 20, 30, 2);` — blue fill with 2px red stroke, individual corner radii

**Flushing**

Call `bRender.flush(graphics)` after adding all elements to submit them for rendering.

> **Note:** If you are extending `Screen`, call `super.render(...)` before `bRender.flush(graphics)` to make sure Brapi draws on top.

## License

    Brapi, Minecraft UI Rendering API
    Copyright (C) 2026 BSprout

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.