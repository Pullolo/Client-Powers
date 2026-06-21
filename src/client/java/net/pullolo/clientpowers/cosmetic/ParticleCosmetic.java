package net.pullolo.clientpowers.cosmetic;

import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.ParticleEffect;
import net.pullolo.clientpowers.config.Config;
import net.pullolo.clientpowers.power.Power;
import net.pullolo.clientpowers.power.PowerManager;

import java.util.Random;

public class ParticleCosmetic {
    private static final Random RANDOM = new Random();
    private int tick = 0;

    public void tick(MinecraftClient client, boolean moving) {
        if (client.player == null || client.world == null) return;
        if (!Config.INSTANCE.particlesEnabled) return;
        if (moving) return;

        Power power = PowerManager.INSTANCE.getActivePower();
        ParticleEffect body = power.getBodyParticle();
        ParticleEffect env  = power.getEnvironmentalParticle();
        if (body == null) return;

        tick++;

        if (tick % 2 == 0) {
            spawnBodyParticles(client, power, body);
        }
        if (tick % 10 == 0 && env != null) {
            spawnEnvironmentalParticles(client, env);
        }
    }

    private void spawnBodyParticles(MinecraftClient client, Power power, ParticleEffect particle) {
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
            client.world.addParticleClient(particle, px + ox, py + oy, pz + oz, vx, vy, vz);
        }
    }

    private void spawnEnvironmentalParticles(MinecraftClient client, ParticleEffect particle) {
        double px = client.player.getX();
        double py = client.player.getY();
        double pz = client.player.getZ();

        for (int i = 0; i < 2; i++) {
            double ox = (RANDOM.nextDouble() - 0.5) * 8.0;
            double oy = RANDOM.nextDouble() * 3.0;
            double oz = (RANDOM.nextDouble() - 0.5) * 8.0;
            client.world.addParticleClient(particle, px + ox, py + oy, pz + oz, 0, 0.01, 0);
        }
    }
}
