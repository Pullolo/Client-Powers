package net.pullolo.clientpowers.cosmetic;

import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.ParticleEffect;
import net.pullolo.clientpowers.config.Config;
import net.pullolo.clientpowers.power.Power;
import net.pullolo.clientpowers.power.PowerManager;

import java.util.Random;

public class WingsRenderer {
    private static final Random RANDOM = new Random();
    private double flapAngle = 0.0;
    private int    tick      = 0;

    private static final float[][] WING_SHAPE = {
        {0.30f, 1.50f},
        {0.55f, 1.65f},
        {0.80f, 1.55f},
        {1.00f, 1.35f},
        {0.90f, 1.15f},
        {0.65f, 1.05f},
        {0.35f, 1.00f},
    };

    public void tick(MinecraftClient client, boolean moving) {
        if (client.player == null || client.world == null) return;
        if (!shouldRender()) return;

        // Always advance so flap phase is continuous when player stops
        tick++;
        flapAngle += 0.10;

        if (moving) return;
        if (tick % 2 != 0) return;

        Power power = PowerManager.INSTANCE.getActivePower();
        ParticleEffect particle = power.getBodyParticle();
        if (particle == null) return;

        double px = client.player.getX();
        double py = client.player.getY();
        double pz = client.player.getZ();

        float cosYaw = (float) Math.cos(Math.toRadians(client.player.getYaw()));
        float sinYaw = (float) Math.sin(Math.toRadians(client.player.getYaw()));

        double flapOffset = Math.sin(flapAngle) * 0.25;

        for (float[] point : WING_SHAPE) {
            double side   = point[0];
            double height = point[1] + flapOffset * (side / 0.5);
            spawnWingParticle(client, particle, px, py, pz, cosYaw, sinYaw,  side, height);
            spawnWingParticle(client, particle, px, py, pz, cosYaw, sinYaw, -side, height);
        }
    }

    private void spawnWingParticle(MinecraftClient client, ParticleEffect particle,
                                    double px, double py, double pz,
                                    float cosYaw, float sinYaw,
                                    double side, double height) {
        double ox = (RANDOM.nextDouble() - 0.5) * 0.05;
        double oz = (RANDOM.nextDouble() - 0.5) * 0.05;
        double wx = side * cosYaw + ox;
        double wz = side * sinYaw + oz;
        client.world.addParticleClient(particle, px + wx, py + height, pz + wz - 0.1, 0.0, 0.003, 0.0);
    }

    public boolean shouldRender() {
        Power power = PowerManager.INSTANCE.getActivePower();
        if (power == Power.NONE) return false;
        return Config.INSTANCE.wingsEnabled;
    }
}
