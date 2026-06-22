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
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.pullolo.clientpowers.config.Config;
import net.pullolo.clientpowers.cosmetic.CosmeticManager;
import net.pullolo.clientpowers.gui.GuideScreen;
import net.pullolo.clientpowers.gui.PowerHud;
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
    private long     lodestarlLastScanMs = 0;

    // VOID — portal finder state
    private BlockPos portalTarget     = null;
    private String   portalTypeName   = "";
    private int      portalTypeColor  = 0xFF888888;
    private double   portalDist       = 0;
    private long     portalLastScanMs = 0;

    // NINJA — PvP combat awareness state
    private float   ninjaHitAlpha     = 0f;
    private float   ninjaHitAngle     = 0f;
    private int     ninjaLastHurtTime = 0;
    private float   ninjaBlindAlpha   = 0f;
    private float   ninjaBackstabAngle = Float.NaN;
    private int     ninjaTick         = 0;

    @Override
    public void onInitializeClient() {
        Config.load();
        PowerManager.INSTANCE.loadFromConfig();

        KEY_POWER_WHEEL = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.clientpowers.power_wheel", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY));
        KEY_SETTINGS = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.clientpowers.settings",    InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_P, CATEGORY));
        KEY_GUIDE = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.clientpowers.guide",       InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY));

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
        if (client.player != null) tickPowerEffects();
    }

    private void tickPowerEffects() {
        Power power = PowerManager.INSTANCE.getActivePower();

        if (power == Power.THUNDER) {
            if (++thunderTick >= 100) { thunderFlash = 1.0f; thunderTick = 0; }
        } else {
            thunderTick = 0; thunderFlash = 0f;
        }

        if (power == Power.SHADOW) {
            if (++shadowTick >= 150) { shadowFlash = 1.0f; shadowTick = 0; }
        } else {
            shadowTick = 0; shadowFlash = 0f;
        }

        if (power != Power.LODESTAR) { lodestarlTarget = null; lodestarlLastScanMs = 0; }
        if (power != Power.VOID)     { portalTarget    = null; portalLastScanMs    = 0; }

        if (power == Power.NINJA) {
            MinecraftClient c = MinecraftClient.getInstance();
            if (c.player != null && c.world != null) {
                ninjaTick++;
                int hurt = c.player.hurtTime;
                if (hurt > ninjaLastHurtTime) { detectNinjaHitDirection(c); ninjaHitAlpha = 1.0f; }
                ninjaLastHurtTime = hurt;
                ninjaHitAlpha   = Math.max(0f, ninjaHitAlpha   - 0.04f);
                ninjaBlindAlpha = Math.max(0f, ninjaBlindAlpha - 0.05f);
                if (hasNinjaBlindSpot(c)) ninjaBlindAlpha = Math.min(1.0f, ninjaBlindAlpha + 0.15f);
                ninjaBackstabAngle = checkNinjaBackstab(c);
                if (Config.INSTANCE.particlesEnabled && c.player.isSprinting() && ninjaTick % 3 == 0) {
                    double yawRad = Math.toRadians(c.player.getYaw());
                    double bkX = Math.sin(yawRad), bkZ = -Math.cos(yawRad);
                    c.world.addParticleClient(ParticleTypes.ASH,
                        c.player.getX() + bkX * 0.3 + (Math.random() - 0.5) * 0.3,
                        c.player.getY() + 0.4 + Math.random() * 0.8,
                        c.player.getZ() + bkZ * 0.3 + (Math.random() - 0.5) * 0.3,
                        0, 0.01, 0);
                }
            }
        } else {
            ninjaHitAlpha = 0f; ninjaBlindAlpha = 0f;
            ninjaBackstabAngle = Float.NaN; ninjaLastHurtTime = 0; ninjaTick = 0;
        }
    }

    private void handleKeys(MinecraftClient client) {
        while (KEY_POWER_WHEEL.wasPressed()) if (client.currentScreen == null) client.setScreen(new PowerWheelScreen());
        while (KEY_SETTINGS.wasPressed())   if (client.currentScreen == null) client.setScreen(new SettingsScreen());
        while (KEY_GUIDE.wasPressed())      if (client.currentScreen == null) client.setScreen(new GuideScreen());
    }

    private void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden || client.currentScreen != null) return;

        int w = client.getWindow().getScaledWidth();
        int h = client.getWindow().getScaledHeight();
        Power power = PowerManager.INSTANCE.getActivePower();

        if (power != Power.NONE) PowerHud.drawPowerVignette(context, w, h, power);

        if (thunderFlash > 0) {
            context.fill(0, 0, w, h, ((int)(thunderFlash * 110) << 24) | 0xFFFF88);
            thunderFlash = Math.max(0f, thunderFlash - 0.08f);
        }
        if (shadowFlash > 0) {
            context.fill(0, 0, w, h, ((int) (shadowFlash * 70) << 24));
            shadowFlash = Math.max(0f, shadowFlash - 0.04f);
        }

        if (power == Power.THUNDER && client.player.isSprinting()) PowerHud.drawThunderSpeedLines(context, w, h);
        if (power == Power.SHADOW  && client.player.isSprinting()) PowerHud.drawShadowSpeedLines(context, w, h);
        if (power == Power.FROST)     PowerHud.drawFrostSnowflakes(context, w, h);
        if (power == Power.NATURE)    PowerHud.drawNatureBloom(context, w, h);
        if (power == Power.STARGAZER) PowerHud.drawStargazerStars(context, w, h);
        if (power == Power.OCEAN)     PowerHud.drawOceanBubbles(context, w, h);
        if (power == Power.SHADOW)    PowerHud.drawShadowWisps(context, w, h);
        if (power == Power.BLOOD)     PowerHud.drawBloodDrips(context, w, h);
        if (power == Power.WIND)      PowerHud.drawWindStreaks(context, w, h, client.player.isSprinting());
        if (power == Power.CRYSTAL)   PowerHud.drawCrystalPrism(context, w, h);

        if (power == Power.LODESTAR)  drawLodestarCompass(context, w, h, client);
        if (power == Power.STARGAZER) PowerHud.drawMoonPhaseHud(context, w, h, client);
        if (power == Power.VOID)      drawPortalFinder(context, w, h, client);
        if (power == Power.WIND)      PowerHud.drawWeatherForecast(context, w, h, client);
        if (power == Power.NINJA)     drawNinjaHUD(context, w, h, client);

        if (power == Power.NONE) return;
        int x = 5, y = h - 55;
        int textW = client.textRenderer.getWidth(power.displayName) + 16;
        RenderHelper.drawRoundedRect(context, x, y, textW, 16, 5, 0xCC101010);
        RenderHelper.drawRoundedRectOutline(context, x, y, textW, 16, 5, 1,
                0xAA000000 | (power.accentColor & 0x00FFFFFF));
        context.fill(x + 5, y + 6, x + 7, y + 10, power.accentColor);
        context.drawTextWithShadow(client.textRenderer, power.displayName, x + 10, y + 4, power.accentColor);
    }

    // ── LODESTAR ────────────────────────────────────────────────────────────────

    private void scanNearestOre(MinecraftClient client) {
        if (client.world == null || client.player == null) return;
        BlockPos origin = client.player.getBlockPos();
        int range = 16;
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        BlockPos bestPos = null; double bestDist = Double.MAX_VALUE; String bestName = "";
        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    double distSq = (double)(dx*dx + dy*dy + dz*dz);
                    if (distSq > range * range) continue;
                    mutable.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    String name = getOreName(client.world.getBlockState(mutable));
                    if (name != null && distSq < bestDist) { bestDist = distSq; bestPos = mutable.toImmutable(); bestName = name; }
                }
            }
        }
        lodestarlTarget   = bestPos;
        lodestarlOreName  = bestName;
        lodestarlOreColor = bestPos != null ? getOreColor(bestName) : 0xFF666666;
        lodestarlDist     = bestPos != null ? Math.sqrt(bestDist) : 0;
    }

    private static String getOreName(BlockState state) {
        if (state.isIn(BlockTags.DIAMOND_ORES))          return "Diamond";
        if (state.isIn(BlockTags.EMERALD_ORES))          return "Emerald";
        if (state.getBlock() == Blocks.ANCIENT_DEBRIS)   return "Debris";
        if (state.isIn(BlockTags.GOLD_ORES))             return "Gold";
        if (state.isIn(BlockTags.IRON_ORES))             return "Iron";
        if (state.isIn(BlockTags.COPPER_ORES))           return "Copper";
        if (state.isIn(BlockTags.LAPIS_ORES))            return "Lapis";
        if (state.isIn(BlockTags.REDSTONE_ORES))         return "Redstone";
        if (state.isIn(BlockTags.COAL_ORES))             return "Coal";
        return null;
    }

    private static int getOreColor(String name) {
        return switch (name) {
            case "Diamond"  -> 0xFF55DDFF; case "Emerald"  -> 0xFF55FF44;
            case "Debris"   -> 0xFFCC8833; case "Gold"     -> 0xFFFFAA00;
            case "Iron"     -> 0xFFDDBBA0; case "Copper"   -> 0xFFDD6633;
            case "Lapis"    -> 0xFF3366DD; case "Redstone" -> 0xFFFF3333;
            case "Coal"     -> 0xFF888888;
            default         -> Power.LODESTAR.accentColor;
        };
    }

    private void drawLodestarCompass(DrawContext ctx, int w, int h, MinecraftClient client) {
        if (client.player == null) return;
        long nowMs = System.currentTimeMillis();
        if (nowMs - lodestarlLastScanMs >= 100L) { lodestarlLastScanMs = nowMs; scanNearestOre(client); }

        int cx = w - 38, cy = h - 50, r = 18;
        int accent = Power.LODESTAR.accentColor & 0x00FFFFFF;
        RenderHelper.drawFilledCircle(ctx, cx, cy, r, 0xCC0D0D0D);
        RenderHelper.drawCircleOutline(ctx, cx, cy, r, 1, 0xBB000000 | accent);
        ctx.fill(cx, cy - r + 2, cx + 1, cy - r + 5, 0xFF888888);
        ctx.fill(cx, cy + r - 5, cx + 1, cy + r - 2, 0xFF444444);
        ctx.fill(cx - r + 2, cy, cx - r + 5, cy + 1, 0xFF444444);
        ctx.fill(cx + r - 5, cy, cx + r - 2, cy + 1, 0xFF444444);

        if (lodestarlTarget != null) {
            double dx = lodestarlTarget.getX() + 0.5 - client.player.getX();
            double dz = lodestarlTarget.getZ() + 0.5 - client.player.getZ();
            double rel = Math.atan2(dz, dx) - Math.toRadians(90.0 + client.player.getYaw());
            int nx = cx + (int)(Math.sin(rel) * (r - 5));
            int ny = cy - (int)(Math.cos(rel) * (r - 5));
            PowerHud.drawCompassLine(ctx, cx, cy, nx, ny, lodestarlOreColor);
            ctx.fill(nx - 1, ny - 1, nx + 2, ny + 2, lodestarlOreColor);
            ctx.drawCenteredTextWithShadow(client.textRenderer,
                    lodestarlOreName + " " + (int)lodestarlDist + "m", cx, cy + r + 4, lodestarlOreColor);
        } else {
            ctx.drawCenteredTextWithShadow(client.textRenderer, "—", cx, cy - 3, 0xFF444444);
            ctx.drawCenteredTextWithShadow(client.textRenderer, "none nearby", cx, cy + r + 4, 0xFF444444);
        }
    }

    // ── VOID ─────────────────────────────────────────────────────────────────────

    private void scanNearestPortal(MinecraftClient client) {
        if (client.world == null || client.player == null) return;
        BlockPos origin = client.player.getBlockPos();
        int range = 48;
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        BlockPos bestPos = null; double bestDist = Double.MAX_VALUE; String bestType = "";
        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    double distSq = (double)(dx * dx + dy * dy + dz * dz);
                    if (distSq > range * range) continue;
                    mutable.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    String type = getPortalTypeName(client.world.getBlockState(mutable));
                    if (type != null && distSq < bestDist) { bestDist = distSq; bestPos = mutable.toImmutable(); bestType = type; }
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

    private void drawPortalFinder(DrawContext ctx, int w, int h, MinecraftClient client) {
        if (client.player == null) return;
        long nowMs = System.currentTimeMillis();
        if (nowMs - portalLastScanMs >= 150L) { portalLastScanMs = nowMs; scanNearestPortal(client); }

        int cx = w - 38, cy = h - 50, r = 18;
        long t = System.currentTimeMillis();
        int accent = Power.VOID.accentColor & 0x00FFFFFF;
        RenderHelper.drawFilledCircle(ctx, cx, cy, r, 0xCC06000A);
        RenderHelper.drawCircleOutline(ctx, cx, cy, r, 1, 0xBB000000 | accent);
        ctx.fill(cx, cy - r + 2, cx + 1, cy - r + 5, 0xFF666677);
        ctx.fill(cx, cy + r - 5, cx + 1, cy + r - 2, 0xFF333344);
        ctx.fill(cx - r + 2, cy, cx - r + 5, cy + 1, 0xFF333344);
        ctx.fill(cx + r - 5, cy, cx + r - 2, cy + 1, 0xFF333344);

        for (int i = 0; i < 3; i++) {
            double angle = t / 2000.0 + i * 2.094;
            int ox = cx + (int)(Math.cos(angle) * (r + 3));
            int oy = cy + (int)(Math.sin(angle) * (r + 3));
            ctx.fill(ox, oy, ox + 1, oy + 1, ((int)(40 + 28 * Math.abs(Math.sin(t / 300.0 + i))) << 24) | 0x9900CC);
        }

        if (portalTarget != null) {
            double dx  = portalTarget.getX() + 0.5 - client.player.getX();
            double dz  = portalTarget.getZ() + 0.5 - client.player.getZ();
            double rel = Math.atan2(dz, dx) - Math.toRadians(90.0 + client.player.getYaw());
            int nx = cx + (int)(Math.sin(rel) * (r - 5));
            int ny = cy - (int)(Math.cos(rel) * (r - 5));
            PowerHud.drawCompassLine(ctx, cx, cy, nx, ny, portalTypeColor);
            ctx.fill(nx - 1, ny - 1, nx + 2, ny + 2, portalTypeColor);
            ctx.drawCenteredTextWithShadow(client.textRenderer,
                    portalTypeName + " " + (int)portalDist + "m", cx, cy + r + 4, portalTypeColor);
        } else {
            ctx.drawCenteredTextWithShadow(client.textRenderer, "—",         cx, cy - 3,    0xFF333355);
            ctx.drawCenteredTextWithShadow(client.textRenderer, "no portal", cx, cy + r + 4, 0xFF444466);
        }
    }

    // ── NINJA ────────────────────────────────────────────────────────────────────

    private void drawNinjaHUD(DrawContext ctx, int w, int h, MinecraftClient client) {
        int cx = w / 2, cy = h / 2;
        // 1. Crit window indicator
        boolean critReady = !client.player.isOnGround()
                && client.player.getVelocity().y < 0
                && !client.player.isTouchingWater()
                && client.player.getVehicle() == null;
        if (critReady) {
            long t = System.currentTimeMillis();
            int alpha = (int)(90 * (0.55 + 0.45 * Math.abs(Math.sin(t / 140.0))));
            ctx.fill(cx - 6, cy - 2, cx + 5, cy + 1, (alpha << 24) | 0xFFFF88);
            ctx.fill(cx - 2, cy - 6, cx + 1, cy + 5, (alpha << 24) | 0xFFFF88);
        }
        // 2. Blind spot rear arc on circle around crosshair
        if (ninjaBlindAlpha > 0.01f) {
            long t     = System.currentTimeMillis();
            int  alpha = (int)(ninjaBlindAlpha * 180 * (0.5 + 0.5 * Math.abs(Math.sin(t / 400.0))));
            drawNinjaArc(ctx, cx, cy, 22, Math.PI, Math.toRadians(70),
                    (alpha << 24) | (Power.NINJA.accentColor & 0x00FFFFFF));
        }
        // 3. Backstab indicator — directional arc pointing toward the backstab target
        if (!Float.isNaN(ninjaBackstabAngle)) {
            double rel   = Math.toRadians(ninjaBackstabAngle) - Math.toRadians(90.0 + client.player.getYaw());
            long   t     = System.currentTimeMillis();
            int    alpha = (int)(210 * (0.6 + 0.4 * Math.abs(Math.sin(t / 180.0))));
            drawNinjaArcArrow(ctx, cx, cy, 30, rel, Math.toRadians(38), (alpha << 24) | 0xFFCC44);
        }
        // 4. Hit direction arc + arrowhead on circle around crosshair
        if (ninjaHitAlpha > 0.01f) {
            double rel   = Math.toRadians(ninjaHitAngle) - Math.toRadians(90.0 + client.player.getYaw());
            int    alpha = (int)(ninjaHitAlpha * ninjaHitAlpha * 220);
            drawNinjaArcArrow(ctx, cx, cy, 22, rel, Math.toRadians(50), (alpha << 24) | 0xFF2222);
        }
    }

    private void detectNinjaHitDirection(MinecraftClient client) {
        if (client.world == null || client.player == null) return;
        LivingEntity nearest = null; double minDist = Double.MAX_VALUE;
        for (LivingEntity e : client.world.getEntitiesByClass(LivingEntity.class,
                client.player.getBoundingBox().expand(10.0),
                en -> en != client.player && en.isAlive())) {
            double d = e.squaredDistanceTo(client.player);
            if (d < minDist) { minDist = d; nearest = e; }
        }
        if (nearest != null) {
            ninjaHitAngle = (float) Math.toDegrees(Math.atan2(
                    nearest.getZ() - client.player.getZ(),
                    nearest.getX() - client.player.getX()));
        }
    }

    private boolean hasNinjaBlindSpot(MinecraftClient client) {
        if (client.world == null || client.player == null) return false;
        double yawRad = Math.toRadians(client.player.getYaw());
        double fwdX = -Math.sin(yawRad), fwdZ = Math.cos(yawRad);
        for (LivingEntity e : client.world.getEntitiesByClass(LivingEntity.class,
                client.player.getBoundingBox().expand(8.0),
                en -> en != client.player && en.isAlive()
                        && (en instanceof HostileEntity || en instanceof PlayerEntity))) {
            double dx = e.getX() - client.player.getX(), dz = e.getZ() - client.player.getZ();
            double d  = Math.sqrt(dx * dx + dz * dz);
            if (d < 0.5) continue;
            if ((dx / d) * fwdX + (dz / d) * fwdZ < -0.34) return true;
        }
        return false;
    }

    private float checkNinjaBackstab(MinecraftClient client) {
        if (client.world == null || client.player == null) return Float.NaN;
        double yawRad = Math.toRadians(client.player.getYaw());
        double pFwdX = -Math.sin(yawRad), pFwdZ = Math.cos(yawRad);
        for (LivingEntity e : client.world.getEntitiesByClass(LivingEntity.class,
                client.player.getBoundingBox().expand(4.0),
                en -> en != client.player && en.isAlive())) {
            double dx = e.getX() - client.player.getX(), dz = e.getZ() - client.player.getZ();
            double d  = Math.sqrt(dx * dx + dz * dz);
            if (d < 0.5 || d > 3.5) continue;
            if ((dx / d) * pFwdX + (dz / d) * pFwdZ < 0.4) continue;
            double eYaw = Math.toRadians(e.getYaw());
            if ((-dx / d) * -Math.sin(eYaw) + (-dz / d) * Math.cos(eYaw) < -0.5)
                return (float) Math.toDegrees(Math.atan2(dz, dx));
        }
        return Float.NaN;
    }

    // centerAngle: 0 = screen top (forward), increases clockwise
    private static void drawNinjaArcArrow(DrawContext ctx, int cx, int cy, int r,
                                           double centerAngle, double halfSpan, int color) {
        int aFull  = (color >>> 24) & 0xFF;
        int rgb    = color & 0x00FFFFFF;
        int aGlow  = aFull / 4;
        double step = 0.04;
        // Outer glow pass
        for (double a = centerAngle - halfSpan; a <= centerAngle + halfSpan; a += step) {
            int px = cx + (int)((r + 2) * Math.sin(a));
            int py = cy - (int)((r + 2) * Math.cos(a));
            ctx.fill(px - 2, py - 2, px + 4, py + 4, (aGlow << 24) | rgb);
        }
        // Main arc
        for (double a = centerAngle - halfSpan; a <= centerAngle + halfSpan; a += step) {
            int px = cx + (int)(r * Math.sin(a));
            int py = cy - (int)(r * Math.cos(a));
            ctx.fill(px - 1, py - 1, px + 3, py + 3, color);
        }
        // Filled triangle arrowhead: tip points outward, base on inner arc edge
        int tipX = cx + (int)((r + 7) * Math.sin(centerAngle));
        int tipY = cy - (int)((r + 7) * Math.cos(centerAngle));
        int b1X  = cx + (int)((r - 4) * Math.sin(centerAngle - 0.32));
        int b1Y  = cy - (int)((r - 4) * Math.cos(centerAngle - 0.32));
        int b2X  = cx + (int)((r - 4) * Math.sin(centerAngle + 0.32));
        int b2Y  = cy - (int)((r - 4) * Math.cos(centerAngle + 0.32));
        for (float t = 0; t <= 1.0f; t += 0.07f) {
            int lx = (int)(b1X + t * (tipX - b1X)), ly = (int)(b1Y + t * (tipY - b1Y));
            int rx = (int)(b2X + t * (tipX - b2X)), ry = (int)(b2Y + t * (tipY - b2Y));
            drawNinjaLine(ctx, lx, ly, rx, ry, color);
        }
    }

    private static void drawNinjaArc(DrawContext ctx, int cx, int cy, int r,
                                      double centerAngle, double halfSpan, int color) {
        int aFull = (color >>> 24) & 0xFF;
        int rgb   = color & 0x00FFFFFF;
        int aGlow = aFull / 4;
        double step = 0.04;
        for (double a = centerAngle - halfSpan; a <= centerAngle + halfSpan; a += step) {
            int px = cx + (int)((r + 2) * Math.sin(a));
            int py = cy - (int)((r + 2) * Math.cos(a));
            ctx.fill(px - 2, py - 2, px + 4, py + 4, (aGlow << 24) | rgb);
        }
        for (double a = centerAngle - halfSpan; a <= centerAngle + halfSpan; a += step) {
            int px = cx + (int)(r * Math.sin(a));
            int py = cy - (int)(r * Math.cos(a));
            ctx.fill(px - 1, py - 1, px + 3, py + 3, color);
        }
    }

    private static void drawNinjaLine(DrawContext ctx, int x1, int y1, int x2, int y2, int color) {
        int dx = x2 - x1, dy = y2 - y1;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps == 0) { ctx.fill(x1, y1, x1 + 2, y1 + 2, color); return; }
        for (int i = 0; i <= steps; i++) {
            int px = x1 + dx * i / steps, py = y1 + dy * i / steps;
            ctx.fill(px, py, px + 2, py + 2, color);
        }
    }
}
