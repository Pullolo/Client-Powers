package net.pullolo.clientpowers.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class RenderHelper {
    private RenderHelper() {}

    public static void drawFilledCircle(GuiGraphicsExtractor ctx, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int halfW = (int) Math.sqrt((double) r * r - (double) dy * dy);
            ctx.fill(cx - halfW, cy + dy, cx + halfW + 1, cy + dy + 1, color);
        }
    }

    public static void drawCircleOutline(GuiGraphicsExtractor ctx, int cx, int cy, int r, int thickness, int color) {
        drawFilledCircle(ctx, cx, cy, r, color);
        drawFilledCircle(ctx, cx, cy, r - thickness, 0);
    }

    /** Draws a rounded rectangle using fill approximation. */
    public static void drawRoundedRect(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int r, int color) {
        if (r <= 0) { ctx.fill(x, y, x + w, y + h, color); return; }
        r = Math.min(r, Math.min(w / 2, h / 2));
        // Center cross
        ctx.fill(x + r, y, x + w - r, y + h, color);
        ctx.fill(x, y + r, x + r, y + h - r, color);
        ctx.fill(x + w - r, y + r, x + w, y + h - r, color);
        // Corners
        fillCorner(ctx, x + r, y + r, r, color, true, true);
        fillCorner(ctx, x + w - r - 1, y + r, r, color, false, true);
        fillCorner(ctx, x + r, y + h - r - 1, r, color, true, false);
        fillCorner(ctx, x + w - r - 1, y + h - r - 1, r, color, false, false);
    }

    private static void fillCorner(GuiGraphicsExtractor ctx, int cx, int cy, int r, int color, boolean left, boolean up) {
        for (int dy = 0; dy <= r; dy++) {
            int halfW = (int) Math.sqrt((double) r * r - (double) dy * dy);
            int y1 = up ? cy - dy : cy + dy;
            int x1 = left ? cx - halfW : cx;
            int x2 = left ? cx + 1 : cx + halfW + 1;
            ctx.fill(x1, y1, x2, y1 + 1, color);
        }
    }

    public static void drawRoundedRectOutline(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int r, int thickness, int color) {
        for (int t = 0; t < thickness; t++) {
            drawRoundedRectHollow(ctx, x + t, y + t, w - 2 * t, h - 2 * t, Math.max(0, r - t), color);
        }
    }

    private static void drawRoundedRectHollow(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int r, int color) {
        // Top and bottom bars
        ctx.fill(x + r, y, x + w - r, y + 1, color);
        ctx.fill(x + r, y + h - 1, x + w - r, y + h, color);
        // Left and right bars
        ctx.fill(x, y + r, x + 1, y + h - r, color);
        ctx.fill(x + w - 1, y + r, x + w, y + h - r, color);
        // Corner arcs
        drawArcOutline(ctx, x + r, y + r, r, color, true, true);
        drawArcOutline(ctx, x + w - r - 1, y + r, r, color, false, true);
        drawArcOutline(ctx, x + r, y + h - r - 1, r, color, true, false);
        drawArcOutline(ctx, x + w - r - 1, y + h - r - 1, r, color, false, false);
    }

    private static void drawArcOutline(GuiGraphicsExtractor ctx, int cx, int cy, int r, int color, boolean left, boolean up) {
        for (int angle = 0; angle <= 90; angle += 2) {
            double rad = Math.toRadians(angle);
            int dx = (int) Math.round(Math.cos(rad) * r);
            int dy = (int) Math.round(Math.sin(rad) * r);
            int px = left ? cx - dx : cx + dx;
            int py = up ? cy - dy : cy + dy;
            ctx.fill(px, py, px + 1, py + 1, color);
        }
    }

    public static void drawToggle(GuiGraphicsExtractor ctx, int x, int y, boolean on, int accentColor) {
        int bg = on ? accentColor : 0xFF333333;
        drawRoundedRect(ctx, x, y, 30, 16, 8, bg);
        int dotX = on ? x + 16 : x + 2;
        drawFilledCircle(ctx, dotX + 6, y + 8, 5, 0xFFEEEEEE);
    }

    public static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }
}
