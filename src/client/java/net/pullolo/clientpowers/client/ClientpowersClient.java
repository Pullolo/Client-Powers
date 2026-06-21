package net.pullolo.clientpowers.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.InputUtil;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.pullolo.clientpowers.config.Config;
import net.pullolo.clientpowers.cosmetic.CosmeticManager;
import net.pullolo.clientpowers.gui.GuideScreen;
import net.pullolo.clientpowers.gui.PowerWheelScreen;
import net.pullolo.clientpowers.gui.RenderHelper;
import net.pullolo.clientpowers.gui.SettingsScreen;
import net.pullolo.clientpowers.module.DynamicLightModule;
import net.pullolo.clientpowers.module.PlayerGlowModule;
import net.pullolo.clientpowers.module.ToggleSprintModule;
import net.pullolo.clientpowers.power.Power;
import net.pullolo.clientpowers.power.PowerManager;
import org.lwjgl.glfw.GLFW;

public class ClientpowersClient implements ClientModInitializer {

    private final long startTime = System.currentTimeMillis();
    public static KeyBinding KEY_POWER_WHEEL;
    public static KeyBinding KEY_SETTINGS;
    public static KeyBinding KEY_GUIDE;

    private static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of("clientpowers", "clientpowers"));
    private static boolean modulesLoaded = false;

    // THUNDER — periodic screen-flash state
    private float thunderFlash = 0f;
    private int   thunderTick  = 0;

    // SHADOW — periodic dim-pulse state
    private float shadowFlash = 0f;
    private int   shadowTick  = 0;

    // LODESTAR — ore scan state
    private BlockPos lodestarlTarget     = null;
    private String   lodestarlOreName    = "";
    private int      lodestarlOreColor   = 0xFF888888;
    private double   lodestarlDist       = 0;
    private long     lodestarlLastScanMs = 0; // scan triggered in HUD render, not in tick

    // VOID — portal finder state
    private BlockPos portalTarget     = null;
    private String   portalTypeName   = "";
    private int      portalTypeColor  = 0xFF888888;
    private double   portalDist       = 0;
    private long     portalLastScanMs = 0; // scan triggered in HUD render, not in tick

    @Override
    public void onInitializeClient() {
        Config.load();
        PowerManager.INSTANCE.loadFromConfig();

        KEY_POWER_WHEEL = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.clientpowers.power_wheel",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                CATEGORY
        ));
        KEY_SETTINGS = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.clientpowers.settings",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                CATEGORY
        ));
        KEY_GUIDE = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.clientpowers.guide",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        HudRenderCallback.EVENT.register(this::onHudRender);
    }

    private void onTick(MinecraftClient client) {
        if (!modulesLoaded && client.player != null) {
            DynamicLightModule.INSTANCE.loadFromConfig();
            PlayerGlowModule.INSTANCE.loadFromConfig();
            ToggleSprintModule.INSTANCE.loadFromConfig();
            modulesLoaded = true;
        }

        handleKeys(client);

        DynamicLightModule.INSTANCE.tick();
        PlayerGlowModule.INSTANCE.tick();
        ToggleSprintModule.INSTANCE.tick();
        CosmeticManager.INSTANCE.tick(client);

        if (client.player != null) {
            tickPowerEffects();
        }
    }

    private void tickPowerEffects() {
        Power power = PowerManager.INSTANCE.getActivePower();

        // THUNDER — Static Discharge: screen flash every 5 seconds
        if (power == Power.THUNDER) {
            if (++thunderTick >= 100) {
                thunderFlash = 1.0f;
                thunderTick  = 0;
            }
        } else {
            thunderTick  = 0;
            thunderFlash = 0f;
        }

        // SHADOW — Penumbra Pulse: dim overlay every 7.5 seconds
        if (power == Power.SHADOW) {
            if (++shadowTick >= 150) {
                shadowFlash = 1.0f;
                shadowTick  = 0;
            }
        } else {
            shadowTick  = 0;
            shadowFlash = 0f;
        }

        // LODESTAR / VOID — clear stale targets when the power is inactive
        // (scanning is now time-gated inside the HUD draw methods on the render thread)
        if (power != Power.LODESTAR) {
            lodestarlTarget     = null;
            lodestarlLastScanMs = 0;
        }
        if (power != Power.VOID) {
            portalTarget     = null;
            portalLastScanMs = 0;
        }
    }

    private void handleKeys(MinecraftClient client) {
        while (KEY_POWER_WHEEL.wasPressed()) {
            if (client.currentScreen == null) {
                client.setScreen(new PowerWheelScreen());
            }
        }
        while (KEY_SETTINGS.wasPressed()) {
            if (client.currentScreen == null) {
                client.setScreen(new SettingsScreen());
            }
        }
        while (KEY_GUIDE.wasPressed()) {
            if (client.currentScreen == null) {
                client.setScreen(new GuideScreen());
            }
        }
    }

    private void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden || client.currentScreen != null) return;

        int w = client.getWindow().getScaledWidth();
        int h = client.getWindow().getScaledHeight();
        Power power = PowerManager.INSTANCE.getActivePower();

        if (power != Power.NONE) {
            drawPowerVignette(context, w, h, power);
        }

        // THUNDER — periodic discharge flash
        if (thunderFlash > 0) {
            int alpha = (int)(thunderFlash * 110);
            context.fill(0, 0, w, h, (alpha << 24) | 0xFFFF88);
            thunderFlash = Math.max(0f, thunderFlash - 0.08f);
        }

        // SHADOW — periodic penumbra pulse (screen dims briefly)
        if (shadowFlash > 0) {
            int alpha = (int)(shadowFlash * 70);
            context.fill(0, 0, w, h, (alpha << 24) | 0x000000);
            shadowFlash = Math.max(0f, shadowFlash - 0.04f);
        }

        // THUNDER — speed lines burst outward from center while sprinting
        if (power == Power.THUNDER && client.player.isSprinting()) {
            drawThunderSpeedLines(context, w, h);
        }

        // SHADOW — dark afterimage streaks while sprinting
        if (power == Power.SHADOW && client.player.isSprinting()) {
            drawShadowSpeedLines(context, w, h);
        }

        // FROST — drifting snowflakes along screen edges
        if (power == Power.FROST) {
            drawFrostSnowflakes(context, w, h);
        }

        // NATURE — falling leaf bloom from top of screen
        if (power == Power.NATURE) {
            drawNatureBloom(context, w, h);
        }

        // STARGAZER — animated starfield across screen
        if (power == Power.STARGAZER) {
            drawStargazerStars(context, w, h);
        }

        // OCEAN — bubbles rising from bottom of screen
        if (power == Power.OCEAN) {
            drawOceanBubbles(context, w, h);
        }

        // SHADOW — dark wisps creeping from screen edges
        if (power == Power.SHADOW) {
            drawShadowWisps(context, w, h);
        }

        // BLOOD — blood drips from top corners
        if (power == Power.BLOOD) {
            drawBloodDrips(context, w, h);
        }

        // WIND — horizontal wind streaks, more intense when sprinting
        if (power == Power.WIND) {
            drawWindStreaks(context, w, h, client.player.isSprinting());
        }

        // CRYSTAL — rainbow prism sparkles along screen edges
        if (power == Power.CRYSTAL) {
            drawCrystalPrism(context, w, h);
        }

        // LODESTAR — ore compass in bottom-right corner
        if (power == Power.LODESTAR) {
            drawLodestarCompass(context, w, h, client);
        }

        // STARGAZER — moon phase panel (top-right)
        if (power == Power.STARGAZER) {
            drawMoonPhaseHud(context, w, h, client);
        }

        // VOID — portal finder compass (bottom-right)
        if (power == Power.VOID) {
            drawPortalFinder(context, w, h, client);
        }

        // WIND — weather forecast panel (top-centre)
        if (power == Power.WIND) {
            drawWeatherForecast(context, w, h, client);
        }

        // Power indicator chip (bottom-left corner)
        if (power == Power.NONE) return;
        int x = 5;
        int y = h - 55;
        int textW = client.textRenderer.getWidth(power.displayName) + 16;
        int barH  = 16;

        RenderHelper.drawRoundedRect(context, x, y, textW, barH, 5, 0xCC101010);
        RenderHelper.drawRoundedRectOutline(context, x, y, textW, barH, 5, 1,
                0xAA000000 | (power.accentColor & 0x00FFFFFF));
        context.fill(x + 5, y + 6, x + 7, y + 10, power.accentColor);
        context.drawTextWithShadow(client.textRenderer, power.displayName, x + 10, y + 4, power.accentColor);
    }

    // Lines burst outward from center-x: left half goes left, right half goes right.
    private void drawThunderSpeedLines(DrawContext ctx, int w, int h) {
        long t  = System.currentTimeMillis();
        int  cx = w / 2;
        for (int i = 0; i < 6; i++) {
            long phase = (t / 3 + (long) i * 89) % cx;
            int len   = 25 + (i * 37) % 55;
            int sy    = h / 8 + i * (h * 6 / 8) / 5;
            int alpha = (int)(18 + 30 * Math.abs(Math.sin(t / 350.0 + i)));
            int color = (alpha << 24) | 0xFFE040;

            // Left side: streak moving toward the left edge
            int leftHead = cx - (int) phase;
            ctx.fill(Math.max(0, leftHead - len), sy, Math.min(w, leftHead), sy + 1, color);

            // Right side: streak moving toward the right edge
            int rightHead = cx + (int) phase;
            ctx.fill(Math.max(0, rightHead), sy, Math.min(w, rightHead + len), sy + 1, color);
        }
    }

    // Tiny cross-shaped snowflakes drifting downward, constrained to screen edges.
    private void drawFrostSnowflakes(DrawContext ctx, int w, int h) {
        long t    = System.currentTimeMillis();
        int  band = w / 5;
        for (int i = 0; i < 12; i++) {
            long seed  = (long) i * 137;
            float speed = 0.022f + (seed % 5) * 0.008f;
            int startX = (i % 2 == 0)
                    ? (int)(seed % band)
                    : w - band + (int)(seed % band);
            int xi = (int)(startX + Math.sin(t / 2000.0 + i) * 5);
            int yi = (int)((t * speed + seed * 51) % h);
            int alpha = 55 + (int)(seed % 35);
            int color = (alpha << 24) | 0xADD8FF;
            ctx.fill(xi - 1, yi,     xi + 2, yi + 1, color);
            ctx.fill(xi,     yi - 1, xi + 1, yi + 2, color);
        }
    }

    // Animated starfield: twinkle, drift, and a periodic shooting star.
    private void drawStargazerStars(DrawContext ctx, int w, int h) {
        long t = System.currentTimeMillis();

        // 30 fixed-position stars with individual twinkle phases
        for (int i = 0; i < 30; i++) {
            int x = Math.abs((i * 73856093 + 1234567) % w);
            int y = Math.abs((i * 19349663 + 7654321) % h);

            double twinkle = Math.sin(t / (500.0 + i * 70.0) + i * 1.9);
            int    alpha   = (int)(40 + 100 * (twinkle * 0.5 + 0.5));

            switch (i % 4) {
                case 0 -> {  // 4-point cross (bright warm star)
                    int c = (Math.min(255, alpha + 60) << 24) | 0xFFFFDD;
                    ctx.fill(x - 1, y,     x + 2, y + 1, c);
                    ctx.fill(x,     y - 1, x + 1, y + 2, c);
                }
                case 1 -> {  // 2×2 blue-white dot
                    ctx.fill(x, y, x + 2, y + 2, (alpha << 24) | 0xCCDDFF);
                }
                case 2 -> {  // Tiny brilliant spark
                    ctx.fill(x, y, x + 1, y + 1, (Math.min(255, alpha + 90) << 24) | 0xFFFFFF);
                }
                default -> {  // Faint background pin-point
                    ctx.fill(x, y, x + 1, y + 1, ((alpha / 3) << 24) | 0xDDEEFF);
                }
            }
        }

        // 5 slowly drifting "wandering" stars
        for (int i = 0; i < 5; i++) {
            int   bx    = Math.abs((i * 131071 + 99991) % w);
            int   dy2   = (int)((t / (3000L + i * 500L) + (long) i * 217) % h);
            int   alpha = (int)(55 + 55 * Math.abs(Math.sin(t / 1500.0 + i)));
            ctx.fill(bx, dy2, bx + 1, dy2 + 1, (alpha << 24) | 0xEEFFEE);
        }

        // Shooting star every 8 seconds (1.2-second streak)
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

    // Falling leaf-pixel bloom from top of screen for NATURE.
    private void drawNatureBloom(DrawContext ctx, int w, int h) {
        long t = System.currentTimeMillis();
        for (int i = 0; i < 15; i++) {
            long seed  = (long) i * 137;
            float speed = 0.018f + (seed % 5) * 0.007f;
            int startX = (int)(Math.abs(seed * 57L) % w);
            int xi     = (int)(startX + Math.sin(t / 1800.0 + i * 0.7) * 8);
            xi = Math.max(1, Math.min(w - 2, xi));
            int yi     = (int)((t * speed + seed * 43) % h);
            int alpha  = 45 + (int)(seed % 40);
            int color  = (alpha << 24) | 0x44FF66;
            ctx.fill(xi - 1, yi, xi + 2, yi + 1, color);
            ctx.fill(xi, yi - 1, xi + 1, yi + 2, color);
        }
    }

    // Dark tendrils pulsing from screen edges for SHADOW.
    private void drawShadowWisps(DrawContext ctx, int w, int h) {
        long t = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            long  seed  = (long) i * 193;
            float pulse = (float) Math.abs(Math.sin(t / (700.0 + i * 130.0) + i * 1.3));
            int   alpha = (int)(8 + 28 * pulse);
            int   xi    = (i % 2 == 0)
                    ? (int)(seed % (w / 5))
                    : w - 1 - (int)(seed % (w / 5));
            int yStart  = (int)(seed * 31 % h);
            int len     = 20 + (int)(seed % 70);
            ctx.fill(xi, yStart, xi + 1, Math.min(h, yStart + len), (alpha << 24) | 0x1A0030);
        }
    }

    // Dark afterimage streaks bursting from center while sprinting for SHADOW.
    private void drawShadowSpeedLines(DrawContext ctx, int w, int h) {
        long t  = System.currentTimeMillis();
        int  cx = w / 2;
        for (int i = 0; i < 5; i++) {
            long phase = (t / 4 + (long) i * 103) % cx;
            int len   = 20 + (i * 41) % 45;
            int sy    = h / 6 + i * (h * 4 / 6) / 4;
            int alpha = (int)(10 + 18 * Math.abs(Math.sin(t / 400.0 + i)));
            int color = (alpha << 24) | 0x250040;
            int leftHead  = cx - (int) phase;
            ctx.fill(Math.max(0, leftHead - len), sy, Math.min(w, leftHead), sy + 1, color);
            int rightHead = cx + (int) phase;
            ctx.fill(Math.max(0, rightHead), sy, Math.min(w, rightHead + len), sy + 1, color);
        }
    }

    // Bubbles rising from the bottom of the screen for OCEAN.
    private void drawOceanBubbles(DrawContext ctx, int w, int h) {
        long t = System.currentTimeMillis();
        for (int i = 0; i < 18; i++) {
            long  seed  = (long) i * 113;
            float speed = 0.016f + (seed % 6) * 0.007f;
            int   bx    = (int)(Math.abs(seed * 71L) % w);
            int   xi    = (int)(bx + Math.sin(t / 1400.0 + i * 0.9) * 5);
            xi = Math.max(0, Math.min(w - 2, xi));
            int yi = (int)(h - (t * speed + seed * 67) % h);
            if (yi < 0 || yi >= h) continue;
            int alpha = 35 + (int)(seed % 45);
            ctx.fill(xi, yi, xi + 2, yi + 2, (alpha << 24) | 0x00DDCC);
        }
    }

    // Blood drips falling from the upper portion of the screen for BLOOD.
    private void drawBloodDrips(DrawContext ctx, int w, int h) {
        long t = System.currentTimeMillis();
        for (int i = 0; i < 7; i++) {
            long  seed  = (long) i * 211;
            float speed = 0.010f + (seed % 4) * 0.005f;
            int   xi    = (int)(seed * 83 % w);
            int   yi    = (int)((t * speed + seed * 59) % (h / 3));
            int   len   = 10 + (int)(seed % 14);
            int   alpha = 55 + (int)(seed % 45);
            ctx.fill(xi, yi, xi + 1, Math.min(h, yi + len), (alpha << 24) | 0x8B0000);
            // Round drip tip
            ctx.fill(xi - 1, yi + len - 2, xi + 2, yi + len + 1, ((alpha * 2 / 3) << 24) | 0x8B0000);
        }
    }

    // Horizontal wind streaks for WIND; more streaks and brighter when sprinting.
    private void drawWindStreaks(DrawContext ctx, int w, int h, boolean sprinting) {
        long elapsed = System.currentTimeMillis() - startTime;

        int count = sprinting ? 14 : 7;

        for (int i = 0; i < count; i++) {
            long seed = (long) i * 157L;

            // Use double for smooth movement
            double speed = 0.06 + (seed % 5) * 0.03;

            int len = 35 + (int) (seed % 90);
            int sy = (int) ((seed * 47) % h);

            // Smooth looping horizontal position
            int baseX = (int) (
                    w - ((elapsed * speed + seed * 89.0) % (w + len + 20))
            );

            // Pulsing alpha
            int alpha = (int) (10 + 22 * Math.abs(Math.sin(elapsed / 600.0 + i * 0.8)));

            if (sprinting) {
                alpha = Math.min(255, (int) (alpha * 1.9));
            }

            // Draw only if visible
            int x1 = Math.max(0, baseX);
            int x2 = Math.min(w, baseX + len);

            if (x2 > x1) {
                ctx.fill(
                        x1,
                        sy,
                        x2,
                        sy + 1,
                        (alpha << 24) | 0xCCEEFF
                );
            }
        }
    }

    // Rainbow sparkles cycling along all four screen edges for CRYSTAL.
    private void drawCrystalPrism(DrawContext ctx, int w, int h) {
        long t = System.currentTimeMillis();
        for (int i = 0; i < 24; i++) {
            long seed = (long) i * 167;
            int xi, yi;
            int zone = i % 4;
            if (zone == 0)      { xi = (int)(seed * 37 % (w / 5));         yi = (int)(seed * 53 % h); }
            else if (zone == 1) { xi = w - 1 - (int)(seed * 37 % (w / 5)); yi = (int)(seed * 53 % h); }
            else if (zone == 2) { xi = (int)(seed * 37 % w);                yi = (int)(seed * 53 % (h / 5)); }
            else                { xi = (int)(seed * 37 % w);                yi = h - 1 - (int)(seed * 53 % (h / 5)); }
            // Three-phase sine waves → smooth rainbow without HSV conversion
            double hue = t / 2500.0 + i * 0.26;
            int r = (int)(127 + 127 * Math.sin(hue));
            int g = (int)(127 + 127 * Math.sin(hue + 2.094));
            int b = (int)(127 + 127 * Math.sin(hue + 4.189));
            double twinkle = Math.abs(Math.sin(t / (350.0 + i * 40) + i * 0.9));
            int alpha = (int)(15 + 55 * twinkle);
            ctx.fill(xi, yi, xi + 2, yi + 2, (alpha << 24) | (r << 16) | (g << 8) | b);
        }
    }

    // Scan blocks in 16-block sphere; find nearest diamond/emerald/gold/debris ore.
    private void scanNearestOre(MinecraftClient client) {
        if (client.world == null || client.player == null) return;
        BlockPos origin = client.player.getBlockPos();
        int range = 16;
        BlockPos.Mutable mutable  = new BlockPos.Mutable();
        BlockPos         bestPos  = null;
        double           bestDist = Double.MAX_VALUE;
        String           bestName = "";

        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    double distSq = (double)(dx*dx + dy*dy + dz*dz);
                    if (distSq > range * range) continue;
                    mutable.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    String name = getOreName(client.world.getBlockState(mutable));
                    if (name == null) continue;
                    if (distSq < bestDist) {
                        bestDist = distSq;
                        bestPos  = mutable.toImmutable();
                        bestName = name;
                    }
                }
            }
        }

        lodestarlTarget   = bestPos;
        lodestarlOreName  = bestName;
        lodestarlOreColor = bestPos != null ? getOreColor(bestName) : 0xFF666666;
        lodestarlDist     = bestPos != null ? Math.sqrt(bestDist) : 0;
    }

    private static String getOreName(BlockState state) {
        if (state.isIn(BlockTags.DIAMOND_ORES))    return "Diamond";
        if (state.isIn(BlockTags.EMERALD_ORES))    return "Emerald";
        if (state.getBlock() == Blocks.ANCIENT_DEBRIS) return "Debris";
        if (state.isIn(BlockTags.GOLD_ORES))       return "Gold";
        if (state.isIn(BlockTags.IRON_ORES))       return "Iron";
        if (state.isIn(BlockTags.COPPER_ORES))     return "Copper";
        if (state.isIn(BlockTags.LAPIS_ORES))      return "Lapis";
        if (state.isIn(BlockTags.REDSTONE_ORES))   return "Redstone";
        if (state.isIn(BlockTags.COAL_ORES))       return "Coal";
        return null;
    }

    private static int getOreColor(String name) {
        return switch (name) {
            case "Diamond"  -> 0xFF55DDFF;
            case "Emerald"  -> 0xFF55FF44;
            case "Debris"   -> 0xFFCC8833;
            case "Gold"     -> 0xFFFFAA00;
            case "Iron"     -> 0xFFDDBBA0;
            case "Copper"   -> 0xFFDD6633;
            case "Lapis"    -> 0xFF3366DD;
            case "Redstone" -> 0xFFFF3333;
            case "Quartz"   -> 0xFFEEDDCC;
            case "Coal"     -> 0xFF888888;
            default         -> Power.LODESTAR.accentColor;
        };
    }

    // Compass needle pointing toward the nearest tracked ore, relative to player view.
    private void drawLodestarCompass(DrawContext ctx, int w, int h, MinecraftClient client) {
        if (client.player == null) return;

        // Re-scan at most every 100 ms — feels instant while keeping CPU cost negligible
        long nowMs = System.currentTimeMillis();
        if (nowMs - lodestarlLastScanMs >= 100L) {
            lodestarlLastScanMs = nowMs;
            scanNearestOre(client);
        }

        int cx = w - 38;
        int cy = h - 50;
        int r  = 18;
        int accent = Power.LODESTAR.accentColor & 0x00FFFFFF;

        RenderHelper.drawFilledCircle(ctx, cx, cy, r, 0xCC0D0D0D);
        RenderHelper.drawCircleOutline(ctx, cx, cy, r, 1, 0xBB000000 | accent);

        // Tick marks: brighter at top (= forward direction)
        ctx.fill(cx, cy - r + 2, cx + 1, cy - r + 5, 0xFF888888);
        ctx.fill(cx, cy + r - 5, cx + 1, cy + r - 2, 0xFF444444);
        ctx.fill(cx - r + 2, cy, cx - r + 5, cy + 1, 0xFF444444);
        ctx.fill(cx + r - 5, cy, cx + r - 2, cy + 1, 0xFF444444);

        if (lodestarlTarget != null) {
            double dx = lodestarlTarget.getX() + 0.5 - client.player.getX();
            double dz = lodestarlTarget.getZ() + 0.5 - client.player.getZ();
            double oreAngle     = Math.atan2(dz, dx);
            double forwardAngle = Math.toRadians(90.0 + client.player.getYaw());
            double rel          = oreAngle - forwardAngle;

            int nx = cx + (int)(Math.sin(rel) * (r - 5));
            int ny = cy - (int)(Math.cos(rel) * (r - 5));
            drawCompassLine(ctx, cx, cy, nx, ny, lodestarlOreColor);
            ctx.fill(nx - 1, ny - 1, nx + 2, ny + 2, lodestarlOreColor);

            String label = lodestarlOreName + " " + (int)lodestarlDist + "m";
            ctx.drawCenteredTextWithShadow(client.textRenderer, label,
                    cx, cy + r + 4, lodestarlOreColor);
        } else {
            ctx.drawCenteredTextWithShadow(client.textRenderer, "—",
                    cx, cy - 3, 0xFF444444);
            ctx.drawCenteredTextWithShadow(client.textRenderer, "none nearby",
                    cx, cy + r + 4, 0xFF444444);
        }
    }

    // VOID — scan 48-block sphere for the nearest nether/end portal, every 10 seconds
    private void scanNearestPortal(MinecraftClient client) {
        if (client.world == null || client.player == null) return;
        BlockPos         origin   = client.player.getBlockPos();
        int              range    = 48;
        BlockPos.Mutable mutable  = new BlockPos.Mutable();
        BlockPos         bestPos  = null;
        double           bestDist = Double.MAX_VALUE;
        String           bestType = "";

        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    double distSq = (double)(dx * dx + dy * dy + dz * dz);
                    if (distSq > range * range) continue;
                    mutable.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    String type = getPortalTypeName(client.world.getBlockState(mutable));
                    if (type == null) continue;
                    if (distSq < bestDist) { bestDist = distSq; bestPos = mutable.toImmutable(); bestType = type; }
                }
            }
        }
        portalTarget    = bestPos;
        portalTypeName  = bestType;
        portalTypeColor = bestPos != null ? getPortalColor(bestType) : 0xFF666688;
        portalDist      = bestPos != null ? Math.sqrt(bestDist) : 0;
    }

    private static String getPortalTypeName(BlockState state) {
        if (state.getBlock() == Blocks.NETHER_PORTAL)    return "Nether";
        if (state.getBlock() == Blocks.END_PORTAL)       return "End";
        if (state.getBlock() == Blocks.END_PORTAL_FRAME) return "End";
        return null;
    }

    private static int getPortalColor(String type) {
        return switch (type) {
            case "Nether" -> 0xFFBB66FF;
            case "End"    -> 0xFF55EE88;
            default       -> Power.VOID.accentColor;
        };
    }

    // VOID — circular compass pointing at the nearest portal, styled in void purple
    private void drawPortalFinder(DrawContext ctx, int w, int h, MinecraftClient client) {
        if (client.player == null) return;

        // Portals don't move — 150 ms is more than responsive enough
        long nowMs = System.currentTimeMillis();
        if (nowMs - portalLastScanMs >= 150L) {
            portalLastScanMs = nowMs;
            scanNearestPortal(client);
        }

        int  cx     = w - 38;
        int  cy     = h - 50;
        int  r      = 18;
        long t      = System.currentTimeMillis();
        int  accent = Power.VOID.accentColor & 0x00FFFFFF;

        // Background disc + accent ring
        RenderHelper.drawFilledCircle(ctx, cx, cy, r, 0xCC06000A);
        RenderHelper.drawCircleOutline(ctx, cx, cy, r, 1, 0xBB000000 | accent);

        // Cardinal tick marks (brighter at north = forward)
        ctx.fill(cx, cy - r + 2, cx + 1, cy - r + 5, 0xFF666677);
        ctx.fill(cx, cy + r - 5, cx + 1, cy + r - 2, 0xFF333344);
        ctx.fill(cx - r + 2, cy, cx - r + 5, cy + 1, 0xFF333344);
        ctx.fill(cx + r - 5, cy, cx + r - 2, cy + 1, 0xFF333344);

        // Slowly orbiting void-particle decorations
        for (int i = 0; i < 3; i++) {
            double angle = t / 2000.0 + i * 2.094;
            int    ox    = cx + (int)(Math.cos(angle) * (r + 3));
            int    oy    = cy + (int)(Math.sin(angle) * (r + 3));
            int    pa    = (int)(40 + 28 * Math.abs(Math.sin(t / 300.0 + i)));
            ctx.fill(ox, oy, ox + 1, oy + 1, (pa << 24) | 0x9900CC);
        }

        if (portalTarget != null) {
            double dx           = portalTarget.getX() + 0.5 - client.player.getX();
            double dz           = portalTarget.getZ() + 0.5 - client.player.getZ();
            double portalAngle  = Math.atan2(dz, dx);
            double forwardAngle = Math.toRadians(90.0 + client.player.getYaw());
            double rel          = portalAngle - forwardAngle;
            int    nx           = cx + (int)(Math.sin(rel) * (r - 5));
            int    ny           = cy - (int)(Math.cos(rel) * (r - 5));
            drawCompassLine(ctx, cx, cy, nx, ny, portalTypeColor);
            ctx.fill(nx - 1, ny - 1, nx + 2, ny + 2, portalTypeColor);
            ctx.drawCenteredTextWithShadow(client.textRenderer,
                    portalTypeName + " " + (int)portalDist + "m", cx, cy + r + 4, portalTypeColor);
        } else {
            ctx.drawCenteredTextWithShadow(client.textRenderer, "—",         cx, cy - 3,    0xFF333355);
            ctx.drawCenteredTextWithShadow(client.textRenderer, "no portal", cx, cy + r + 4, 0xFF444466);
        }
    }

    // STARGAZER — top-right panel showing moon phase icon, night counter, and full-moon warning
    private void drawMoonPhaseHud(DrawContext ctx, int w, int h, MinecraftClient client) {
        if (client.world == null) return;

        int     phase    = (int)((client.world.getTimeOfDay() / 24000L) % 8L);
        long    days     = client.world.getTimeOfDay() / 24000L;
        long    timeOfDay = client.world.getTimeOfDay() % 24000L;
        boolean isNight  = timeOfDay >= 13000 && timeOfDay < 23000;
        boolean fullMoon = (phase == 0);
        long    t        = System.currentTimeMillis();
        int     accent   = Power.STARGAZER.accentColor & 0x00FFFFFF;

        int panelW = 122;
        int panelH = 54;
        int px     = w - panelW - 5;
        int py     = 5;

        // Ambient glow behind panel on full moon
        if (fullMoon) {
            int ga = (int)(30 + 22 * Math.abs(Math.sin(t / 500.0)));
            RenderHelper.drawRoundedRect(ctx, px - 3, py - 3, panelW + 6, panelH + 6, 10, (ga << 24) | 0xFFCC44);
        }

        // Panel background + border
        RenderHelper.drawRoundedRect(ctx, px, py, panelW, panelH, 7, 0xCC0A0A16);
        RenderHelper.drawRoundedRectOutline(ctx, px, py, panelW, panelH, 7, 1,
                fullMoon ? 0xAAFFCC44 : (0x44000000 | accent));

        // Top accent bar (pulsing gold on full moon)
        int barColor = fullMoon
                ? (((int)(150 + 80 * Math.abs(Math.sin(t / 500.0)))) << 24 | 0xFFCC44)
                : (0x88000000 | accent);
        RenderHelper.drawRoundedRect(ctx, px, py, panelW, 3, 2, barColor);

        // Scattered twinkling star pixels
        int[] starX = {46, 100, 64, 85, 55, 93};
        int[] starY = {10,  16, 34,  44, 48, 28};
        for (int i = 0; i < starX.length; i++) {
            double tw = Math.abs(Math.sin(t / (700.0 + i * 130) + i));
            int    sa = (int)(10 + 30 * tw);
            ctx.fill(px + starX[i], py + starY[i], px + starX[i] + 1, py + starY[i] + 1, (sa << 24) | 0xCCDDFF);
        }

        // Moon icon
        drawMoonIcon(ctx, px + 21, py + panelH / 2, 10, phase);

        // Night counter
        ctx.drawTextWithShadow(client.textRenderer, "Night " + days,
                px + 39, py + 9, RenderHelper.withAlpha(0xFF000000 | accent, 200));

        // Phase name
        ctx.drawTextWithShadow(client.textRenderer, getMoonPhaseName(phase),
                px + 39, py + 22, fullMoon ? 0xFFFFCC44 : 0xFFCCCCCC);

        // Status / danger line
        if (fullMoon) {
            int wa = (int)(170 + 70 * Math.abs(Math.sin(t / 380.0)));
            ctx.drawTextWithShadow(client.textRenderer, "!! Danger !!",
                    px + 39, py + 36, RenderHelper.withAlpha(0xFFFF4444, wa));
        } else {
            ctx.drawTextWithShadow(client.textRenderer, isNight ? "Nighttime" : "Daytime",
                    px + 39, py + 36, 0xFF555568);
        }
    }

    // Draws a moon icon using a lit disc with a shadow disc offset to create the phase shape
    private static void drawMoonIcon(DrawContext ctx, int cx, int cy, int r, int phase) {
        int lit    = 0xFFEEEECC;
        int shadow = 0xFF0A0A16; // opaque near-black matching panel bg

        if (phase == 4) {
            // New Moon: faint ring + dim centre dot
            RenderHelper.drawCircleOutline(ctx, cx, cy, r, 1, 0xFF2E2E44);
            ctx.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFF181828);
            return;
        }

        RenderHelper.drawFilledCircle(ctx, cx, cy, r, lit);

        // Shadow disc offset determines which portion of the lit disc is hidden
        int shadowX = switch (phase) {
            case 0 -> Integer.MIN_VALUE;            // Full Moon  — no shadow
            case 1 -> cx + (int)(r * 1.6);          // Waning Gibbous
            case 2 -> cx + r;                        // Last Quarter
            case 3 -> cx + (int)(r * 0.45);         // Waning Crescent
            case 5 -> cx - (int)(r * 0.45);         // Waxing Crescent
            case 6 -> cx - r;                        // First Quarter
            case 7 -> cx - (int)(r * 1.6);          // Waxing Gibbous
            default -> Integer.MIN_VALUE;
        };
        if (shadowX != Integer.MIN_VALUE) {
            RenderHelper.drawFilledCircle(ctx, shadowX, cy, r, shadow);
        }
    }

    private static String getMoonPhaseName(int phase) {
        return switch (phase) {
            case 0 -> "Full Moon";
            case 1 -> "Waning Gibbous";
            case 2 -> "Last Quarter";
            case 3 -> "Waning Crescent";
            case 4 -> "New Moon";
            case 5 -> "Waxing Crescent";
            case 6 -> "First Quarter";
            case 7 -> "Waxing Gibbous";
            default -> "Unknown";
        };
    }

    // WIND — centred top-screen panel with animated weather icon and condition info
    private void drawWeatherForecast(DrawContext ctx, int w, int h, MinecraftClient client) {
        if (client.world == null || client.player == null) return;

        boolean raining  = client.world.isRaining();
        boolean thunder  = client.world.isThundering();
        float   rainGrad = client.world.getRainGradient(1.0f);
        long    timeOfDay = client.world.getTimeOfDay() % 24000L;
        boolean isNight  = timeOfDay >= 13000 && timeOfDay < 23000;
        long    t        = System.currentTimeMillis();

        int panelW  = 152;
        int panelH  = 40;
        int px      = (w - panelW) / 2;
        int py      = 5;
        int bAccent = (thunder ? Power.THUNDER.accentColor : Power.WIND.accentColor) & 0x00FFFFFF;
        int bAlpha  = thunder ? (int)(85 + 65 * Math.abs(Math.sin(t / 240.0))) : 70;

        // Ambient glow on storm
        if (thunder) {
            int ga = (int)(18 + 14 * Math.abs(Math.sin(t / 240.0)));
            RenderHelper.drawRoundedRect(ctx, px - 2, py - 2, panelW + 4, panelH + 4, 10, (ga << 24) | bAccent);
        }

        // Panel background + border
        RenderHelper.drawRoundedRect(ctx, px, py, panelW, panelH, 8, 0xCC07070F);
        RenderHelper.drawRoundedRectOutline(ctx, px, py, panelW, panelH, 8, 1, (bAlpha << 24) | bAccent);

        // Top accent line
        int lineA = thunder ? (int)(130 + 90 * Math.abs(Math.sin(t / 240.0))) : 85;
        RenderHelper.drawRoundedRect(ctx, px, py, panelW, 3, 2, (lineA << 24) | bAccent);

        // Animated weather icon (left side)
        drawWeatherIcon(ctx, px + 22, py + panelH / 2, raining, thunder, t);

        // Thin separator
        ctx.fill(px + 41, py + 8, px + 42, py + panelH - 8, 0xFF1C1C2A);

        // Condition label
        String condition;
        int    condColor;
        if (thunder)      { condition = "Storm";                 condColor = Power.THUNDER.accentColor; }
        else if (raining) { condition = "Rainy";                 condColor = 0xFF7799EE; }
        else              { condition = isNight ? "Clear Night" : "Clear"; condColor = 0xFFFFDD55; }
        ctx.drawTextWithShadow(client.textRenderer, condition, px + 49, py + 9, condColor);

        // Secondary info row
        if (raining || thunder) {
            int barX = px + 49;
            int barY = py + 24;
            int barW = panelW - 60;
            ctx.fill(barX, barY, barX + barW, barY + 3, 0xFF181828);
            ctx.fill(barX, barY, barX + (int)(barW * Math.max(0, rainGrad)), barY + 3,
                    thunder ? Power.THUNDER.accentColor : 0xFF5588CC);
            String intensity = rainGrad > 0.85f ? "Intense" : rainGrad > 0.45f ? "Moderate" : "Building";
            ctx.drawTextWithShadow(client.textRenderer, intensity, barX, py + 29, 0xFF666688);
        } else {
            String sub = isNight ? "Starry · No rain" : "Sunny · No rain";
            ctx.drawTextWithShadow(client.textRenderer, sub, px + 49, py + 24, 0xFF555568);
        }
    }

    private static void drawWeatherIcon(DrawContext ctx, int cx, int cy,
                                         boolean rain, boolean thunder, long t) {
        if (!rain && !thunder) {
            // Sun: central disc + 8 rays (diagonal pair shifts every 600 ms for sparkle)
            ctx.fill(cx - 3, cy - 3, cx + 4, cy + 4, 0xFFFFDD44);
            ctx.fill(cx - 1, cy - 8, cx + 2, cy - 5, 0xFFFFDD44); // N
            ctx.fill(cx - 1, cy + 6, cx + 2, cy + 9, 0xFFFFDD44); // S
            ctx.fill(cx - 8, cy - 1, cx - 5, cy + 2, 0xFFFFDD44); // W
            ctx.fill(cx + 6, cy - 1, cx + 9, cy + 2, 0xFFFFDD44); // E
            int d = (int)((t / 550) % 2);
            ctx.fill(cx - 6 + d, cy - 6 + d, cx - 4 + d, cy - 4 + d, 0xFFFFEE55); // NW
            ctx.fill(cx + 4 - d, cy - 6 + d, cx + 7 - d, cy - 4 + d, 0xFFFFEE55); // NE
            ctx.fill(cx - 6 + d, cy + 4 - d, cx - 4 + d, cy + 7 - d, 0xFFFFEE55); // SW
            ctx.fill(cx + 4 - d, cy + 4 - d, cx + 7 - d, cy + 7 - d, 0xFFFFEE55); // SE
        } else {
            // Cloud body (two overlapping rounded rects)
            ctx.fill(cx - 7, cy - 2, cx + 7, cy + 4, 0xFFBBBBBB);
            ctx.fill(cx - 5, cy - 5, cx + 3, cy - 1, 0xFFCCCCCC);
            ctx.fill(cx - 1, cy - 7, cx + 5, cy - 3, 0xFFDDDDDD);

            if (thunder) {
                // Lightning bolt (two angled segments)
                ctx.fill(cx + 2, cy + 4, cx + 5, cy + 8,  0xFFF1C40F);
                ctx.fill(cx - 1, cy + 7, cx + 3, cy + 12, 0xFFF1C40F);
                // Bright core flicker every 3rd 100 ms tick
                int bright = ((int)(t / 100)) % 3 == 0 ? 0xFFFFFFAA : 0xFFF5D33B;
                ctx.fill(cx + 3, cy + 5, cx + 4, cy + 7, bright);
            } else {
                // Animated rain drops scrolling downward
                for (int i = 0; i < 4; i++) {
                    int dropX   = cx - 5 + i * 4;
                    int dropOff = (int)((t / 210.0 + i * 1.2) % 10);
                    int dropY   = cy + 5 + dropOff;
                    if (dropY < cy + 15)
                        ctx.fill(dropX, dropY, dropX + 1, dropY + 3, 0xFF6699EE);
                }
            }
        }
    }

    private static void drawCompassLine(DrawContext ctx, int x1, int y1, int x2, int y2, int color) {
        int dx = x2 - x1, dy = y2 - y1;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps == 0) return;
        float sx = (float)dx / steps, sy = (float)dy / steps;
        for (int i = 0; i <= steps; i++) {
            ctx.fill(x1 + (int)(sx * i), y1 + (int)(sy * i),
                     x1 + (int)(sx * i) + 1, y1 + (int)(sy * i) + 1, color);
        }
    }

    // Quadratic-falloff colored vignette around screen edges.
    private void drawPowerVignette(DrawContext ctx, int w, int h, Power power) {
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
                // Heartbeat: two sharp peaks (lub-DUB) every 1.5 seconds
                double ph = (System.currentTimeMillis() % 1500) / 1500.0;
                double b1 = Math.max(0.0, 1.0 - Math.abs(ph - 0.07) * 22);
                double b2 = Math.max(0.0, 1.0 - Math.abs(ph - 0.22) * 22);
                yield (int)(30 + 65 * Math.max(b1, b2));
            }
            case WIND       -> 28;
            case CRYSTAL    -> 20;
            case LODESTAR   -> 32;
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
}
