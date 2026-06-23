package net.pullolo.clientpowers.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.particles.ParticleOptions;
import net.pullolo.clientpowers.power.Power;
import net.pullolo.clientpowers.power.PowerManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(Player.class)
public class PlayerEntityMixin {

    private static final Random RANDOM = new Random();

    @Inject(method = "attack", at = @At("HEAD"))
    private void onAttack(Entity target, CallbackInfo ci) {
        Player self = (Player)(Object)this;
        if (!(self.level() instanceof ClientLevel)) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        Power power = PowerManager.INSTANCE.getActivePower();
        ParticleOptions hitParticle = power.getHitParticle();
        if (hitParticle == null) return;

        double x = target.getX();
        double y = target.getY() + target.getBbHeight() * 0.6;
        double z = target.getZ();

        for (int i = 0; i < 8; i++) {
            double ox = (RANDOM.nextDouble() - 0.5) * 0.6;
            double oy = (RANDOM.nextDouble() - 0.5) * 0.6;
            double oz = (RANDOM.nextDouble() - 0.5) * 0.6;
            double vx = (RANDOM.nextDouble() - 0.5) * 0.3;
            double vy = RANDOM.nextDouble() * 0.2;
            double vz = (RANDOM.nextDouble() - 0.5) * 0.3;
            client.level.addParticle(hitParticle, x + ox, y + oy, z + oz, vx, vy, vz);
        }
    }
}
