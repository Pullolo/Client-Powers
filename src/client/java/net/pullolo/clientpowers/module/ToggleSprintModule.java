package net.pullolo.clientpowers.module;

import net.minecraft.client.MinecraftClient;
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
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        if (client.options.forwardKey.isPressed() && !client.player.isSprinting()
                && !client.player.isSneaking() && client.player.isOnGround()) {
            client.player.setSprinting(true);
        }
    }

    public void loadFromConfig() {
        this.enabled = Config.INSTANCE.toggleSprintEnabled;
    }
}
