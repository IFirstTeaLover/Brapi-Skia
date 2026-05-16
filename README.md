# BRAPI

## Setup

**Adding as dependency**

you can't lol
well actually you can do jar in jar but its not on maven central yet

**Using in code**

Everything is documented in code comments, but here are some examples:

- `bRender.roundRect(100, 100, 100, 100, 0xFFFF0000, 10);` — draws a 100x100 rounded rectangle at (100, 100) with red color and 10px corner radius

- `bRender.circle(100, 100, 50, 0xFFFF0000);` — draws a circle with 50px radius at top-left (100, 100) with red color

- `bRender.rect(100, 100, 100, 100, 0xFFFF0000);` — draws a red 100x100 rectangle at (100, 100)

- Call `bRender.flush(graphics)` after adding all elements to actually submit them for rendering (NOTE: Call super.render before it if you are extending Screen class)

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
