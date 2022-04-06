package com.github.standobyte.jojo.entity.stand;

import javax.annotation.Nullable;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;

public class StandStatFormulas {

    public static float getHeavyAttackDamage(double strength, @Nullable LivingEntity armoredTarget) {
        float damage = Math.max((float) strength, 1F);
        return damage;
    }
    
    public static int getHeavyAttackWindup(double speed, float comboMeter) {
        float f = (40 - (float) speed * 1.25F);
        float min = f / 3;
        float max = f * 2 / 3;
        return MathHelper.ceil(MathHelper.lerp(comboMeter, max, min));
    }
    
    public static int getHeavyAttackRecovery(double speed) {
        return MathHelper.floor((40 - speed * 1.25) / 2);
    }
    
    
    
    public static float getLightAttackDamage(double strength) {
        return 0.4F + (float) strength * 0.2F;
    }
    
    public static int getLightAttackWindup(double speed, float comboMeter) {
        return Math.max((int) ((8 - speed * 0.25) * (1.0 - 0.4 * comboMeter)) + 1, 0);
    }
    
    public static int getLightAttackRecovery(double speed) {
        return Math.round(lightAttackRecovery(speed));
    }
    
    private static final float lightAttackRecovery(double speed) {
        return Math.max((40.0F - (float) speed * 1.25F) / 3.0F, 0);
    }
    
    public static float getParryTiming(double precision) {
        return Math.min(0.05F + (float) precision * 0.025F, 1F);
    }
    
    
    
    public static float getBarrageHitDamage(double strength, double precision) {
        float damage = 0.04F + (float) strength * 0.01F;
        if (precision > 0) {
            double pr = precision / 16;
            damage *= 1 + pr * 0.5 * Math.min(pr, 0.5);
        }
        return damage;
    }
    
    public static int getBarrageHitsPerSecond(double speed) {
        return Math.max((int) (speed * 8.0 - 20.0), 0);
    }
    
    public static int getBarrageRecovery(double speed) {
        return MathHelper.floor((40.0 - speed * 1.25) / 5.0);
    }
    
    public static int getBarrageMaxDuration(double durability) {
        return 20 + (int) (durability * 5.0);
    }
    
    
    
    public static float getPhysicalResistance(double durability, double strength, float blocked) {
        double resistance = MathHelper.clamp(durability * 0.01875 + strength * 0.0125, 0, 1);
        resistance += (1 - resistance) * blocked * 0.8;
        return (float) resistance;
    }
    
    public static float getStaminaMultiplier(double durability) {
        return (float) Math.pow(2, durability / 8 - 1);
    }
    
    public static float getBlockStaminaCost(float incomingDamage) {
        return 0.5F * (float) Math.pow(incomingDamage, 2);
    }
    
    public static int getSummonLockTicks(double speed) {
        return Math.max(20 - (int) (speed * 1.25), 0);
    }
    
    public static int getBlockingBreakTicks(double durability) {
        return Math.min(120 - (int) (durability * 5), 1);
    }
    
    public static float getMaxBarrageParryTickDamage(double durability) {
        return Math.max(((float) durability - 4F) * 0.125F, 0);
    }
    
    public static float getLeapStrength(double strength) {
        return (float) Math.min(strength, 40) / 5F;
    }
    
    public static double getMovementSpeed(double speed) {
        return 0.1 + speed * 0.05;
    }
    
    public static boolean isBlockBreakable(double strength, float blockHardness, int blockHarvestLevel) {
        /* damage:
         * 2                                4                                   8                                   12                                      16
         * 
         * hardness:
         * e^0.5 = 1.649                    e^1 ~ 2.718                         e^2 ~ 7.389                         e^3 ~ 20.086                            e^4 ~ 54.598
         * 
         * 0.3:  glass, glowstone           1.8:  concrete                      2.8:  blue ice                      10:   hardened glass                    22.5: ender chest
         * 0.4:  netherrack                 2:    bricks, cobblestone, wood     3.5:  furnace                                                               30:   ancient debris
         * 0.5:  dirt, ice, sand, hay       2.5:  chest                         3:    ores, gold/lapis block                                                50:   obsidian, netherite
         * 0.6:  clay, gravel                                                   4.5:  deepslate ores
         * 0.8:  quartz, sandstone, wool                                        5:    diamond/iron/redstone block
         * 1:    melon, mob head
         * 1.25: terracota, basalt 
         * 1.5:  stone 
         * 
         * harvest level:
         * -1: leaves/grass                 0: dirt/wood/stone                  1: iron                             2: diamond/gold                         3: obsidian/netherite
         */
        return blockHardness < Math.exp(strength / 4) && blockHarvestLevel < (int) strength / 4;
    }
    
    // dash
    // projectileAccuracy
    // hitboxExpansion
    // unsummonedAttackDeflectSpeed
    
    public static float rangeStrengthFactor(double rangeEffective, double rangeMax, double distance) {
        if (distance <= rangeEffective) {
            return 1F;
        }
        float f = (float) ((rangeMax - rangeEffective) / (2 * rangeEffective - rangeMax - distance));
        return f * f;
    }
}
