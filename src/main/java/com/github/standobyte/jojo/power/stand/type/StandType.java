package com.github.standobyte.jojo.power.stand.type;

import java.util.function.Predicate;
import java.util.function.Supplier;

import com.github.standobyte.jojo.action.actions.StandAction;
import com.github.standobyte.jojo.capability.entity.LivingUtilCapProvider;
import com.github.standobyte.jojo.init.ModStandTypes;
import com.github.standobyte.jojo.power.IPowerType;
import com.github.standobyte.jojo.power.stand.IStandPower;
import com.github.standobyte.jojo.power.stand.StandUtil;
import com.github.standobyte.jojo.power.stand.stats.StandStats;
import com.github.standobyte.jojo.util.JojoModUtil;
import com.github.standobyte.jojo.util.damage.IStandDamageSource;
import com.github.standobyte.jojo.util.data.StandStatsManager;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.registries.ForgeRegistryEntry;

public abstract class StandType<T extends StandStats> extends ForgeRegistryEntry<StandType<?>> implements IPowerType<IStandPower, StandType<?>> {
    private final int tier;
    private final int color;
    private final ITextComponent partName;
    private final StandAction[] attacks;
    private final StandAction[] abilities;
    private Predicate<LivingEntity> prioritizedCondition = null;
    private final T defaultStats;
    private final Class<T> statsClass;
    private String translationKey;
    private final Supplier<SoundEvent> summonShoutSupplier;
    private ResourceLocation iconTexture;
    
    public StandType(int tier, int color, ITextComponent partName, StandAction[] attacks, StandAction[] abilities, 
            Supplier<SoundEvent> summonShoutSupplier, Class<T> statsClass, T defaultStats) {
        this.tier = MathHelper.clamp(tier, 0, StandUtil.TIER_XP_LEVELS.length - 1);
        this.color = color;
        this.partName = partName;
        this.attacks = attacks;
        this.abilities = abilities;
        this.summonShoutSupplier = summonShoutSupplier;
        this.statsClass = statsClass;
        this.defaultStats = defaultStats;
    }
    
    public StandType<T> addPrioritizedCondition(Predicate<LivingEntity> condition) { // FIXME sort this thing out
        this.prioritizedCondition = condition;
        return this;
    }
    
    public T getStats() {
        return StandStatsManager.getInstance().getStats(this);
    }
    
    public T getDefaultStats() {
        return defaultStats;
    }
    
    public Class<T> getStatsClass() {
        return statsClass;
    }
    
    @Override
    public int getColor() {
        return color;
    }

    @Override
    public boolean isReplaceableWith(StandType<?> newType) {
        return false;
    }

    @Override
    public void tickUser(LivingEntity user, IStandPower power) {
        if (!power.canUsePower()) {
            forceUnsummon(user, power);
        }
    }
    
    @Override
    public StandAction[] getAttacks() {
        return attacks;
    }

    @Override
    public StandAction[] getAbilities() {
        return abilities;
    }
    
    public boolean usesStamina() {
        return false;
    }
    
    public float getMaxStamina(IStandPower power) {
        return 1000;
    }
    
    public float getStaminaRegen(IStandPower power) {
        return 2.5F;
    }
    
    public boolean usesResolve() {
        return false;
    }
    
    public int getResolveLevels() {
        return 4;
    }
    
    public void onNewResolveLevel(IStandPower power) {}
    
    public boolean usesStandComboMechanic() {
        return false;
    }
    
    @Override
    public int getExpRewardMultiplier() {
        return tier + 1;
    }
    
    @Override
    public String getTranslationKey() {
        if (translationKey == null) {
            translationKey = Util.makeDescriptionId("stand", ModStandTypes.Registry.getRegistry().getKey(this));
        }
        return this.translationKey;
    }
    
    @Override
    public ResourceLocation getIconTexture() {
        if (iconTexture == null) {
            iconTexture = JojoModUtil.makeTextureLocation("power", getRegistryName().getNamespace(), getRegistryName().getPath());
        }
        return this.iconTexture;
    }
    
    @Override
    public String getEnergyString() {
        return "stand";
    }
    
    public ITextComponent getPartName() {
        return partName;
    }

    public int getTier() {
        return tier;
    }
    
    public boolean prioritizedCondition(LivingEntity entity) {
        return prioritizedCondition == null ? false : prioritizedCondition.test(entity);
    }
    
    public void toggleSummon(IStandPower standPower) {
        if (!standPower.isActive()) {
            summon(standPower.getUser(), standPower, false);
        }
        else {
            unsummon(standPower.getUser(), standPower);
        }
    }

    public boolean summon(LivingEntity user, IStandPower standPower, boolean withoutNameVoiceLine) {
        if (!standPower.canUsePower()) {
            return false;
        }
        if (!withoutNameVoiceLine && !user.isShiftKeyDown()) {
            SoundEvent shout = summonShoutSupplier.get();
            if (shout != null) {
                JojoModUtil.sayVoiceLine(user, shout);
            }
        }
        return true;
    }
    
    public abstract void unsummon(LivingEntity user, IStandPower standPower);
    
    public abstract void forceUnsummon(LivingEntity user, IStandPower standPower);
    
    public boolean isStandSummoned(LivingEntity user, IStandPower standPower) {
        return standPower.getStandManifestation() != null;
    }
    
    public static void onHurtByStand(LivingDamageEvent event) {
        DamageSource damageSrc = event.getSource();
        if (damageSrc instanceof IStandDamageSource) {
            IStandPower attackerStand = ((IStandDamageSource) damageSrc).getStandPower();
            event.getEntityLiving().getCapability(LivingUtilCapProvider.CAPABILITY).ifPresent(cap -> {
                cap.setLastHurtByStand(attackerStand);
            });
            attackerStand.addResolveOnAttack(event.getEntityLiving(), event.getAmount());
        }
    }

//    public static void giveStandExp(LivingDeathEvent event) {
//        LivingEntity dead = event.getEntityLiving();
//        dead.getCapability(LivingUtilCapProvider.CAPABILITY).ifPresent(cap -> {
//            IStandPower stand = cap.getLastHurtByStand();
//            if (stand != null) {
//                int expToAdd = 0;
//                if (dead.getType().getCategory() == EntityClassification.MONSTER) {
//                    expToAdd = 4;
//                }
//                else if (dead instanceof PlayerEntity) {
//                    expToAdd = 20;
//                }
//                else {
//                    return;
//                }
//                
//                expToAdd *= IStandPower.getStandPowerOptional(dead).map(deadPower -> {
//                    StandType<?> type = deadPower.getType();
//                    return type != null ? type.getExpRewardMultiplier() : 1;
//                }).orElse(1);
//                expToAdd *= INonStandPower.getNonStandPowerOptional(dead).map(deadPower -> {
//                    NonStandPowerType<?> type = deadPower.getType();
//                    return type != null ? type.getExpRewardMultiplier() : 1;
//                }).orElse(1);
//                expToAdd = (int) ((float) expToAdd * dead.getMaxHealth() / 20F);
//                stand.setXp(stand.getXp() + expToAdd);
//            }
//        });
//    }
}
