package net.pullolo.clientpowers.power;

import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;

public enum Power {
    NONE       ("None",       0xFF9B59B6, 0xFF000000),
    FLAME      ("Flame",      0xFFE74C3C, 0xFFFF4400),
    FROST      ("Frost",      0xFF3498DB, 0xFF88DDFF),
    THUNDER    ("Thunder",    0xFFF1C40F, 0xFFFFFF44),
    VOID       ("Void",       0xFF6C3483, 0xFF9900CC),
    STARGAZER  ("Stargazer",  0xFF8080FF, 0xFFEEEEFF),
    NATURE     ("Nature",     0xFF27AE60, 0xFF55FF88),
    SHADOW     ("Shadow",     0xFF5D6D7E, 0xFF1A0030),
    OCEAN      ("Ocean",      0xFF1ABC9C, 0xFF00DDCC),
    BLOOD      ("Blood",      0xFFC0392B, 0xFF8B0000),
    WIND       ("Wind",       0xFFAED6F1, 0xFFE8F8FF),
    CRYSTAL    ("Crystal",    0xFFF0E6FF, 0xFFFFFFFF),
    LODESTAR   ("Lodestar",   0xFFF39C12, 0xFFFFD700),
    NINJA      ("Ninja",      0xFF9E9E9E, 0xFF050508);

    public final String displayName;
    public final int accentColor;
    public final int auraColor;

    Power(String displayName, int accentColor, int auraColor) {
        this.displayName = displayName;
        this.accentColor = accentColor;
        this.auraColor   = auraColor;
    }

    public ParticleEffect getBodyParticle() {
        return switch (this) {
            case FLAME      -> ParticleTypes.FLAME;
            case FROST      -> ParticleTypes.SNOWFLAKE;
            case THUNDER    -> ParticleTypes.ELECTRIC_SPARK;
            case VOID       -> ParticleTypes.PORTAL;
            case STARGAZER  -> ParticleTypes.END_ROD;
            case NATURE     -> ParticleTypes.HAPPY_VILLAGER;
            case SHADOW     -> ParticleTypes.SOUL;
            case OCEAN      -> ParticleTypes.DRIPPING_WATER;
            case BLOOD      -> ParticleTypes.LAVA;
            case WIND       -> ParticleTypes.CLOUD;
            case CRYSTAL    -> ParticleTypes.ENCHANT;
            case LODESTAR   -> ParticleTypes.CRIT;
            case NINJA      -> ParticleTypes.POOF;
            default         -> null;
        };
    }

    public ParticleEffect getAuraParticle() {
        return switch (this) {
            case FLAME      -> ParticleTypes.FLAME;
            case FROST      -> ParticleTypes.SNOWFLAKE;
            case THUNDER    -> ParticleTypes.END_ROD;
            case VOID       -> ParticleTypes.PORTAL;
            case STARGAZER  -> ParticleTypes.END_ROD;
            case NATURE     -> ParticleTypes.GLOW;
            case SHADOW     -> ParticleTypes.SOUL_FIRE_FLAME;
            case OCEAN      -> ParticleTypes.BUBBLE;
            case BLOOD      -> ParticleTypes.FALLING_LAVA;
            case WIND       -> ParticleTypes.END_ROD;
            case CRYSTAL    -> ParticleTypes.NAUTILUS;
            case LODESTAR   -> ParticleTypes.GLOW;
            case NINJA      -> ParticleTypes.ASH;
            default         -> null;
        };
    }

    public ParticleEffect getEnvironmentalParticle() {
        return switch (this) {
            case FLAME      -> ParticleTypes.LARGE_SMOKE;
            case FROST      -> ParticleTypes.SNOWFLAKE;
            case THUNDER    -> ParticleTypes.ELECTRIC_SPARK;
            case VOID       -> ParticleTypes.ASH;
            case STARGAZER  -> ParticleTypes.GLOW;
            case NATURE     -> ParticleTypes.SPORE_BLOSSOM_AIR;
            case SHADOW     -> ParticleTypes.REVERSE_PORTAL;
            case OCEAN      -> ParticleTypes.UNDERWATER;
            case BLOOD      -> ParticleTypes.CAMPFIRE_SIGNAL_SMOKE;
            case WIND       -> ParticleTypes.CLOUD;
            case CRYSTAL    -> ParticleTypes.GLOW;
            case LODESTAR   -> ParticleTypes.NAUTILUS;
            case NINJA      -> ParticleTypes.SMOKE;
            default         -> null;
        };
    }

    public ParticleEffect getHitParticle() {
        return switch (this) {
            case FLAME      -> ParticleTypes.FLAME;
            case FROST      -> ParticleTypes.SNOWFLAKE;
            case THUNDER    -> ParticleTypes.ELECTRIC_SPARK;
            case VOID       -> ParticleTypes.PORTAL;
            case STARGAZER  -> ParticleTypes.END_ROD;
            case NATURE     -> ParticleTypes.HAPPY_VILLAGER;
            case SHADOW     -> ParticleTypes.ASH;
            case OCEAN      -> ParticleTypes.SPLASH;
            case BLOOD      -> ParticleTypes.DRIPPING_LAVA;
            case WIND       -> ParticleTypes.CLOUD;
            case CRYSTAL    -> ParticleTypes.ENCHANT;
            case LODESTAR   -> ParticleTypes.CRIT;
            case NINJA      -> ParticleTypes.CRIT;
            default         -> null;
        };
    }

    public boolean hasDefaultWings() {
        return this == FLAME || this == VOID;
    }

    public static Power fromName(String name) {
        for (Power p : values()) {
            if (p.name().equalsIgnoreCase(name)) return p;
        }
        return NONE;
    }
}
