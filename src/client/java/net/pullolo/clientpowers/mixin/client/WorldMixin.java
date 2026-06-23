package net.pullolo.clientpowers.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.pullolo.clientpowers.module.DynamicLightModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelLightEngine.class)
public class WorldMixin {

    @Inject(method = "getRawBrightness", at = @At("RETURN"), cancellable = true)
    private void onGetRawBrightness(BlockPos pos, int ambientDarkness, CallbackInfoReturnable<Integer> cir) {
        if (!DynamicLightModule.INSTANCE.isEnabled()) return;

        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) return;
        if (!client.isSameThread()) return;

        BlockPos playerPos = client.player.blockPosition();
        int radius = DynamicLightModule.INSTANCE.getRadius();
        int dx = pos.getX() - playerPos.getX();
        int dy = pos.getY() - playerPos.getY();
        int dz = pos.getZ() - playerPos.getZ();

        if (dx * dx + dy * dy + dz * dz <= radius * radius) {
            if (cir.getReturnValue() < 15) cir.setReturnValue(15);
        }
    }
}
