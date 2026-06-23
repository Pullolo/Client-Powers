package net.pullolo.clientpowers.cosmetic;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.pullolo.clientpowers.config.Config;
import net.pullolo.clientpowers.power.Power;
import net.pullolo.clientpowers.power.PowerManager;

import java.util.Random;

public class ParticleCosmetic {
    private static final Random RANDOM = new Random();
    private int tick = 0;

    public void tick(Minecraft client, boolean moving) {
        if (client.player == null || client.level == null) return;
        if (!Config.INSTANCE.particlesEnabled) return;
        if (moving) return;

        Power power = PowerManager.INSTANCE.getActivePower();
        ParticleOptions body = power.getBodyParticle();
        ParticleOptions env  = power.getEnvironmentalParticle();
        if (body == null) return;

        tick++;

        if (tick % 2 == 0) {
            spawnBodyParticles(client, power, body);
        }
        if (tick % 10 == 0 && env != null) {
            spawnEnvironmentalParticles(client, env);
        }
    }

    private void spawnBodyParticles(Minecraft client, Power power, ParticleOptions particle) {
        double px = client.player.getX();
        double py = client.player.getY();
        double pz = client.player.getZ();

        for (int i = 0; i < 3; i++) {
            double ox = (RANDOM.nextDouble() - 0.5) * 0.6;
            double oy = RANDOM.nextDouble() * 1.8;
            double oz = (RANDOM.nextDouble() - 0.5) * 0.6;
            double vx = (RANDOM.nextDouble() - 0.5) * 0.02;
            double vy = power == Power.VOID ? (RANDOM.nextDouble() - 0.5) * 0.02 : 0.02 + RANDOM.nextDouble() * 0.02;
            double vz = (RANDOM.nextDouble() - 0.5) * 0.02;
            client.level.addParticle(particle, px + ox, py + oy, pz + oz, vx, vy, vz);
        }
    }

    private void spawnEnvironmentalParticles(Minecraft client, ParticleOptions particle) {
        double px = client.player.getX();
        double py = client.player.getY();
        double pz = client.player.getZ();

        for (int i = 0; i < 2; i++) {
            double ox = (RANDOM.nextDouble() - 0.5) * 8.0;
            double oy = RANDOM.nextDouble() * 3.0;
            double oz = (RANDOM.nextDouble() - 0.5) * 8.0;
            client.level.addParticle(particle, px + ox, py + oy, pz + oz, 0, 0.01, 0);
        }
    }
}
