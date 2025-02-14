package com.github.standobyte.jojo.action.non_stand;

import com.github.standobyte.jojo.action.ActionConditionResult;
import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.entity.damaging.projectile.HamonCutterEntity;
import com.github.standobyte.jojo.power.impl.nonstand.INonStandPower;
import com.github.standobyte.jojo.power.impl.nonstand.type.hamon.HamonUtil;
import com.github.standobyte.jojo.util.mc.MCUtil;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PotionItem;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

public class HamonCutter extends HamonAction {

    public HamonCutter(HamonAction.Builder builder) {
        super(builder);
    }
    
    @Override
    protected ActionConditionResult checkHeldItems(LivingEntity user, INonStandPower power) {
        if (getUseableItem(user).isEmpty()) {
            return conditionMessage("potion");
        }
        return ActionConditionResult.POSITIVE;
    }
    
    @Override
    protected void perform(World world, LivingEntity user, INonStandPower power, ActionTarget target) {
        if (!world.isClientSide()) {
            ItemStack potionItem = getUseableItem(user);
            if (potionItem.isEmpty()) return;

            PlayerEntity player = null;
            if (user instanceof PlayerEntity) {
                player = (PlayerEntity) user;
            }
            if (player == null || !player.abilities.instabuild) {
                potionItem.shrink(1);
                MCUtil.giveItemTo(user, new ItemStack(Items.GLASS_BOTTLE), true);
            }
            
            Vector3d shootingPos = null;
            for (int i = 0; i < 8; i++) {
                HamonCutterEntity hamonCutterEntity = new HamonCutterEntity(user, world, potionItem);
                hamonCutterEntity.setHamonStatPoints(getEnergyCost(power, target) / 8F);
                hamonCutterEntity.shootFromRotation(user, 1.35F + user.getRandom().nextFloat() * 0.3F, 10.0F);
                if (i == 0) shootingPos = hamonCutterEntity.position();
                world.addFreshEntity(hamonCutterEntity);
            }
            HamonUtil.emitHamonSparkParticles(world, null, shootingPos.x, shootingPos.y, shootingPos.z, 0.75F);
        }
    }
    
    private ItemStack getUseableItem(LivingEntity entity) {
        ItemStack potionItem = entity.getMainHandItem();
        if (potionItem.isEmpty() || !(potionItem.getItem() instanceof PotionItem)) {
            potionItem = entity.getOffhandItem();
            if (potionItem.isEmpty() || !(potionItem.getItem() instanceof PotionItem)) {
                return ItemStack.EMPTY;
            }
        }
        return potionItem;
    }
}

