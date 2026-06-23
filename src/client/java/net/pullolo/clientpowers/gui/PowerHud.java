package net.pullolo.clientpowers.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.pullolo.clientpowers.power.Power;

public final class PowerHud {
    private PowerHud() {}

    public static void drawPowerVignette(GuiGraphicsExtractor ctx, int w, int h, Power power) {
        int depth = 22;
        int rgb = power.accentColor & 0x00FFFFFF;
        int maxAlpha = switch (power) {
            case FLAME      -> 55;
            case FROST      -> 40;
            case THUNDER    -> 35;
            case VOID       -> 75;
            case STARGAZER  -> 40;
            case NATURE     -> 45;
            case SHADOW     -> 65;
            case OCEAN      -> 45;
            case BLOOD -> {
                double ph = (System.currentTimeMillis() % 1500) / 1500.0;
                double b1 = Math.max(0.0, 1.0 - Math.abs(ph - 0.07) * 22);
                double b2 = Math.max(0.0, 1.0 - Math.abs(ph - 0.22) * 22);
                yield (int)(30 + 65 * Math.max(b1, b2));
            }
            case WIND       -> 28;
            case CRYSTAL    -> 20;
            case LODESTAR   -> 32;
            case NINJA      -> 45;
            default         -> 0;
        };
        for (int i = 0; i < depth; i++) {
            float t2    = 1f - (float) i / depth;
            int   alpha = (int)(maxAlpha * t2 * t2);
            int   col   = (alpha << 24) | rgb;
            ctx.fill(i, i, w - i, i + 1,         col);
            ctx.fill(i, h - i - 1, w - i, h - i, col);
            ctx.fill(i, i + 1, i + 1, h - i - 1, col);
            ctx.fill(w-i-1, i+1, w-i, h-i-1,     col);
        }
    }

    public static void drawThunderSpeedLines(GuiGraphicsExtractor ctx, int w, int h) {
        long t  = System.currentTimeMillis();
        int  cx = w / 2;
        for (int i = 0; i < 6; i++) {
            long phase = (t / 3 + (long) i * 89) % cx;
            int len   = 25 + (i * 37) % 55;
            int sy    = h / 8 + i * (h * 6 / 8) / 5;
            int alpha = (int)(18 + 30 * Math.abs(Math.sin(t / 350.0 + i)));
            int color = (alpha << 24) | 0xFFE040;
            ctx.fill(Math.max(0, cx - (int)phase - len), sy, Math.min(w, cx - (int)phase), sy + 1, color);
            ctx.fill(Math.max(0, cx + (int)phase), sy, Math.min(w, cx + (int)phase + len), sy + 1, color);
        }
    }

    public static void drawFrostSnowflakes(GuiGraphicsExtractor ctx, int w, int h) {
        long t    = System.currentTimeMillis();
        int  band = w / 5;
        for (int i = 0; i < 12; i++) {
            long seed  = (long) i * 137;
            float speed = 0.022f + (seed % 5) * 0.008f;
            int startX = (i % 2 == 0) ? (int)(seed % band) : w - band + (int)(seed % band);
            int xi = (int)(startX + Math.sin(t / 2000.0 + i) * 5);
            int yi = (int)((t * speed + seed * 51) % h);
            int alpha = 55 + (int)(seed % 35);
            int color = (alpha << 24) | 0xADD8FF;
            ctx.fill(xi - 1, yi,     xi + 2, yi + 1, color);
            ctx.fill(xi,     yi - 1, xi + 1, yi + 2, color);
        }
    }

    public static void drawStargazerStars(GuiGraphicsExtractor ctx, int w, int h) {
        long t = System.currentTimeMillis();
        for (int i = 0; i < 30; i++) {
            int x = Math.abs((i * 73856093 + 1234567) % w);
            int y = Math.abs((i * 19349663 + 7654321) % h);
            double twinkle = Math.sin(t / (500.0 + i * 70.0) + i * 1.9);
            int    alpha   = (int)(40 + 100 * (twinkle * 0.5 + 0.5));
            switch (i % 4) {
                case 0 -> {
                    int c = (Math.min(255, alpha + 60) << 24) | 0xFFFFDD;
                    ctx.fill(x - 1, y,     x + 2, y + 1, c);
                    ctx.fill(x,     y - 1, x + 1, y + 2, c);
                }
                case 1 -> ctx.fill(x, y, x + 2, y + 2, (alpha << 24) | 0xCCDDFF);
                case 2 -> ctx.fill(x, y, x + 1, y + 1, (Math.min(255, alpha + 90) << 24) | 0xFFFFFF);
                default -> ctx.fill(x, y, x + 1, y + 1, ((alpha / 3) << 24) | 0xDDEEFF);
            }
        }
        for (int i = 0; i < 5; i++) {
            int bx    = Math.abs((i * 131071 + 99991) % w);
            int dy2   = (int)((t / (3000L + i * 500L) + (long) i * 217) % h);
            int alpha = (int)(55 + 55 * Math.abs(Math.sin(t / 1500.0 + i)));
            ctx.fill(bx, dy2, bx + 1, dy2 + 1, (alpha << 24) | 0xEEFFEE);
        }
        long shootPhase = t % 8000L;
        if (shootPhase < 1200L) {
            float p  = shootPhase / 1200f;
            int   hx = (int)(w * 0.15f + p * w * 0.6f);
            int   hy = (int)(h * 0.08f + p * h * 0.30f);
            for (int k = 0; k < 40; k++) {
                float fade = (1f - (float) k / 40) * (1f - p);
                int   a    = (int)(190 * fade);
                if (a <= 0) continue;
                ctx.fill(hx - k - 1, hy - k / 2, hx - k, hy - k / 2 + 1, (a << 24) | 0xFFFFCC);
            }
        }
    }

    public static void drawNatureBloom(GuiGraphicsExtractor ctx, int w, int h) {
        long t = System.currentTimeMillis();
        for (int i = 0; i < 15; i++) {
            long seed  = (long) i * 137;
            float speed = 0.018f + (seed % 5) * 0.007f;
            int startX = (int)(Math.abs(seed * 57L) % w);
            int xi     = (int)(startX + Math.sin(t / 1800.0 + i * 0.7) * 8);
            xi = Math.max(1, Math.min(w - 2, xi));
            int yi    = (int)((t * speed + seed * 43) % h);
            int alpha = 45 + (int)(seed % 40);
            ctx.fill(xi - 1, yi, xi + 2, yi + 1, (alpha << 24) | 0x44FF66);
            ctx.fill(xi, yi - 1, xi + 1, yi + 2, (alpha << 24) | 0x44FF66);
        }
    }

    public static void drawShadowWisps(GuiGraphicsExtractor ctx, int w, int h) {
        long t = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            long  seed  = (long) i * 193;
            float pulse = (float) Math.abs(Math.sin(t / (700.0 + i * 130.0) + i * 1.3));
            int   alpha = (int)(8 + 28 * pulse);
            int   xi    = (i % 2 == 0) ? (int)(seed % (w / 5)) : w - 1 - (int)(seed % (w / 5));
            int yStart  = (int)(seed * 31 % h);
            int len     = 20 + (int)(seed % 70);
            ctx.fill(xi, yStart, xi + 1, Math.min(h, yStart + len), (alpha << 24) | 0x1A0030);
        }
    }

    public static void drawShadowSpeedLines(GuiGraphicsExtractor ctx, int w, int h) {
        long t  = System.currentTimeMillis();
        int  cx = w / 2;
        for (int i = 0; i < 5; i++) {
            long phase = (t / 4 + (long) i * 103) % cx;
            int len   = 20 + (i * 41) % 45;
            int sy    = h / 6 + i * (h * 4 / 6) / 4;
            int alpha = (int)(10 + 18 * Math.abs(Math.sin(t / 400.0 + i)));
            int color = (alpha << 24) | 0x250040;
            ctx.fill(Math.max(0, cx - (int)phase - len), sy, Math.min(w, cx - (int)phase), sy + 1, color);
            ctx.fill(Math.max(0, cx + (int)phase), sy, Math.min(w, cx + (int)phase + len), sy + 1, color);
        }
    }

    public static void drawOceanBubbles(GuiGraphicsExtractor ctx, int w, int h) {
        long t = System.currentTimeMillis();
        for (int i = 0; i < 18; i++) {
            long  seed  = (long) i * 113;
            float speed = 0.016f + (seed % 6) * 0.007f;
            int   bx    = (int)(Math.abs(seed * 71L) % w);
            int   xi    = (int)(bx + Math.sin(t / 1400.0 + i * 0.9) * 5);
            xi = Math.max(0, Math.min(w - 2, xi));
            int yi = (int)(h - (t * speed + seed * 67) % h);
            if (yi < 0 || yi >= h) continue;
            ctx.fill(xi, yi, xi + 2, yi + 2, ((35 + (int)(seed % 45)) << 24) | 0x00DDCC);
        }
    }

    public static void drawBloodDrips(GuiGraphicsExtractor ctx, int w, int h) {
        long t = System.currentTimeMillis();
        for (int i = 0; i < 7; i++) {
            long  seed  = (long) i * 211;
            float speed = 0.010f + (seed % 4) * 0.005f;
            int   xi    = (int)(seed * 83 % w);
            int   yi    = (int)((t * speed + seed * 59) % (h / 3));
            int   len   = 10 + (int)(seed % 14);
            int   alpha = 55 + (int)(seed % 45);
            ctx.fill(xi, yi, xi + 1, Math.min(h, yi + len), (alpha << 24) | 0x8B0000);
            ctx.fill(xi - 1, yi + len - 2, xi + 2, yi + len + 1, ((alpha * 2 / 3) << 24) | 0x8B0000);
        }
    }

    public static void drawWindStreaks(GuiGraphicsExtractor ctx, int w, int h, boolean sprinting) {
        long elapsed = System.currentTimeMillis();
        int count = sprinting ? 14 : 7;
        for (int i = 0; i < count; i++) {
            long seed = (long) i * 157L;
            double speed = 0.06 + (seed % 5) * 0.03;
            int len = 35 + (int)(seed % 90);
            int sy  = (int)((seed * 47) % h);
            int baseX = (int)(w - ((elapsed * speed + seed * 89.0) % (w + len + 20)));
            int alpha = (int)(10 + 22 * Math.abs(Math.sin(elapsed / 600.0 + i * 0.8)));
            if (sprinting) alpha = Math.min(255, (int)(alpha * 1.9));
            int x1 = Math.max(0, baseX), x2 = Math.min(w, baseX + len);
            if (x2 > x1) ctx.fill(x1, sy, x2, sy + 1, (alpha << 24) | 0xCCEEFF);
        }
    }

    public static void drawCrystalPrism(GuiGraphicsExtractor ctx, int w, int h) {
        long t = System.currentTimeMillis();
        for (int i = 0; i < 24; i++) {
            long seed = (long) i * 167;
            int xi, yi, zone = i % 4;
            if      (zone == 0) { xi = (int)(seed * 37 % (w / 5));          yi = (int)(seed * 53 % h); }
            else if (zone == 1) { xi = w - 1 - (int)(seed * 37 % (w / 5)); yi = (int)(seed * 53 % h); }
            else if (zone == 2) { xi = (int)(seed * 37 % w);                yi = (int)(seed * 53 % (h / 5)); }
            else                { xi = (int)(seed * 37 % w);                yi = h - 1 - (int)(seed * 53 % (h / 5)); }
            double hue = t / 2500.0 + i * 0.26;
            int r = (int)(127 + 127 * Math.sin(hue));
            int g = (int)(127 + 127 * Math.sin(hue + 2.094));
            int b = (int)(127 + 127 * Math.sin(hue + 4.189));
            int alpha = (int)(15 + 55 * Math.abs(Math.sin(t / (350.0 + i * 40) + i * 0.9)));
            ctx.fill(xi, yi, xi + 2, yi + 2, (alpha << 24) | (r << 16) | (g << 8) | b);
        }
    }

    public static void drawMoonPhaseHud(GuiGraphicsExtractor ctx, int w, int h, Minecraft client) {
        if (client.level == null) return;
        long    dayTime  = client.level.getGameTime();
        int     phase    = (int)((dayTime / 24000L) % 8L);
        long    days     = dayTime / 24000L;
        long    timeOfDay = dayTime % 24000L;
        boolean isNight  = timeOfDay >= 13000 && timeOfDay < 23000;
        boolean fullMoon = (phase == 0);
        long    t        = System.currentTimeMillis();
        int     accent   = Power.STARGAZER.accentColor & 0x00FFFFFF;
        int panelW = 122, panelH = 54, px = w - panelW - 5, py = 5;

        if (fullMoon) {
            int ga = (int)(30 + 22 * Math.abs(Math.sin(t / 500.0)));
            RenderHelper.drawRoundedRect(ctx, px - 3, py - 3, panelW + 6, panelH + 6, 10, (ga << 24) | 0xFFCC44);
        }
        RenderHelper.drawRoundedRect(ctx, px, py, panelW, panelH, 7, 0xCC0A0A16);
        RenderHelper.drawRoundedRectOutline(ctx, px, py, panelW, panelH, 7, 1,
                fullMoon ? 0xAAFFCC44 : (0x44000000 | accent));
        int barColor = fullMoon
                ? (((int)(150 + 80 * Math.abs(Math.sin(t / 500.0)))) << 24 | 0xFFCC44)
                : (0x88000000 | accent);
        RenderHelper.drawRoundedRect(ctx, px, py, panelW, 3, 2, barColor);

        int[] starX = {46, 100, 64, 85, 55, 93};
        int[] starY = {10,  16, 34,  44, 48, 28};
        for (int i = 0; i < starX.length; i++) {
            int sa = (int)(10 + 30 * Math.abs(Math.sin(t / (700.0 + i * 130) + i)));
            ctx.fill(px + starX[i], py + starY[i], px + starX[i] + 1, py + starY[i] + 1, (sa << 24) | 0xCCDDFF);
        }

        drawMoonIcon(ctx, px + 21, py + panelH / 2, 10, phase);
        ctx.text(client.font, "Night " + days,
                px + 39, py + 9, RenderHelper.withAlpha(0xFF000000 | accent, 200), false);
        ctx.text(client.font, getMoonPhaseName(phase),
                px + 39, py + 22, fullMoon ? 0xFFFFCC44 : 0xFFCCCCCC, false);
        if (fullMoon) {
            int wa = (int)(170 + 70 * Math.abs(Math.sin(t / 380.0)));
            ctx.text(client.font, "!! Danger !!",
                    px + 39, py + 36, RenderHelper.withAlpha(0xFFFF4444, wa), false);
        } else {
            ctx.text(client.font, isNight ? "Nighttime" : "Daytime",
                    px + 39, py + 36, 0xFF555568, false);
        }
    }

    private static void drawMoonIcon(GuiGraphicsExtractor ctx, int cx, int cy, int r, int phase) {
        if (phase == 4) {
            RenderHelper.drawCircleOutline(ctx, cx, cy, r, 1, 0xFF2E2E44);
            ctx.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFF181828);
            return;
        }
        RenderHelper.drawFilledCircle(ctx, cx, cy, r, 0xFFEEEECC);
        int shadowX = switch (phase) {
            case 0 -> Integer.MIN_VALUE;
            case 1 -> cx + (int)(r * 1.6);
            case 2 -> cx + r;
            case 3 -> cx + (int)(r * 0.45);
            case 5 -> cx - (int)(r * 0.45);
            case 6 -> cx - r;
            case 7 -> cx - (int)(r * 1.6);
            default -> Integer.MIN_VALUE;
        };
        if (shadowX != Integer.MIN_VALUE) RenderHelper.drawFilledCircle(ctx, shadowX, cy, r, 0xFF0A0A16);
    }

    private static String getMoonPhaseName(int phase) {
        return switch (phase) {
            case 0 -> "Full Moon";      case 1 -> "Waning Gibbous";
            case 2 -> "Last Quarter";   case 3 -> "Waning Crescent";
            case 4 -> "New Moon";       case 5 -> "Waxing Crescent";
            case 6 -> "First Quarter";  case 7 -> "Waxing Gibbous";
            default -> "Unknown";
        };
    }

    public static void drawWeatherForecast(GuiGraphicsExtractor ctx, int w, int h, Minecraft client) {
        if (client.level == null || client.player == null) return;
        boolean raining  = client.level.isRaining();
        boolean thunder  = client.level.isThundering();
        float   rainGrad = client.level.getRainLevel(1.0f);
        boolean isNight  = (client.level.getGameTime() % 24000L) >= 13000;
        long    t        = System.currentTimeMillis();
        int panelW = 152, panelH = 40, px = (w - 152) / 2, py = 5;
        int bAccent = (thunder ? Power.THUNDER.accentColor : Power.WIND.accentColor) & 0x00FFFFFF;
        int bAlpha  = thunder ? (int)(85 + 65 * Math.abs(Math.sin(t / 240.0))) : 70;

        if (thunder) {
            int ga = (int)(18 + 14 * Math.abs(Math.sin(t / 240.0)));
            RenderHelper.drawRoundedRect(ctx, px - 2, py - 2, panelW + 4, panelH + 4, 10, (ga << 24) | bAccent);
        }
        RenderHelper.drawRoundedRect(ctx, px, py, panelW, panelH, 8, 0xCC07070F);
        RenderHelper.drawRoundedRectOutline(ctx, px, py, panelW, panelH, 8, 1, (bAlpha << 24) | bAccent);
        int lineA = thunder ? (int)(130 + 90 * Math.abs(Math.sin(t / 240.0))) : 85;
        RenderHelper.drawRoundedRect(ctx, px, py, panelW, 3, 2, (lineA << 24) | bAccent);

        drawWeatherIcon(ctx, px + 22, py + panelH / 2, raining, thunder, t);
        ctx.fill(px + 41, py + 8, px + 42, py + panelH - 8, 0xFF1C1C2A);

        String condition; int condColor;
        if (thunder)      { condition = "Storm";                         condColor = Power.THUNDER.accentColor; }
        else if (raining) { condition = "Rainy";                         condColor = 0xFF7799EE; }
        else              { condition = isNight ? "Clear Night" : "Clear"; condColor = 0xFFFFDD55; }
        ctx.text(client.font, condition, px + 49, py + 9, condColor, false);

        if (raining || thunder) {
            int barX = px + 49, barY = py + 24, barW = panelW - 60;
            ctx.fill(barX, barY, barX + barW, barY + 3, 0xFF181828);
            ctx.fill(barX, barY, barX + (int)(barW * Math.max(0, rainGrad)), barY + 3,
                    thunder ? Power.THUNDER.accentColor : 0xFF5588CC);
            ctx.text(client.font,
                    rainGrad > 0.85f ? "Intense" : rainGrad > 0.45f ? "Moderate" : "Building",
                    barX, py + 29, 0xFF666688, false);
        } else {
            ctx.text(client.font,
                    isNight ? "Starry · No rain" : "Sunny · No rain", px + 49, py + 24, 0xFF555568, false);
        }
    }

    private static void drawWeatherIcon(GuiGraphicsExtractor ctx, int cx, int cy, boolean rain, boolean thunder, long t) {
        if (!rain && !thunder) {
            ctx.fill(cx - 3, cy - 3, cx + 4, cy + 4, 0xFFFFDD44);
            ctx.fill(cx - 1, cy - 8, cx + 2, cy - 5, 0xFFFFDD44);
            ctx.fill(cx - 1, cy + 6, cx + 2, cy + 9, 0xFFFFDD44);
            ctx.fill(cx - 8, cy - 1, cx - 5, cy + 2, 0xFFFFDD44);
            ctx.fill(cx + 6, cy - 1, cx + 9, cy + 2, 0xFFFFDD44);
            int d = (int)((t / 550) % 2);
            ctx.fill(cx - 6 + d, cy - 6 + d, cx - 4 + d, cy - 4 + d, 0xFFFFEE55);
            ctx.fill(cx + 4 - d, cy - 6 + d, cx + 7 - d, cy - 4 + d, 0xFFFFEE55);
            ctx.fill(cx - 6 + d, cy + 4 - d, cx - 4 + d, cy + 7 - d, 0xFFFFEE55);
            ctx.fill(cx + 4 - d, cy + 4 - d, cx + 7 - d, cy + 7 - d, 0xFFFFEE55);
        } else {
            ctx.fill(cx - 7, cy - 2, cx + 7, cy + 4, 0xFFBBBBBB);
            ctx.fill(cx - 5, cy - 5, cx + 3, cy - 1, 0xFFCCCCCC);
            ctx.fill(cx - 1, cy - 7, cx + 5, cy - 3, 0xFFDDDDDD);
            if (thunder) {
                ctx.fill(cx + 2, cy + 4, cx + 5, cy + 8,  0xFFF1C40F);
                ctx.fill(cx - 1, cy + 7, cx + 3, cy + 12, 0xFFF1C40F);
                ctx.fill(cx + 3, cy + 5, cx + 4, cy + 7, ((int)(t / 100)) % 3 == 0 ? 0xFFFFFFAA : 0xFFF5D33B);
            } else {
                for (int i = 0; i < 4; i++) {
                    int dropY = cy + 5 + (int)((t / 210.0 + i * 1.2) % 10);
                    if (dropY < cy + 15) ctx.fill(cx - 5 + i * 4, dropY, cx - 4 + i * 4, dropY + 3, 0xFF6699EE);
                }
            }
        }
    }

    public static void drawCompassLine(GuiGraphicsExtractor ctx, int x1, int y1, int x2, int y2, int color) {
        int dx = x2 - x1, dy = y2 - y1;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps == 0) return;
        float sx = (float)dx / steps, sy = (float)dy / steps;
        for (int i = 0; i <= steps; i++) {
            ctx.fill(x1 + (int)(sx * i), y1 + (int)(sy * i),
                     x1 + (int)(sx * i) + 1, y1 + (int)(sy * i) + 1, color);
        }
    }
}
