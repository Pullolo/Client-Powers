package net.pullolo.clientpowers.cosmetic;

import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.util.math.Vec3d;
import net.pullolo.clientpowers.config.Config;
import net.pullolo.clientpowers.power.Power;
import net.pullolo.clientpowers.power.PowerManager;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

public class TrailCosmetic {
    private static final int TRAIL_LENGTH = 12;
    private static final Random RANDOM = new Random();
    private final Deque<Vec3d> positions = new ArrayDeque<>();

    public void tick(MinecraftClient client, boolean moving) {
        if (client.player == null || client.world == null) return;
        if (!Config.INSTANCE.trailEnabled) return;

        Power power = PowerManager.INSTANCE.getActivePower();
        ParticleEffect particle = power.getBodyParticle();
        if (particle == null) return;

        if (!moving) {
            positions.clear();
            return;
        }

        Vec3d current = new Vec3d(client.player.getX(), client.player.getY(), client.player.getZ());
        positions.addFirst(current);
        while (positions.size() > TRAIL_LENGTH) positions.removeLast();

        if (positions.size() < 2) return;

        Vec3d prev = null;
        int   i    = 0;
        for (Vec3d pos : positions) {
            if (prev != null && RANDOM.nextFloat() < 0.4f) {
                double ox   = (RANDOM.nextDouble() - 0.5) * 0.2;
                double oz   = (RANDOM.nextDouble() - 0.5) * 0.2;
                double fade = 1.0 - (double) i / TRAIL_LENGTH;
                if (RANDOM.nextDouble() < fade) {
                    client.world.addParticleClient(particle,
                            pos.x + ox, pos.y + 0.3, pos.z + oz, 0.0, 0.005, 0.0);
                }
            }
            prev = pos;
            i++;
        }
    }

    public void reset() {
        positions.clear();
    }
}
