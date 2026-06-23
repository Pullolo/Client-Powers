package net.pullolo.clientpowers.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.minecraft.world.level.dimension.DimensionType;
import net.pullolo.clientpowers.power.Power;
import net.pullolo.clientpowers.power.PowerManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Lightmap.class)
public class LightmapMixin {

    @Unique private boolean wasEmberActive   = false;
    @Unique private boolean wasOceanActive   = false;
    @Unique private boolean wasShadowActive  = false;
    @Unique private boolean wasCrystalActive = false;

    @Inject(method = "render", at = @At("HEAD"))
    private void forceUpdateForPowers(LightmapRenderState renderState, CallbackInfo ci) {
        Power power = PowerManager.INSTANCE.getActivePower();
        boolean emberActive   = power == Power.FLAME;
        boolean oceanActive   = power == Power.OCEAN;
        boolean shadowActive  = power == Power.SHADOW;
        boolean crystalActive = power == Power.CRYSTAL;
        if (emberActive   || wasEmberActive)   renderState.needsUpdate = true;
        if (oceanActive   || wasOceanActive)   renderState.needsUpdate = true;
        if (shadowActive  || wasShadowActive)  renderState.needsUpdate = true;
        if (crystalActive || wasCrystalActive) renderState.needsUpdate = true;
        wasEmberActive   = emberActive;
        wasOceanActive   = oceanActive;
        wasShadowActive  = shadowActive;
        wasCrystalActive = crystalActive;
    }

    @Inject(method = "getBrightness(Lnet/minecraft/world/level/dimension/DimensionType;I)F", at = @At("RETURN"), cancellable = true)
    private static void onGetBrightness(DimensionType dimensionType, int lightLevel,
                                         CallbackInfoReturnable<Float> cir) {
        Power power = PowerManager.INSTANCE.getActivePower();
        if (power == Power.FLAME) {
            cir.setReturnValue(Math.min(1.0f, cir.getReturnValue() + 0.25f));
        } else if (power == Power.SHADOW && lightLevel <= 4) {
            cir.setReturnValue(Math.min(1.0f, cir.getReturnValue() + 0.40f));
        } else if (power == Power.OCEAN) {
            Minecraft client = Minecraft.getInstance();
            if (client.player != null && client.player.isUnderWater()) {
                cir.setReturnValue(Math.min(1.0f, cir.getReturnValue() + 0.35f));
            }
        } else if (power == Power.CRYSTAL) {
            cir.setReturnValue(Math.min(1.0f, cir.getReturnValue() + 0.15f));
        }
    }
}
