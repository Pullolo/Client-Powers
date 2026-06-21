package net.pullolo.clientpowers.module;

import net.pullolo.clientpowers.config.Config;

public class PlayerGlowModule implements Module {
    public static final PlayerGlowModule INSTANCE = new PlayerGlowModule();

    private boolean enabled;
    private float range = 32.0f;

    @Override
    public String getName() { return "Player Glow"; }

    @Override
    public boolean isEnabled() { return enabled; }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        Config.INSTANCE.playerGlowEnabled = enabled;
        Config.INSTANCE.save();
    }

    public float getRange() { return range; }

    public void setRange(float range) {
        this.range = range;
        Config.INSTANCE.playerGlowRange = range;
        Config.INSTANCE.save();
    }

    public void loadFromConfig() {
        this.enabled = Config.INSTANCE.playerGlowEnabled;
        this.range = Config.INSTANCE.playerGlowRange;
    }
}
