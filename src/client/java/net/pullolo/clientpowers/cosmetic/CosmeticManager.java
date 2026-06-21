package net.pullolo.clientpowers.cosmetic;

import net.minecraft.client.MinecraftClient;

public class CosmeticManager {
    public static final CosmeticManager INSTANCE = new CosmeticManager();

    private final ParticleCosmetic particles = new ParticleCosmetic();
    private final AuraCosmetic     aura      = new AuraCosmetic();
    private final TrailCosmetic    trail     = new TrailCosmetic();
    private final WingsRenderer    wings     = new WingsRenderer();

    public void tick(MinecraftClient client) {
        boolean moving = isMoving(client);
        particles.tick(client, moving);
        aura.tick(client, moving);
        trail.tick(client, moving);
        wings.tick(client, moving);
    }

    public void onPowerChanged() {
        trail.reset();
    }

    private static boolean isMoving(MinecraftClient client) {
        if (client.player == null) return false;
        return client.options.forwardKey.isPressed()
            || client.options.backKey.isPressed()
            || client.options.leftKey.isPressed()
            || client.options.rightKey.isPressed();
    }
}
