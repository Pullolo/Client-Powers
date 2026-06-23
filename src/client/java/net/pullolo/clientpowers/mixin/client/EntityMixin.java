package net.pullolo.clientpowers.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.pullolo.clientpowers.module.PlayerGlowModule;
import net.pullolo.clientpowers.power.Power;
import net.pullolo.clientpowers.power.PowerManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {

    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void onIsGlowing(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity)(Object)this;
        if (!(self.level() instanceof ClientLevel)) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || self == client.player) return;

        Vec3 playerPos = new Vec3(client.player.getX(), client.player.getY(), client.player.getZ());
        Vec3 entityPos = new Vec3(self.getX(), self.getY(), self.getZ());
        double distSq   = playerPos.distanceToSqr(entityPos);

        Power power = PowerManager.INSTANCE.getActivePower();

        if (power == Power.VOID && distSq <= 24.0 * 24.0) {
            cir.setReturnValue(true);
            return;
        }

        if (power == Power.NATURE && self instanceof Animal && distSq <= 20.0 * 20.0) {
            cir.setReturnValue(true);
            return;
        }

        if (power == Power.SHADOW && !(self instanceof Player) && distSq <= 12.0 * 12.0) {
            cir.setReturnValue(true);
            return;
        }

        if (power == Power.BLOOD && self instanceof Monster && distSq <= 18.0 * 18.0) {
            cir.setReturnValue(true);
            return;
        }

        if (power == Power.NINJA && self instanceof Player && distSq <= 20.0 * 20.0) {
            cir.setReturnValue(true);
            return;
        }

        if (self instanceof Player && PlayerGlowModule.INSTANCE.isEnabled()) {
            float range = PlayerGlowModule.INSTANCE.getRange();
            if (distSq <= (double) range * range) {
                cir.setReturnValue(true);
            }
        }
    }
}
