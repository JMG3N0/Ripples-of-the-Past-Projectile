package com.github.standobyte.jojo.util.mod;

import java.util.Comparator;

import com.github.standobyte.jojo.JojoMod;
import com.github.standobyte.jojo.power.impl.nonstand.type.vampirism.VampirismUtil;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.monster.EndermanEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.IndirectEntityDamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = JojoMod.MOD_ID)
public class ModInteractionUtil {
    private static final ResourceLocation MOWZIES_FROZEN_EFFECT = new ResourceLocation("mowziesmobs", "frozen");
    private static final ResourceLocation TWILIGHT_FOREST_FROSTED_EFFECT = new ResourceLocation("twilightforest", "frosted");
    public static float getEntityFreeze(LivingEntity entity) {
        return Math.min(entity.getActiveEffectsMap().entrySet().stream().map(entry -> {
            if (MOWZIES_FROZEN_EFFECT.equals(entry.getKey().getRegistryName())) {
                return 1F;
            }
            if (TWILIGHT_FOREST_FROSTED_EFFECT.equals(entry.getKey().getRegistryName())) {
                return Math.min((entry.getValue().getAmplifier() + 1) * 0.25F, 1);
            }
            return 0F;
        }).max(Comparator.naturalOrder()).orElse(0F), 1);
    }

    private static final ResourceLocation MUTANT_ENDERMAN_ID = new ResourceLocation("mutantbeasts", "mutant_enderman");
    private static final ResourceLocation MUTANT_ENDERMAN_ID_2 = new ResourceLocation("mutantbeasts", "endersoul_clone");
    private static final ResourceLocation MUTANT_ENDERMAN_ID_3 = new ResourceLocation("mutantbeasts", "endersoul_fragment");
    public static boolean isEntityEnderman(Entity entity) {
        if (entity == null) return false;
        
        if (entity instanceof EndermanEntity) {
            return true;
        }
        
        EntityType<?> type = entity.getType();
        if (type == null) return false;
        ResourceLocation typeId = type.getRegistryName();
        return 
                MUTANT_ENDERMAN_ID.equals(typeId) ||
                MUTANT_ENDERMAN_ID_2.equals(typeId) ||
                MUTANT_ENDERMAN_ID_3.equals(typeId);
    }
    
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttackFromOtherMods(LivingAttackEvent event) {
        DamageSource damageSource = event.getSource();
        Entity entity = damageSource.getDirectEntity();
        LivingEntity target = event.getEntityLiving();
        
        if ("mob".equals(damageSource.msgId) && entity != null) {
            ResourceLocation damagingEntityId = entity.getType().getRegistryName();
            if (damagingEntityId.getNamespace().equals("mowziesmobs")) {
                String entityName = damagingEntityId.getPath();
                if (    entityName.equals("sunstrike") || 
                        entityName.equals("solar_beam") || 
                        entityName.equals("super_nova")) {
                    boolean targetIsVampire = target instanceof PlayerEntity && JojoModUtil.isPlayerUndead((PlayerEntity) target);
                    if (targetIsVampire) {
                        DamageSource extraDmgSource = new IndirectEntityDamageSource("mowzie_sun", entity, damageSource.getEntity())
                                .bypassArmor().bypassMagic().setIsFire();
                        if (target.hurt(extraDmgSource, event.getAmount() * 4)) {
                            VampirismUtil.incSunBurn(target, 2);
                        }
                    }
                }
            }
        }
    }
}
