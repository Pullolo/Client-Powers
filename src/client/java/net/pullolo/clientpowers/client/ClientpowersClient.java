package net.pullolo.clientpowers.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
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

    public static KeyMapping KEY_POWER_WHEEL;
    public static KeyMapping KEY_SETTINGS;
    public static KeyMapping KEY_GUIDE;

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

        KeyMapping.Category powersCategory = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("clientpowers", "powers"));
        KEY_POWER_WHEEL = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.clientpowers.power_wheel", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R,
                powersCategory));
        KEY_SETTINGS = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.clientpowers.settings",    InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_P,
                powersCategory));
        KEY_GUIDE = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.clientpowers.guide",       InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G,
                powersCategory));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("clientpowers", "hud"), this::onHudRender);
    }

    private void onTick(Minecraft client) {
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
            Minecraft c = Minecraft.getInstance();
            if (c.player != null && c.level != null) {
                ninjaTick++;
                int hurt = c.player.hurtTime;
                if (hurt > ninjaLastHurtTime) { detectNinjaHitDirection(c); ninjaHitAlpha = 1.0f; }
                ninjaLastHurtTime = hurt;
                ninjaHitAlpha   = Math.max(0f, ninjaHitAlpha   - 0.04f);
                ninjaBlindAlpha = Math.max(0f, ninjaBlindAlpha - 0.05f);
                if (hasNinjaBlindSpot(c)) ninjaBlindAlpha = Math.min(1.0f, ninjaBlindAlpha + 0.15f);
                ninjaBackstabAngle = checkNinjaBackstab(c);
                if (Config.INSTANCE.particlesEnabled && c.player.isSprinting() && ninjaTick % 3 == 0) {
                    double yawRad = Math.toRadians(c.player.getYRot());
                    double bkX = Math.sin(yawRad), bkZ = -Math.cos(yawRad);
                    c.level.addParticle(ParticleTypes.ASH,
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

    private void handleKeys(Minecraft client) {
        while (KEY_POWER_WHEEL.consumeClick()) if (client.gui.screen() == null) client.gui.setScreen(new PowerWheelScreen());
        while (KEY_SETTINGS.consumeClick())   if (client.gui.screen() == null) client.gui.setScreen(new SettingsScreen());
        while (KEY_GUIDE.consumeClick())      if (client.gui.screen() == null) client.gui.setScreen(new GuideScreen());
    }

    private void onHudRender(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gui.screen() != null) return;

        int w = client.getWindow().getGuiScaledWidth();
        int h = client.getWindow().getGuiScaledHeight();
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
        int textW = client.font.width(power.displayName) + 16;
        RenderHelper.drawRoundedRect(context, x, y, textW, 16, 5, 0xCC101010);
        RenderHelper.drawRoundedRectOutline(context, x, y, textW, 16, 5, 1,
                0xAA000000 | (power.accentColor & 0x00FFFFFF));
        context.fill(x + 5, y + 6, x + 7, y + 10, power.accentColor);
        context.text(client.font, power.displayName, x + 10, y + 4, power.accentColor, false);
    }

    // ── LODESTAR ────────────────────────────────────────────────────────────────

    private void scanNearestOre(Minecraft client) {
        if (client.level == null || client.player == null) return;
        BlockPos origin = client.player.blockPosition();
        int range = 16;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        BlockPos bestPos = null; double bestDist = Double.MAX_VALUE; String bestName = "";
        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    double distSq = (double)(dx*dx + dy*dy + dz*dz);
                    if (distSq > range * range) continue;
                    mutable.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    String name = getOreName(client.level.getBlockState(mutable));
                    if (name != null && distSq < bestDist) { bestDist = distSq; bestPos = mutable.immutable(); bestName = name; }
                }
            }
        }
        lodestarlTarget   = bestPos;
        lodestarlOreName  = bestName;
        lodestarlOreColor = bestPos != null ? getOreColor(bestName) : 0xFF666666;
        lodestarlDist     = bestPos != null ? Math.sqrt(bestDist) : 0;
    }

    private static String getOreName(BlockState state) {
        if (state.getBlock() == Blocks.DIAMOND_ORE || state.getBlock() == Blocks.DEEPSLATE_DIAMOND_ORE)
            return "Diamond";
        if (state.getBlock() == Blocks.EMERALD_ORE || state.getBlock() == Blocks.DEEPSLATE_EMERALD_ORE)
            return "Emerald";
        if (state.getBlock() == Blocks.ANCIENT_DEBRIS)
            return "Debris";
        if (state.is(BlockTags.GOLD_ORES))    return "Gold";
        if (state.is(BlockTags.IRON_ORES))    return "Iron";
        if (state.is(BlockTags.COPPER_ORES))  return "Copper";
        if (state.getBlock() == Blocks.LAPIS_ORE || state.getBlock() == Blocks.DEEPSLATE_LAPIS_ORE)
            return "Lapis";
        if (state.getBlock() == Blocks.REDSTONE_ORE || state.getBlock() == Blocks.DEEPSLATE_REDSTONE_ORE)
            return "Redstone";
        if (state.getBlock() == Blocks.COAL_ORE || state.getBlock() == Blocks.DEEPSLATE_COAL_ORE)
            return "Coal";
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

    private void drawLodestarCompass(GuiGraphicsExtractor ctx, int w, int h, Minecraft client) {
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
            double rel = Math.atan2(dz, dx) - Math.toRadians(90.0 + client.player.getYRot());
            int nx = cx + (int)(Math.sin(rel) * (r - 5));
            int ny = cy - (int)(Math.cos(rel) * (r - 5));
            PowerHud.drawCompassLine(ctx, cx, cy, nx, ny, lodestarlOreColor);
            ctx.fill(nx - 1, ny - 1, nx + 2, ny + 2, lodestarlOreColor);
            ctx.centeredText(client.font,
                    lodestarlOreName + " " + (int)lodestarlDist + "m", cx, cy + r + 4, lodestarlOreColor);
        } else {
            ctx.centeredText(client.font, "—", cx, cy - 3, 0xFF444444);
            ctx.centeredText(client.font, "none nearby", cx, cy + r + 4, 0xFF444444);
        }
    }

    // ── VOID ─────────────────────────────────────────────────────────────────────

    private void scanNearestPortal(Minecraft client) {
        if (client.level == null || client.player == null) return;
        BlockPos origin = client.player.blockPosition();
        int range = 48;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        BlockPos bestPos = null; double bestDist = Double.MAX_VALUE; String bestType = "";
        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    double distSq = (double)(dx * dx + dy * dy + dz * dz);
                    if (distSq > range * range) continue;
                    mutable.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    String type = getPortalTypeName(client.level.getBlockState(mutable));
                    if (type != null && distSq < bestDist) { bestDist = distSq; bestPos = mutable.immutable(); bestType = type; }
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

    private void drawPortalFinder(GuiGraphicsExtractor ctx, int w, int h, Minecraft client) {
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
            double rel = Math.atan2(dz, dx) - Math.toRadians(90.0 + client.player.getYRot());
            int nx = cx + (int)(Math.sin(rel) * (r - 5));
            int ny = cy - (int)(Math.cos(rel) * (r - 5));
            PowerHud.drawCompassLine(ctx, cx, cy, nx, ny, portalTypeColor);
            ctx.fill(nx - 1, ny - 1, nx + 2, ny + 2, portalTypeColor);
            ctx.centeredText(client.font,
                    portalTypeName + " " + (int)portalDist + "m", cx, cy + r + 4, portalTypeColor);
        } else {
            ctx.centeredText(client.font, "—",         cx, cy - 3,    0xFF333355);
            ctx.centeredText(client.font, "no portal", cx, cy + r + 4, 0xFF444466);
        }
    }

    // ── NINJA ────────────────────────────────────────────────────────────────────

    private void drawNinjaHUD(GuiGraphicsExtractor ctx, int w, int h, Minecraft client) {
        int cx = w / 2, cy = h / 2;
        boolean critReady = !client.player.onGround()
                && client.player.getDeltaMovement().y < 0
                && !client.player.isInWater()
                && client.player.getVehicle() == null;
        if (critReady) {
            long t = System.currentTimeMillis();
            int alpha = (int)(90 * (0.55 + 0.45 * Math.abs(Math.sin(t / 140.0))));
            ctx.fill(cx - 6, cy - 2, cx + 5, cy + 1, (alpha << 24) | 0xFFFF88);
            ctx.fill(cx - 2, cy - 6, cx + 1, cy + 5, (alpha << 24) | 0xFFFF88);
        }
        if (ninjaBlindAlpha > 0.01f) {
            long t     = System.currentTimeMillis();
            int  alpha = (int)(ninjaBlindAlpha * 180 * (0.5 + 0.5 * Math.abs(Math.sin(t / 400.0))));
            drawNinjaArc(ctx, cx, cy, 22, Math.PI, Math.toRadians(70),
                    (alpha << 24) | (Power.NINJA.accentColor & 0x00FFFFFF));
        }
        if (!Float.isNaN(ninjaBackstabAngle)) {
            double rel   = Math.toRadians(ninjaBackstabAngle) - Math.toRadians(90.0 + client.player.getYRot());
            long   t     = System.currentTimeMillis();
            int    alpha = (int)(210 * (0.6 + 0.4 * Math.abs(Math.sin(t / 180.0))));
            drawNinjaArcArrow(ctx, cx, cy, 30, rel, Math.toRadians(38), (alpha << 24) | 0xFFCC44);
        }
        if (ninjaHitAlpha > 0.01f) {
            double rel   = Math.toRadians(ninjaHitAngle) - Math.toRadians(90.0 + client.player.getYRot());
            int    alpha = (int)(ninjaHitAlpha * ninjaHitAlpha * 220);
            drawNinjaArcArrow(ctx, cx, cy, 22, rel, Math.toRadians(50), (alpha << 24) | 0xFF2222);
        }
    }

    private void detectNinjaHitDirection(Minecraft client) {
        if (client.level == null || client.player == null) return;
        LivingEntity nearest = null; double minDist = Double.MAX_VALUE;
        for (LivingEntity e : client.level.getEntitiesOfClass(LivingEntity.class,
                client.player.getBoundingBox().inflate(10.0),
                en -> en != client.player && en.isAlive())) {
            double d = e.distanceToSqr(client.player);
            if (d < minDist) { minDist = d; nearest = e; }
        }
        if (nearest != null) {
            ninjaHitAngle = (float) Math.toDegrees(Math.atan2(
                    nearest.getZ() - client.player.getZ(),
                    nearest.getX() - client.player.getX()));
        }
    }

    private boolean hasNinjaBlindSpot(Minecraft client) {
        if (client.level == null || client.player == null) return false;
        double yawRad = Math.toRadians(client.player.getYRot());
        double fwdX = -Math.sin(yawRad), fwdZ = Math.cos(yawRad);
        for (LivingEntity e : client.level.getEntitiesOfClass(LivingEntity.class,
                client.player.getBoundingBox().inflate(8.0),
                en -> en != client.player && en.isAlive()
                        && (en instanceof Monster || en instanceof Player))) {
            double dx = e.getX() - client.player.getX(), dz = e.getZ() - client.player.getZ();
            double d  = Math.sqrt(dx * dx + dz * dz);
            if (d < 0.5) continue;
            if ((dx / d) * fwdX + (dz / d) * fwdZ < -0.34) return true;
        }
        return false;
    }

    private float checkNinjaBackstab(Minecraft client) {
        if (client.level == null || client.player == null) return Float.NaN;
        double yawRad = Math.toRadians(client.player.getYRot());
        double pFwdX = -Math.sin(yawRad), pFwdZ = Math.cos(yawRad);
        for (LivingEntity e : client.level.getEntitiesOfClass(LivingEntity.class,
                client.player.getBoundingBox().inflate(4.0),
                en -> en != client.player && en.isAlive())) {
            double dx = e.getX() - client.player.getX(), dz = e.getZ() - client.player.getZ();
            double d  = Math.sqrt(dx * dx + dz * dz);
            if (d < 0.5 || d > 3.5) continue;
            if ((dx / d) * pFwdX + (dz / d) * pFwdZ < 0.4) continue;
            double eYaw = Math.toRadians(e.getYRot());
            if ((-dx / d) * -Math.sin(eYaw) + (-dz / d) * Math.cos(eYaw) < -0.5)
                return (float) Math.toDegrees(Math.atan2(dz, dx));
        }
        return Float.NaN;
    }

    private static void drawNinjaArcArrow(GuiGraphicsExtractor ctx, int cx, int cy, int r,
                                           double centerAngle, double halfSpan, int color) {
        int aFull  = (color >>> 24) & 0xFF;
        int rgb    = color & 0x00FFFFFF;
        int aGlow  = aFull / 4;
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

    private static void drawNinjaArc(GuiGraphicsExtractor ctx, int cx, int cy, int r,
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

    private static void drawNinjaLine(GuiGraphicsExtractor ctx, int x1, int y1, int x2, int y2, int color) {
        int dx = x2 - x1, dy = y2 - y1;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps == 0) { ctx.fill(x1, y1, x1 + 2, y1 + 2, color); return; }
        for (int i = 0; i <= steps; i++) {
            int px = x1 + dx * i / steps, py = y1 + dy * i / steps;
            ctx.fill(px, py, px + 2, py + 2, color);
        }
    }
}
