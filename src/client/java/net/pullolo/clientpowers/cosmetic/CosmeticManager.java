package net.pullolo.clientpowers.cosmetic;

import net.minecraft.client.Minecraft;

public class CosmeticManager {
    public static final CosmeticManager INSTANCE = new CosmeticManager();

    private final ParticleCosmetic particles = new ParticleCosmetic();
    private final AuraCosmetic     aura      = new AuraCosmetic();
    private final TrailCosmetic    trail     = new TrailCosmetic();
    private final WingsRenderer    wings     = new WingsRenderer();

    public void tick(Minecraft client) {
        boolean moving = isMoving(client);
        particles.tick(client, moving);
        aura.tick(client, moving);
        trail.tick(client, moving);
        wings.tick(client, moving);
    }

    public void onPowerChanged() {
        trail.reset();
    }

    private static boolean isMoving(Minecraft client) {
        if (client.player == null) return false;
        return client.options.keyUp.isDown()
            || client.options.keyDown.isDown()
            || client.options.keyLeft.isDown()
            || client.options.keyRight.isDown();
    }
}
