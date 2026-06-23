package net.pullolo.clientpowers.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.pullolo.clientpowers.config.Config;
import net.pullolo.clientpowers.module.*;
import net.pullolo.clientpowers.power.Power;
import net.pullolo.clientpowers.power.PowerManager;

import java.util.ArrayList;
import java.util.List;

public class SettingsScreen extends Screen {

    private static final int PANEL_W = 300;
    private static final int ROW_H = 22;
    private static final int SECTION_H = 18;
    private static final int PADDING = 12;
    private static final int COLOR_BG = 0xF0101010;
    private static final int COLOR_TEXT = 0xFFDDDDDD;
    private static final int COLOR_DIM = 0xFF888888;

    private int panelX, panelY, panelH;

    private final List<int[]> clickAreas = new ArrayList<>();

    private static final int ID_PARTICLES = 0;
    private static final int ID_AURA = 1;
    private static final int ID_TRAIL = 2;
    private static final int ID_WINGS = 3;
    private static final int ID_DYNLIGHT = 4;
    private static final int ID_DYNLIGHT_MINUS = 5;
    private static final int ID_DYNLIGHT_PLUS = 6;
    private static final int ID_GLOW = 7;
    private static final int ID_GLOW_MINUS = 8;
    private static final int ID_GLOW_PLUS = 9;
    private static final int ID_SPRINT = 10;

    public SettingsScreen() {
        super(Component.literal("ClientPowers Settings"));
    }

    @Override
    protected void init() {
        clickAreas.clear();
        panelH = computePanelHeight();
        panelX = (width - PANEL_W) / 2;
        panelY = Math.max(4, (height - panelH) / 2);
    }

    private int computePanelHeight() {
        return PADDING + 20 + PADDING + SECTION_H + 4 * ROW_H
                + PADDING + SECTION_H + 3 * ROW_H
                + PADDING + 4 + PADDING;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0x88000000);

        Power activePower = PowerManager.INSTANCE.getActivePower();
        int accent = activePower.accentColor;

        RenderHelper.drawRoundedRect(context, panelX, panelY, PANEL_W, panelH, 10, COLOR_BG);
        RenderHelper.drawRoundedRect(context, panelX, panelY, PANEL_W, 4, 2, accent);

        context.centeredText(font, "ClientPowers",
                panelX + PANEL_W / 2, panelY + PADDING + 2, accent);

        int sepY = panelY + PADDING + 16;
        context.fill(panelX + PADDING, sepY, panelX + PANEL_W - PADDING, sepY + 1, 0xFF2A2A2A);

        clickAreas.clear();
        int y = sepY + PADDING;
        int rowW = PANEL_W - 2 * PADDING;

        drawSectionHeader(context, "Cosmetics", panelX + PADDING, y, accent);
        y += SECTION_H;
        y = drawToggleRow(context, "Particles",     Config.INSTANCE.particlesEnabled, panelX + PADDING, y, rowW, accent, ID_PARTICLES);
        y = drawToggleRow(context, "Aura",          Config.INSTANCE.auraEnabled,      panelX + PADDING, y, rowW, accent, ID_AURA);
        y = drawToggleRow(context, "Trail",         Config.INSTANCE.trailEnabled,     panelX + PADDING, y, rowW, accent, ID_TRAIL);
        y = drawToggleRow(context, "Wings",         Config.INSTANCE.wingsEnabled,     panelX + PADDING, y, rowW, accent, ID_WINGS);

        y += PADDING;

        drawSectionHeader(context, "Modules", panelX + PADDING, y, accent);
        y += SECTION_H;
        y = drawToggleRowWithValue(context, "Dynamic Light", Config.INSTANCE.dynamicLightEnabled,
                "r: " + Config.INSTANCE.dynamicLightRadius,
                panelX + PADDING, y, rowW, accent, ID_DYNLIGHT, ID_DYNLIGHT_MINUS, ID_DYNLIGHT_PLUS);
        y = drawToggleRowWithValue(context, "Player Glow", Config.INSTANCE.playerGlowEnabled,
                (int) Config.INSTANCE.playerGlowRange + "m",
                panelX + PADDING, y, rowW, accent, ID_GLOW, ID_GLOW_MINUS, ID_GLOW_PLUS);
        drawToggleRow(context, "Toggle Sprint", Config.INSTANCE.toggleSprintEnabled,
                panelX + PADDING, y, rowW, accent, ID_SPRINT);

        context.centeredText(font, "Press Esc to close",
                panelX + PANEL_W / 2, panelY + panelH - PADDING - 6, COLOR_DIM);
    }

    private void drawSectionHeader(GuiGraphicsExtractor ctx, String title, int x, int y, int accent) {
        ctx.fill(x, y + SECTION_H / 2, x + PANEL_W - 2 * PADDING, y + SECTION_H / 2 + 1, 0xFF2A2A2A);
        RenderHelper.drawRoundedRect(ctx, x, y + 3, font.width(title) + 12, SECTION_H - 6, 4, 0xFF1A1A1A);
        ctx.text(font, title, x + 6, y + 5, accent, false);
    }

    private int drawToggleRow(GuiGraphicsExtractor ctx, String label, boolean on, int x, int y, int w, int accent, int id) {
        ctx.text(font, label, x + 4, y + (ROW_H - 8) / 2, COLOR_TEXT, false);
        int toggleX = x + w - 34;
        int toggleY = y + (ROW_H - 14) / 2;
        RenderHelper.drawToggle(ctx, toggleX, toggleY, on, accent);
        clickAreas.add(new int[]{toggleX, toggleY, 30, 14, id});
        return y + ROW_H;
    }

    private int drawToggleRowWithValue(GuiGraphicsExtractor ctx, String label, boolean on, String value,
                                        int x, int y, int w, int accent,
                                        int toggleId, int minusId, int plusId) {
        ctx.text(font, label, x + 4, y + (ROW_H - 8) / 2, COLOR_TEXT, false);

        int toggleX = x + w - 34;
        int toggleY = y + (ROW_H - 14) / 2;
        RenderHelper.drawToggle(ctx, toggleX, toggleY, on, accent);
        clickAreas.add(new int[]{toggleX, toggleY, 30, 14, toggleId});

        int plusX  = toggleX - 20;
        int minusX = toggleX - 88;
        int btnY   = y + (ROW_H - 12) / 2;

        RenderHelper.drawRoundedRect(ctx, minusX, btnY, 14, 12, 3, 0xFF2A2A2A);
        ctx.centeredText(font, "-", minusX + 7, btnY + 2, COLOR_DIM);
        clickAreas.add(new int[]{minusX, btnY, 14, 12, minusId});

        ctx.centeredText(font, value, (minusX + 14 + plusX) / 2, y + (ROW_H - 8) / 2, COLOR_DIM);

        RenderHelper.drawRoundedRect(ctx, plusX, btnY, 14, 12, 3, 0xFF2A2A2A);
        ctx.centeredText(font, "+", plusX + 7, btnY + 2, COLOR_DIM);
        clickAreas.add(new int[]{plusX, btnY, 14, 12, plusId});

        return y + ROW_H;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        int mx = (int) event.x(), my = (int) event.y();
        for (int[] area : clickAreas) {
            if (mx >= area[0] && mx <= area[0] + area[2] && my >= area[1] && my <= area[1] + area[3]) {
                handleAction(area[4]);
                return true;
            }
        }
        return super.mouseClicked(event, consumed);
    }

    private void handleAction(int id) {
        Config cfg = Config.INSTANCE;
        switch (id) {
            case ID_PARTICLES    -> { cfg.particlesEnabled = !cfg.particlesEnabled; cfg.save(); }
            case ID_AURA         -> { cfg.auraEnabled = !cfg.auraEnabled; cfg.save(); }
            case ID_TRAIL        -> { cfg.trailEnabled = !cfg.trailEnabled; cfg.save(); }
            case ID_WINGS        -> { cfg.wingsEnabled = !cfg.wingsEnabled; cfg.save(); }
            case ID_DYNLIGHT     -> DynamicLightModule.INSTANCE.setEnabled(!DynamicLightModule.INSTANCE.isEnabled());
            case ID_DYNLIGHT_MINUS -> DynamicLightModule.INSTANCE.setRadius(DynamicLightModule.INSTANCE.getRadius() - 1);
            case ID_DYNLIGHT_PLUS  -> DynamicLightModule.INSTANCE.setRadius(DynamicLightModule.INSTANCE.getRadius() + 1);
            case ID_GLOW         -> PlayerGlowModule.INSTANCE.setEnabled(!PlayerGlowModule.INSTANCE.isEnabled());
            case ID_GLOW_MINUS   -> PlayerGlowModule.INSTANCE.setRange(Math.max(4f,   PlayerGlowModule.INSTANCE.getRange() - 4f));
            case ID_GLOW_PLUS    -> PlayerGlowModule.INSTANCE.setRange(Math.min(128f, PlayerGlowModule.INSTANCE.getRange() + 4f));
            case ID_SPRINT       -> ToggleSprintModule.INSTANCE.setEnabled(!ToggleSprintModule.INSTANCE.isEnabled());
        }
    }

    @Override
    public boolean isPauseScreen() { return true; }
}
