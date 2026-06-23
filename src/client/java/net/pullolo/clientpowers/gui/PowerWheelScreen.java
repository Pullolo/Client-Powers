package net.pullolo.clientpowers.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.pullolo.clientpowers.cosmetic.CosmeticManager;
import net.pullolo.clientpowers.power.Power;
import net.pullolo.clientpowers.power.PowerManager;
import org.lwjgl.glfw.GLFW;

public class PowerWheelScreen extends Screen {

    private static final int OUTER_RADIUS = 150;
    private static final int INNER_RADIUS  = 52;
    private static final int SEGMENT_W     = 60;
    private static final int SEGMENT_H     = 32;

    private static final Power[]  WHEEL_POWERS = {
            Power.FLAME, Power.THUNDER, Power.VOID, Power.FROST,
            Power.STARGAZER, Power.OCEAN, Power.SHADOW, Power.NINJA,
            Power.NATURE, Power.BLOOD, Power.WIND, Power.CRYSTAL, Power.LODESTAR
    };
    private static final double[] ANGLES = {
            90.00, 62.31, 34.62, 6.92, 339.23, 311.54, 283.85,
            256.15, 228.46, 200.77, 173.08, 145.38, 117.69
    };

    private long  openTimeMs   = 0;
    private Power hoveredPower = null;

    public PowerWheelScreen() {
        super(Component.literal("Power Wheel"));
    }

    @Override
    protected void init() {
        openTimeMs = System.currentTimeMillis();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        int cx = width / 2;
        int cy = height / 2;

        float openProgress = Math.min(1f, (System.currentTimeMillis() - openTimeMs) / 300f);
        float eased = smoothstep(openProgress);

        context.fill(0, 0, width, height, (int)(140 * eased) << 24);

        updateHover(mouseX, mouseY, cx, cy);

        RenderHelper.drawFilledCircle(context, cx, cy, INNER_RADIUS,
                ((int)(210 * eased) << 24) | 0x0D0D0D);

        Power activePower = PowerManager.INSTANCE.getActivePower();

        if (eased > 0.4f) {
            int textAlpha = (int)(255 * ((eased - 0.4f) / 0.6f));
            Power display  = hoveredPower != null ? hoveredPower : activePower;
            String name    = display == Power.NONE ? "No Power" : display.displayName;
            String hint    = display != Power.NONE ? getHint(display) : "";
            int accent     = display.accentColor;

            context.centeredText(font, name, cx,
                    cy - (hint.isEmpty() ? 4 : 7), RenderHelper.withAlpha(accent, textAlpha));
            if (!hint.isEmpty()) {
                context.centeredText(font, hint, cx, cy + 2,
                        RenderHelper.withAlpha(0xFFAAAAAA, textAlpha * 2 / 3));
            }

            String instruct = hoveredPower != null ? "Release R to select" : "Release R to clear";
            context.centeredText(font, instruct, cx,
                    cy + INNER_RADIUS + 7, RenderHelper.withAlpha(0xFF888888, textAlpha * 2 / 3));
        }

        if (hoveredPower != null && eased > 0.3f) {
            double rad     = Math.toRadians(getAngle(hoveredPower));
            int lineAlpha  = (int)(90 * eased);
            drawLine(context,
                    cx + (int)(Math.cos(rad) * INNER_RADIUS),
                    cy + (int)(-Math.sin(rad) * INNER_RADIUS),
                    cx + (int)(Math.cos(rad) * (OUTER_RADIUS - SEGMENT_W / 2f) * eased),
                    cy + (int)(-Math.sin(rad) * (OUTER_RADIUS - SEGMENT_W / 2f) * eased),
                    (lineAlpha << 24) | (hoveredPower.accentColor & 0x00FFFFFF));
        }

        for (int i = 0; i < WHEEL_POWERS.length; i++) {
            Power power = WHEEL_POWERS[i];
            double rad  = Math.toRadians(ANGLES[i]);
            float dist  = OUTER_RADIUS * eased;
            int px = cx + (int)(Math.cos(rad) * dist);
            int py = cy + (int)(-Math.sin(rad) * dist);
            drawSegment(context, px, py, power, power == hoveredPower, eased);
        }
    }

    private void drawSegment(GuiGraphicsExtractor ctx, int px, int py, Power power, boolean hovered, float alpha) {
        int x = px - SEGMENT_W / 2;
        int y = py - SEGMENT_H / 2;

        int bgAlpha  = (int)(alpha * (hovered ? 235 : 175));
        int bgColor  = hovered ? 0x1E1E1E : 0x111111;
        RenderHelper.drawRoundedRect(ctx, x, y, SEGMENT_W, SEGMENT_H, 7, (bgAlpha << 24) | bgColor);

        int borderAlpha = (int)(alpha * (hovered ? 220 : 65));
        int borderRGB   = hovered ? (power.accentColor & 0x00FFFFFF) : 0x444444;
        RenderHelper.drawRoundedRectOutline(ctx, x, y, SEGMENT_W, SEGMENT_H, 7, 1,
                (borderAlpha << 24) | borderRGB);

        int nameAlpha = (int)(alpha * 255);
        int nameColor = hovered ? power.accentColor : RenderHelper.withAlpha(0xFFCCCCCC, nameAlpha);
        ctx.centeredText(font, power.displayName, px, py - 5, nameColor);

        int subAlpha = (int)(alpha * (hovered ? 180 : 115));
        ctx.centeredText(font, getHint(power), px, py + 5,
                RenderHelper.withAlpha(hovered ? power.accentColor : 0xFF888888, subAlpha));
    }

    private String getHint(Power power) {
        return switch (power) {
            case FLAME      -> "ember vision";
            case FROST      -> "cryo sight";
            case THUNDER    -> "storm rush";
            case VOID       -> "void sense";
            case STARGAZER  -> "astral sight";
            case NATURE     -> "terra bond";
            case SHADOW     -> "penumbra";
            case OCEAN      -> "tidal surge";
            case BLOOD      -> "crimson pulse";
            case WIND       -> "zephyr rush";
            case CRYSTAL    -> "prism sight";
            case LODESTAR   -> "shard compass";
            case NINJA      -> "silent strike";
            default         -> "";
        };
    }

    private double getAngle(Power power) {
        for (int i = 0; i < WHEEL_POWERS.length; i++) {
            if (WHEEL_POWERS[i] == power) return ANGLES[i];
        }
        return 0;
    }

    private void updateHover(int mouseX, int mouseY, int cx, int cy) {
        double dx   = mouseX - cx;
        double dy   = cy - mouseY;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < INNER_RADIUS) { hoveredPower = null; return; }

        double angleDeg = Math.toDegrees(Math.atan2(dy, dx));
        if (angleDeg < 0) angleDeg += 360;

        double minDiff  = Double.MAX_VALUE;
        int    nearest  = 0;
        for (int i = 0; i < ANGLES.length; i++) {
            double diff = Math.abs(angleDeg - ANGLES[i]);
            if (diff > 180) diff = 360 - diff;
            if (diff < minDiff) { minDiff = diff; nearest = i; }
        }
        hoveredPower = WHEEL_POWERS[nearest];
    }

    private void drawLine(GuiGraphicsExtractor ctx, int x1, int y1, int x2, int y2, int color) {
        int dx = x2 - x1, dy = y2 - y1;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps == 0) return;
        float sx = (float) dx / steps, sy = (float) dy / steps;
        for (int i = 0; i < steps; i++) {
            ctx.fill(x1 + (int)(sx * i), y1 + (int)(sy * i),
                     x1 + (int)(sx * i) + 1, y1 + (int)(sy * i) + 1, color);
        }
    }

    private static float smoothstep(float t) {
        return t * t * (3f - 2f * t);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_R) { applySelection(); return true; }
        return super.keyReleased(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        if (event.buttonInfo().button() == 0 && hoveredPower != null) { applySelection(); return true; }
        return super.mouseClicked(event, consumed);
    }

    private void applySelection() {
        PowerManager.INSTANCE.setActivePower(hoveredPower != null ? hoveredPower : Power.NONE);
        CosmeticManager.INSTANCE.onPowerChanged();
        onClose();
    }

    @Override public boolean isPauseScreen()      { return false; }
    @Override public boolean shouldCloseOnEsc() { return true; }
}
