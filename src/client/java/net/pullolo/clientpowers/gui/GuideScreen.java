package net.pullolo.clientpowers.gui;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.pullolo.clientpowers.power.Power;
import net.pullolo.clientpowers.power.PowerManager;

public class GuideScreen extends Screen {

    private static final int PANEL_W   = 400;
    private static final int PANEL_H   = 290;
    private static final int SIDEBAR_W = 120;
    private static final int PADDING   = 10;
    private static final int ROW_H     = 19;

    private static final Power[] POWERS = {
        Power.FLAME, Power.FROST, Power.THUNDER, Power.VOID,
        Power.STARGAZER, Power.NATURE, Power.SHADOW, Power.OCEAN,
        Power.BLOOD, Power.WIND, Power.CRYSTAL, Power.LODESTAR
    };

    private Power selected     = Power.FLAME;
    private int   hoveredIndex = -1;
    private int   panelX, panelY, sbX, contentY;

    public GuideScreen() {
        super(Text.literal("Power Guide"));
    }

    @Override
    protected void init() {
        panelX   = Math.max(4, (width  - PANEL_W) / 2);
        panelY   = Math.max(4, (height - PANEL_H) / 2);
        sbX      = panelX + PADDING;
        contentY = panelY + 28; // below title (10+2) + separator (at +24) + gap (4)
        Power active = PowerManager.INSTANCE.getActivePower();
        if (active != Power.NONE) selected = active;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0xAA000000);

        int accent = selected.accentColor;
        int sbH    = POWERS.length * ROW_H; // 12 × 19 = 228
        int dtX    = sbX + SIDEBAR_W + 8;
        int dtW    = PANEL_W - PADDING - SIDEBAR_W - 8 - PADDING; // 252

        // Panel background + top accent bar
        RenderHelper.drawRoundedRect(ctx, panelX, panelY, PANEL_W, PANEL_H, 10, 0xF0101010);
        RenderHelper.drawRoundedRect(ctx, panelX, panelY, PANEL_W, 4, 2, accent);

        // Title
        ctx.drawCenteredTextWithShadow(textRenderer, "Power Guide",
                panelX + PANEL_W / 2, panelY + PADDING + 2, accent);

        // Title separator
        ctx.fill(panelX + PADDING, panelY + 24, panelX + PANEL_W - PADDING, panelY + 25, 0xFF252525);

        // Sidebar + detail
        hoveredIndex = -1;
        drawSidebar(ctx, mouseX, mouseY, sbH);
        drawDetail(ctx, dtX, contentY, dtW, sbH, selected);

        // Footer
        ctx.drawCenteredTextWithShadow(textRenderer, "Click a power  ·  Esc to close",
                panelX + PANEL_W / 2, panelY + PANEL_H - PADDING - 6, 0xFF444444);
    }

    private void drawSidebar(DrawContext ctx, int mx, int my, int sbH) {
        RenderHelper.drawRoundedRect(ctx, sbX - 2, contentY - 2, SIDEBAR_W + 4, sbH + 4, 6, 0xFF161616);

        for (int i = 0; i < POWERS.length; i++) {
            Power p   = POWERS[i];
            int   ry  = contentY + i * ROW_H;
            boolean hov = mx >= sbX && mx < sbX + SIDEBAR_W && my >= ry && my < ry + ROW_H;
            boolean sel = (p == selected);
            if (hov) hoveredIndex = i;

            if (sel) {
                RenderHelper.drawRoundedRect(ctx, sbX, ry, SIDEBAR_W, ROW_H - 1, 4,
                        0x55000000 | (p.accentColor & 0x00FFFFFF));
            } else if (hov) {
                RenderHelper.drawRoundedRect(ctx, sbX, ry, SIDEBAR_W, ROW_H - 1, 4, 0xFF1E1E1E);
            }

            // Accent dot
            ctx.fill(sbX + 6, ry + 7, sbX + 9, ry + 10,
                    sel ? p.accentColor : hov ? 0xFF888888 : 0xFF444444);

            // Name
            int nc = sel ? p.accentColor : hov ? 0xFFCCCCCC : 0xFF777777;
            ctx.drawTextWithShadow(textRenderer, p.displayName, sbX + 14, ry + 6, nc);
        }
    }

    private void drawDetail(DrawContext ctx, int x, int y, int w, int h, Power p) {
        RenderHelper.drawRoundedRect(ctx, x - 2, y - 2, w + 4, h + 4, 6, 0xFF161616);
        // Top tint strip in accent color
        RenderHelper.drawRoundedRect(ctx, x, y, w, 3, 2,
                0x60000000 | (p.accentColor & 0x00FFFFFF));

        int lx = x + 8;
        int ly = y + 10;

        // Power name + subtitle
        ctx.drawTextWithShadow(textRenderer, p.displayName, lx, ly, p.accentColor);
        ly += 12;
        ctx.drawTextWithShadow(textRenderer, getHint(p), lx, ly, 0xFF777777);
        ly += 12;

        // Separator
        ctx.fill(lx, ly, x + w - 8, ly + 1, 0xFF2A2A2A);
        ly += 8;

        // Description lines
        for (String line : getDesc(p)) {
            ctx.drawTextWithShadow(textRenderer, line, lx, ly, 0xFFAAAAAA);
            ly += 10;
        }

        // Effects section
        ly += 6;
        ctx.drawTextWithShadow(textRenderer, "Effects:", lx, ly, 0xFF666666);
        ly += 11;

        for (String feat : getEffects(p)) {
            ctx.fill(lx + 2, ly + 3, lx + 4, ly + 5, p.accentColor);
            ctx.drawTextWithShadow(textRenderer, feat, lx + 8, ly, 0xFF888888);
            ly += 10;
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean consumed) {
        if (click.button() == 0 && hoveredIndex >= 0) {
            selected = POWERS[hoveredIndex];
            return true;
        }
        return super.mouseClicked(click, consumed);
    }

    @Override public boolean shouldPause()      { return true; }
    @Override public boolean shouldCloseOnEsc() { return true; }

    // ── Static data ────────────────────────────────────────────────────────────

    private static String getHint(Power p) {
        return switch (p) {
            case FLAME     -> "Ember Vision";
            case FROST     -> "Cryo Sight";
            case THUNDER   -> "Storm Rush";
            case VOID      -> "Void Sense";
            case STARGAZER -> "Astral Sight";
            case NATURE    -> "Terra Bond";
            case SHADOW    -> "Penumbra";
            case OCEAN     -> "Tidal Surge";
            case BLOOD     -> "Crimson Pulse";
            case WIND      -> "Zephyr Rush";
            case CRYSTAL   -> "Prism Sight";
            case LODESTAR  -> "Shard Compass";
            default        -> "";
        };
    }

    private static String[] getDesc(Power p) {
        return switch (p) {
            case FLAME     -> new String[]{
                "Infuses you with fire energy.",
                "Flame particles orbit your body",
                "and embers trail as you move."
            };
            case FROST     -> new String[]{
                "Channels the cold into your form.",
                "Snowflakes drift at the edges of",
                "your vision while active."
            };
            case THUNDER   -> new String[]{
                "Harnesses raw electrical power.",
                "A discharge flashes the screen",
                "and speed lines burst on sprint."
            };
            case VOID      -> new String[]{
                "Taps into the energy of the void.",
                "Nearby entities reveal themselves",
                "and a compass tracks portal frames."
            };
            case STARGAZER -> new String[]{
                "Bonds you to the night sky.",
                "Stars fill the screen and a moon",
                "phase panel tracks the lunar cycle."
            };
            case NATURE    -> new String[]{
                "Connects you to the living world.",
                "Passive animals glow softly nearby",
                "and leaves drift down around you."
            };
            case SHADOW    -> new String[]{
                "Cloaks you in shadow energy.",
                "Creatures reveal themselves and",
                "vision sharpens in near-darkness."
            };
            case OCEAN     -> new String[]{
                "Grants the power of deep water.",
                "Your sight clears completely when",
                "submerged, as if born of the sea."
            };
            case BLOOD     -> new String[]{
                "Awakens predatory instincts.",
                "Hostile mobs glow crimson and a",
                "heartbeat pulses at screen edges."
            };
            case WIND      -> new String[]{
                "Channels the speed of open air.",
                "Wind streaks fill the screen and",
                "a weather forecast panel appears."
            };
            case CRYSTAL   -> new String[]{
                "Refracts light through crystal.",
                "Vision brightens and rainbow",
                "sparkles play at screen edges."
            };
            case LODESTAR  -> new String[]{
                "Attunes you to mineral veins.",
                "A compass points to the nearest",
                "ore of any type within 16 blocks."
            };
            default        -> new String[]{};
        };
    }

    private static String[] getEffects(Power p) {
        return switch (p) {
            case FLAME     -> new String[]{
                "+0.25 brightness at all light levels",
                "Fire particles when idle",
                "Flame trail when moving",
                "Default wings"
            };
            case FROST     -> new String[]{
                "Edge snowflake overlay",
                "Snowflake particles when idle"
            };
            case THUNDER   -> new String[]{
                "Screen flash every 5 seconds",
                "Speed lines while sprinting",
                "Electric particles when idle"
            };
            case VOID      -> new String[]{
                "Entity glow within 24 blocks",
                "Portal finder (48-block scan)",
                "Default wings"
            };
            case STARGAZER -> new String[]{
                "Starfield & shooting star overlay",
                "Moon phase HUD (top-right)",
                "Full-moon danger warning"
            };
            case NATURE    -> new String[]{
                "Animals glow within 20 blocks",
                "Falling leaf overlay"
            };
            case SHADOW    -> new String[]{
                "+0.40 brightness in near-darkness",
                "Entities glow within 12 blocks",
                "Edge wisps & shadow streaks",
                "Dim pulse every 7.5 seconds"
            };
            case OCEAN     -> new String[]{
                "+0.35 brightness when submerged",
                "Bubble overlay on screen"
            };
            case BLOOD     -> new String[]{
                "Hostile mobs glow within 18 blocks",
                "Heartbeat vignette pulse",
                "Blood drip overlay"
            };
            case WIND      -> new String[]{
                "Wind streak overlay",
                "Weather forecast HUD (top-center)"
            };
            case CRYSTAL   -> new String[]{
                "+0.15 brightness at all levels",
                "Rainbow prism edge sparkles"
            };
            case LODESTAR  -> new String[]{
                "Ore compass (bottom-right corner)",
                "Detects all ore types",
                "Real-time updates (100 ms)"
            };
            default        -> new String[]{};
        };
    }
}
