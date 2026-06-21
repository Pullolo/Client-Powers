package net.pullolo.clientpowers.module;

import net.pullolo.clientpowers.config.Config;

public class DynamicLightModule implements Module {
    public static final DynamicLightModule INSTANCE = new DynamicLightModule();

    private boolean enabled;
    private int radius = 8;

    @Override
    public String getName() { return "Dynamic Light"; }

    @Override
    public boolean isEnabled() { return enabled; }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        Config.INSTANCE.dynamicLightEnabled = enabled;
        Config.INSTANCE.save();
    }

    public int getRadius() { return radius; }

    public void setRadius(int radius) {
        this.radius = Math.max(1, Math.min(16, radius));
        Config.INSTANCE.dynamicLightRadius = this.radius;
        Config.INSTANCE.save();
    }

    public void loadFromConfig() {
        this.enabled = Config.INSTANCE.dynamicLightEnabled;
        this.radius = Config.INSTANCE.dynamicLightRadius;
    }
}
