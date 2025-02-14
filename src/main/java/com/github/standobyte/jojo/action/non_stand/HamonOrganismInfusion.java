package com.github.standobyte.jojo.action.non_stand;

import java.util.Optional;
import java.util.Set;

import com.github.standobyte.jojo.action.ActionConditionResult;
import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.action.ActionTarget.TargetType;
import com.github.standobyte.jojo.capability.entity.hamonutil.EntityHamonChargeCapProvider;
import com.github.standobyte.jojo.entity.HamonBlockChargeEntity;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.init.power.non_stand.ModPowers;
import com.github.standobyte.jojo.init.power.non_stand.hamon.ModHamonActions;
import com.github.standobyte.jojo.power.impl.nonstand.INonStandPower;
import com.github.standobyte.jojo.power.impl.nonstand.type.hamon.HamonData;
import com.github.standobyte.jojo.util.general.Container;
import com.github.standobyte.jojo.util.mod.JojoModUtil;
import com.google.common.collect.ImmutableSet;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FlowerPotBlock;
import net.minecraft.block.SnowyDirtBlock;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.entity.passive.AmbientEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.GolemEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

public class HamonOrganismInfusion extends HamonAction {

    public HamonOrganismInfusion(HamonAction.Builder builder) {
        super(builder);
    }

    @Override
    public ActionConditionResult checkTarget(ActionTarget target, LivingEntity user, INonStandPower power) {
        switch (target.getType()) {
        case ENTITY:
            Entity entity = target.getEntity();
            boolean isLiving;
            if (entity instanceof LivingEntity) {
                LivingEntity targetLiving = (LivingEntity) entity;
                // not the best way to determine living mobs in other mods
                isLiving = !(targetLiving instanceof StandEntity || targetLiving instanceof ArmorStandEntity || targetLiving instanceof GolemEntity)
                        && !JojoModUtil.isUndead(targetLiving);
            }
            else {
                isLiving = false;
            }
            if (!isLiving) {
                return conditionMessage("living_mob");
            }
            break;
        case BLOCK:
            BlockPos blockPos = target.getBlockPos();
            BlockState blockState = user.level.getBlockState(blockPos);
            Block block = blockState.getBlock();
            if (!(isBlockLiving(blockState) || block instanceof FlowerPotBlock && blockState.getBlock() != Blocks.FLOWER_POT)) {
                return conditionMessage("living_plant");
            }
            break;
        default:
            break;
        }
        return ActionConditionResult.POSITIVE;
    }
    
    @Override
    public TargetRequirement getTargetRequirement() {
        return TargetRequirement.ANY;
    }
    
    @Override
    public float getEnergyCost(INonStandPower power, ActionTarget target) {
        if (this == ModHamonActions.HAMON_PLANT_INFUSION.get() || target.getType() == TargetType.ENTITY) {
            return super.getEnergyCost(power, target);
        }
        return ModHamonActions.HAMON_PLANT_INFUSION.get().getEnergyCost(power, target);
    }
    
    @Override
    public void overrideVanillaMouseTarget(Container<ActionTarget> targetContainer, World world, LivingEntity user, INonStandPower power) {
        if (getTargetRequirement().checkTargetType(TargetType.ENTITY) && targetContainer.get().getType() == TargetType.BLOCK) {
            BlockPos blockPos = targetContainer.get().getBlockPos();
            VoxelShape shape = world.getBlockState(blockPos).getShape(world, blockPos);
            if (shape.isEmpty()) {
                targetContainer.set(ActionTarget.EMPTY);
                return;
            }
            Optional<Entity> entityInside = world.getEntities(user, shape.bounds().move(blockPos))
                    .stream()
                    .filter(entity -> (entity instanceof AnimalEntity || entity instanceof AmbientEntity)
                            && entity.getCapability(EntityHamonChargeCapProvider.CAPABILITY).map(cap -> !cap.hasHamonCharge()).orElse(false))
                    .findAny();
            if (entityInside.isPresent()) {
                targetContainer.set(new ActionTarget(entityInside.get()));
            }
        }
    }
    
    @Override
    protected void perform(World world, LivingEntity user, INonStandPower power, ActionTarget target) {
        if (!world.isClientSide()) {
            HamonData hamon = power.getTypeSpecificData(ModPowers.HAMON.get()).get();
            float hamonEfficiency = hamon.getActionEfficiency(getEnergyCost(power, target), true);
            int chargeTicks = 100 + MathHelper.floor((float) (1100 * hamon.getHamonStrengthLevel())
                    / (float) HamonData.MAX_STAT_LEVEL * hamonEfficiency * hamonEfficiency);
            switch(target.getType()) {
            case BLOCK:
                BlockPos blockPos = target.getBlockPos();
                world.getEntitiesOfClass(HamonBlockChargeEntity.class, 
                        new AxisAlignedBB(Vector3d.atCenterOf(blockPos), Vector3d.atCenterOf(blockPos))).forEach(Entity::remove);
                HamonBlockChargeEntity charge = new HamonBlockChargeEntity(world, target.getBlockPos());
                charge.setCharge(0.02F * hamon.getHamonDamageMultiplier() * hamonEfficiency, chargeTicks, user, getEnergyCost(power, target));
                world.addFreshEntity(charge);
                break;
            case ENTITY:
                LivingEntity entity = (LivingEntity) target.getEntity();
                entity.getCapability(EntityHamonChargeCapProvider.CAPABILITY).ifPresent(cap -> 
                cap.setHamonCharge(0.2F * hamon.getHamonDamageMultiplier() * hamonEfficiency, chargeTicks, user, getEnergyCost(power, target)));
                break;
            default:
                break;
            }
        }
    }

    private static final Set<Material> LIVING_MATERIALS = ImmutableSet.<Material>builder().add(
            Material.PLANT,
            Material.WATER_PLANT,
            Material.REPLACEABLE_PLANT,
            Material.REPLACEABLE_FIREPROOF_PLANT,
            Material.REPLACEABLE_WATER_PLANT,
            Material.GRASS,
            Material.BAMBOO_SAPLING,
            Material.BAMBOO,
            Material.LEAVES,
            Material.CACTUS,
            Material.CORAL,
            Material.VEGETABLE,
            Material.EGG
            ).build();
    public static boolean isBlockLiving(BlockState blockState) {
        if (LIVING_MATERIALS.contains(blockState.getMaterial())) {
            return true;
        }
        
        Block block = blockState.getBlock();
        return block instanceof SnowyDirtBlock || block.getRegistryName().getPath().contains("mossy");
    }

}
