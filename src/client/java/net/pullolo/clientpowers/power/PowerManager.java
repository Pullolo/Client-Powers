package net.pullolo.clientpowers.power;

import net.pullolo.clientpowers.config.Config;

public class PowerManager {
    public static final PowerManager INSTANCE = new PowerManager();

    private Power activePower = Power.NONE;

    public Power getActivePower() {
        return activePower;
    }

    public void setActivePower(Power power) {
        this.activePower = power;
        Config.INSTANCE.activePower = power.name();
        Config.INSTANCE.save();
    }

    public void loadFromConfig() {
        activePower = Power.fromName(Config.INSTANCE.activePower);
    }
}
