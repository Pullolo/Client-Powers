package net.pullolo.clientpowers.cosmetic;

import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.ParticleEffect;
import net.pullolo.clientpowers.config.Config;
import net.pullolo.clientpowers.power.Power;
import net.pullolo.clientpowers.power.PowerManager;

public class AuraCosmetic {
    private double angle = 0.0;
    private int    tick  = 0;

    public void tick(MinecraftClient client, boolean moving) {
        if (client.player == null || client.world == null) return;
        if (!Config.INSTANCE.auraEnabled) return;

        Power power = PowerManager.INSTANCE.getActivePower();
        ParticleEffect particle = power.getAuraParticle();
        if (particle == null) return;

        // Always advance so the ring resumes smoothly when the player stops
        tick++;
        angle += 0.15;

        if (moving) return;
        if (tick % 2 != 0) return;

        double px = client.player.getX();
        double py = client.player.getY() + 0.05;
        double pz = client.player.getZ();

        int    points = 8;
        double radius = 1.1;
        for (int i = 0; i < points; i++) {
            double a = angle + (2.0 * Math.PI / points) * i;
            client.world.addParticleClient(particle,
                    px + Math.cos(a) * radius, py, pz + Math.sin(a) * radius, 0, 0.01, 0);
        }

        // Inner counter-rotating ring
        for (int i = 0; i < 4; i++) {
            double a = -angle * 1.5 + (2.0 * Math.PI / 4) * i;
            double r = 0.55;
            client.world.addParticleClient(particle,
                    px + Math.cos(a) * r, py + 0.1, pz + Math.sin(a) * r, 0, 0.01, 0);
        }
    }
}
