package net.pullolo.clientpowers.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.light.LightingProvider;
import net.pullolo.clientpowers.module.DynamicLightModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightingProvider.class)
public class WorldMixin {

    @Inject(method = "getLight", at = @At("RETURN"), cancellable = true)
    private void onGetLight(BlockPos pos, int ambientDarkness, CallbackInfoReturnable<Integer> cir) {
        if (!DynamicLightModule.INSTANCE.isEnabled()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return;
        if (!client.isOnThread()) return;

        BlockPos playerPos = client.player.getBlockPos();
        int radius = DynamicLightModule.INSTANCE.getRadius();
        int dx = pos.getX() - playerPos.getX();
        int dy = pos.getY() - playerPos.getY();
        int dz = pos.getZ() - playerPos.getZ();

        if (dx * dx + dy * dy + dz * dz <= radius * radius) {
            if (cir.getReturnValue() < 15) cir.setReturnValue(15);
        }
    }
}
