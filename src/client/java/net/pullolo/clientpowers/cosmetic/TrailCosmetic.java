package net.pullolo.clientpowers.cosmetic;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;
import net.pullolo.clientpowers.config.Config;
import net.pullolo.clientpowers.power.Power;
import net.pullolo.clientpowers.power.PowerManager;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

public class TrailCosmetic {
    private static final int TRAIL_LENGTH = 12;
    private static final Random RANDOM = new Random();
    private final Deque<Vec3> positions = new ArrayDeque<>();

    public void tick(Minecraft client, boolean moving) {
        if (client.player == null || client.level == null) return;
        if (!Config.INSTANCE.trailEnabled) return;

        Power power = PowerManager.INSTANCE.getActivePower();
        ParticleOptions particle = power.getBodyParticle();
        if (particle == null) return;

        if (!moving) {
            positions.clear();
            return;
        }

        Vec3 current = new Vec3(client.player.getX(), client.player.getY(), client.player.getZ());
        positions.addFirst(current);
        while (positions.size() > TRAIL_LENGTH) positions.removeLast();

        if (positions.size() < 2) return;

        Vec3 prev = null;
        int   i    = 0;
        for (Vec3 pos : positions) {
            if (prev != null && RANDOM.nextFloat() < 0.4f) {
                double ox   = (RANDOM.nextDouble() - 0.5) * 0.2;
                double oz   = (RANDOM.nextDouble() - 0.5) * 0.2;
                double fade = 1.0 - (double) i / TRAIL_LENGTH;
                if (RANDOM.nextDouble() < fade) {
                    client.level.addParticle(particle,
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
