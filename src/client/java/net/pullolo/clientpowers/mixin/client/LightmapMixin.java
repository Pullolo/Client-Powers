package net.pullolo.clientpowers.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.pullolo.clientpowers.power.Power;
import net.pullolo.clientpowers.power.PowerManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * FLAME — Ember Vision: subtle brightness boost (+0.25) in dark areas,
 * giving a slight warmth advantage underground.
 */
@Mixin(LightmapTextureManager.class)
public class LightmapMixin {

    @Shadow private boolean dirty;

    @Unique private boolean wasEmberActive   = false;
    @Unique private boolean wasOceanActive   = false;
    @Unique private boolean wasShadowActive  = false;
    @Unique private boolean wasCrystalActive = false;

    // Force lightmap to recompute when any brightness-affecting power state changes.
    @Inject(method = "update", at = @At("HEAD"))
    private void forceUpdateForPowers(float tickDelta, CallbackInfo ci) {
        Power power = PowerManager.INSTANCE.getActivePower();
        boolean emberActive   = power == Power.FLAME;
        boolean oceanActive   = power == Power.OCEAN;
        boolean shadowActive  = power == Power.SHADOW;
        boolean crystalActive = power == Power.CRYSTAL;
        if (emberActive   || wasEmberActive)   this.dirty = true;
        if (oceanActive   || wasOceanActive)   this.dirty = true;
        if (shadowActive  || wasShadowActive)  this.dirty = true;
        if (crystalActive || wasCrystalActive) this.dirty = true;
        wasEmberActive   = emberActive;
        wasOceanActive   = oceanActive;
        wasShadowActive  = shadowActive;
        wasCrystalActive = crystalActive;
    }

    // Per-power brightness tweaks applied after vanilla lightmap calculation.
    @Inject(method = "getBrightness(FI)F", at = @At("RETURN"), cancellable = true)
    private static void onGetBrightness(float ambientLight, int lightLevel,
                                         CallbackInfoReturnable<Float> cir) {
        Power power = PowerManager.INSTANCE.getActivePower();
        if (power == Power.FLAME) {
            // Ember Vision: subtle warmth in all lighting
            cir.setReturnValue(Math.min(1.0f, cir.getReturnValue() + 0.25f));
        } else if (power == Power.SHADOW && lightLevel <= 4) {
            // Dark Sight: strong visibility boost only in near-complete darkness
            cir.setReturnValue(Math.min(1.0f, cir.getReturnValue() + 0.40f));
        } else if (power == Power.OCEAN) {
            // Tidal Surge: underwater clarity when fully submerged
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null && client.player.isSubmergedInWater()) {
                cir.setReturnValue(Math.min(1.0f, cir.getReturnValue() + 0.35f));
            }
        } else if (power == Power.CRYSTAL) {
            // Prism Sight: subtle clarity boost at all light levels
            cir.setReturnValue(Math.min(1.0f, cir.getReturnValue() + 0.15f));
        }
    }
}
