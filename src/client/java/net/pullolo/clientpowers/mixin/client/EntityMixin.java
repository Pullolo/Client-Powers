package net.pullolo.clientpowers.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.pullolo.clientpowers.module.PlayerGlowModule;
import net.pullolo.clientpowers.power.Power;
import net.pullolo.clientpowers.power.PowerManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {

    @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true)
    private void onIsGlowing(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity)(Object)this;
        if (!(self.getEntityWorld() instanceof ClientWorld)) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || self == client.player) return;

        Vec3d playerPos = new Vec3d(client.player.getX(), client.player.getY(), client.player.getZ());
        Vec3d entityPos = new Vec3d(self.getX(), self.getY(), self.getZ());
        double distSq   = playerPos.squaredDistanceTo(entityPos);

        Power power = PowerManager.INSTANCE.getActivePower();

        // VOID — Void Sense: all nearby entities glow through walls (24 block range)
        if (power == Power.VOID && distSq <= 24.0 * 24.0) {
            cir.setReturnValue(true);
            return;
        }

        // NATURE — Terra Bond: passive animals glow within 20 blocks
        if (power == Power.NATURE && self instanceof AnimalEntity && distSq <= 20.0 * 20.0) {
            cir.setReturnValue(true);
            return;
        }

        // SHADOW — Penumbra: all nearby non-player entities glow within 12 blocks
        if (power == Power.SHADOW && !(self instanceof PlayerEntity) && distSq <= 12.0 * 12.0) {
            cir.setReturnValue(true);
            return;
        }

        // BLOOD — Crimson Pulse: hostile mobs glow within 18 blocks
        if (power == Power.BLOOD && self instanceof HostileEntity && distSq <= 18.0 * 18.0) {
            cir.setReturnValue(true);
            return;
        }

        // NINJA — Silent Strike: other players glow within 20 blocks
        if (power == Power.NINJA && self instanceof PlayerEntity && distSq <= 20.0 * 20.0) {
            cir.setReturnValue(true);
            return;
        }

        // Player Glow module: other players within configured range glow
        if (self instanceof PlayerEntity && PlayerGlowModule.INSTANCE.isEnabled()) {
            float range = PlayerGlowModule.INSTANCE.getRange();
            if (distSq <= (double) range * range) {
                cir.setReturnValue(true);
            }
        }
    }
}
