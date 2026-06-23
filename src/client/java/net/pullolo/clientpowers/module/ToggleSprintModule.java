package net.pullolo.clientpowers.module;

import net.minecraft.client.Minecraft;
import net.pullolo.clientpowers.config.Config;

public class ToggleSprintModule implements Module {
    public static final ToggleSprintModule INSTANCE = new ToggleSprintModule();

    private boolean enabled = true;

    @Override
    public String getName() { return "Toggle Sprint"; }

    @Override
    public boolean isEnabled() { return enabled; }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        Config.INSTANCE.toggleSprintEnabled = enabled;
        Config.INSTANCE.save();
    }

    @Override
    public void tick() {
        if (!enabled) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        if (client.options.keyUp.isDown() && !client.player.isSprinting()
                && !client.player.isShiftKeyDown() && client.player.onGround()) {
            client.player.setSprinting(true);
        }
    }

    public void loadFromConfig() {
        this.enabled = Config.INSTANCE.toggleSprintEnabled;
    }
}
