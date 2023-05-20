package com.github.standobyte.jojo.client.particle.custom;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.github.standobyte.jojo.client.ClientUtil;
import com.github.standobyte.jojo.client.particle.HamonAuraParticle;
import com.github.standobyte.jojo.init.ModParticles;
import com.github.standobyte.jojo.util.general.GeneralUtil;
import com.github.standobyte.jojo.util.mc.reflection.ClientReflection;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.IAnimatedSprite;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.settings.ParticleStatus;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ParticleType;
import net.minecraft.util.Hand;
import net.minecraft.util.HandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.registries.ForgeRegistries;

public class CustomParticlesHelper {
    
    private static final Map<ResourceLocation, IAnimatedSprite> SPRITE_SETS = new HashMap<>();
    
    public static void saveSprites(Minecraft mc) {
        Map<ResourceLocation, ? extends IAnimatedSprite> spritesMap = ClientReflection.getSpriteSets(mc.particleEngine);
        CustomParticlesHelper.saveSprites(spritesMap, ModParticles.CD_RESTORATION.get());
        CustomParticlesHelper.saveSprites(spritesMap, ModParticles.HAMON_SPARK.get());
        CustomParticlesHelper.saveSprites(spritesMap, ModParticles.HAMON_SPARK_BLUE.get());
        CustomParticlesHelper.saveSprites(spritesMap, ModParticles.HAMON_SPARK_YELLOW.get());
        CustomParticlesHelper.saveSprites(spritesMap, ModParticles.HAMON_SPARK_RED.get());
        CustomParticlesHelper.saveSprites(spritesMap, ModParticles.HAMON_SPARK_SILVER.get());
        CustomParticlesHelper.saveSprites(spritesMap, ModParticles.HAMON_AURA.get());
        CustomParticlesHelper.saveSprites(spritesMap, ModParticles.HAMON_AURA_BLUE.get());
        CustomParticlesHelper.saveSprites(spritesMap, ModParticles.HAMON_AURA_YELLOW.get());
        CustomParticlesHelper.saveSprites(spritesMap, ModParticles.HAMON_AURA_RED.get());
        CustomParticlesHelper.saveSprites(spritesMap, ModParticles.HAMON_AURA_SILVER.get());
    }
    
    public static IAnimatedSprite getSavedSpriteSet(ParticleType<?> particleType) {
        return SPRITE_SETS.get(ForgeRegistries.PARTICLE_TYPES.getKey(particleType));
    }
    
    private static void saveSprites(Map<ResourceLocation, ? extends IAnimatedSprite> sprites, ParticleType<?> particleType) {
        ResourceLocation key = ForgeRegistries.PARTICLE_TYPES.getKey(particleType);
        SPRITE_SETS.put(key, sprites.get(key));
    }
    
    
    
    public static boolean createCDRestorationParticle(LivingEntity entity, Hand hand) {
        if (!ClientUtil.canSeeStands()) return false;
        
        EntityPosParticle particle = CDRestorationHandItemParticle.createCustomParticle((ClientWorld) entity.level, entity, hand);
        return addParticle(particle, particle.getPos(), false, false);
    }
    
    @SuppressWarnings("resource")
    public static boolean createBloodParticle(IParticleData type, @Nullable Entity entity, 
            double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        BloodFromEntityParticle particle = BloodFromEntityParticle.createCustomParticle(
                type, Minecraft.getInstance().level, entity, x, y, z, xSpeed, ySpeed, zSpeed);
        return addParticle(particle, new Vector3d(x, y, z), false, false);
    }
    
    @SuppressWarnings("resource")
    public static void createHamonAuraParticle(IParticleData type, 
            LivingEntity user, double x, double y, double z) {
        IAnimatedSprite sprite = getSavedSpriteSet(type.getType());
        if (sprite != null) {
            HamonAuraParticle particle = HamonAuraParticle.createCustomParticle(
                    sprite, Minecraft.getInstance().level, user, x, y, z);
            addParticle(particle, new Vector3d(x, y, z), false, false);
        }
        else {
            Minecraft.getInstance().level.addParticle(type, x, y, z, 0, 0, 0);
        }
    }

    // FIXME !!!!!! particles at arms in 1st person
    public static void summonHamonAuraParticlesFirstPerson(IParticleData type, LivingEntity user, float particlesPerTick) {
//        IAnimatedSprite sprite = getSavedSpriteSet(type.getType());
//        if (sprite != null) {
//            Minecraft mc = Minecraft.getInstance();
//            Vector3d vec = user.position();
//            
//            GeneralUtil.doFractionTimes(() -> {
//                addParticle(HamonAuraParticle.firstPersonHandParticle(sprite, mc.level, user, HandSide.RIGHT, vec), vec, false, false);
//                addParticle(HamonAuraParticle.firstPersonHandParticle(sprite, mc.level, user, HandSide.LEFT, vec), vec, false, false);
//            }, particlesPerTick);
//        }
    }
    
    public static boolean addParticle(Particle particle, Vector3d particlePos, boolean overrideLimiter, boolean alwaysVisible) {
        Minecraft mc = Minecraft.getInstance();
        ActiveRenderInfo activerenderinfo = mc.gameRenderer.getMainCamera();
        if (activerenderinfo.isInitialized() && mc.particleEngine != null && activerenderinfo.getPosition().distanceToSqr(particlePos) < 1024.0D) {
            if (alwaysVisible || calculateParticleLevel(mc, mc.level, alwaysVisible) != ParticleStatus.MINIMAL) {
                mc.particleEngine.add(particle);
                return true;
            } 
        }
        return false;
    }

    private static ParticleStatus calculateParticleLevel(Minecraft mc, ClientWorld world, boolean overrideLimiter) {
        ParticleStatus status = mc.options.particles;
        if (overrideLimiter && status == ParticleStatus.MINIMAL && world.random.nextInt(10) == 0) {
            status = ParticleStatus.DECREASED;
        }

        if (status == ParticleStatus.DECREASED && world.random.nextInt(3) == 0) {
            status = ParticleStatus.MINIMAL;
        }

        return status;
    }
}
