package com.github.standobyte.jojo.action.stand;

import com.github.standobyte.jojo.action.Action;
import com.github.standobyte.jojo.action.ActionConditionResult;
import com.github.standobyte.jojo.action.ActionTarget;
import com.github.standobyte.jojo.entity.damaging.projectile.CDBlockBulletEntity;
import com.github.standobyte.jojo.entity.damaging.projectile.MRFireballEntity;
import com.github.standobyte.jojo.entity.damaging.projectile.TommyGunBulletEntity;
import com.github.standobyte.jojo.entity.stand.*;
import com.github.standobyte.jojo.entity.stand.stands.SilverChariotEntity;
import com.github.standobyte.jojo.entity.stand.stands.StarPlatinumEntity;
import com.github.standobyte.jojo.init.ModSounds;
import com.github.standobyte.jojo.power.impl.stand.IStandPower;
import com.github.standobyte.jojo.util.mod.JojoModUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.HandSide;
import net.minecraft.world.World;

import java.util.function.Supplier;

public class ShootProjectile extends StandEntityAction {
    public static final StandPose BEARING_SHOT = new StandPose("Bearing_Shot");
    private final StandRelativeOffset userOffsetLeftArm;
    private final Supplier<MagiciansRedFireball> BearingShot;

    public ShootProjectile(StandEntityAction.Builder builder, Supplier<MagiciansRedFireball> BearingShot) {
        super(builder);
        this.userOffsetLeftArm = builder.userOffset.copyScale(-1, 1, 1);
        this.BearingShot = BearingShot;
    }



    @Override
    protected ActionConditionResult checkSpecificConditions(LivingEntity user, IStandPower power, ActionTarget target) {
        ItemStack itemToShoot = user.getOffhandItem();
        boolean isIronNugget = !itemToShoot.isEmpty() && itemToShoot.getItem() == Items.IRON_NUGGET;



        // could return the result like this instead, it just won't print the message

            return ActionConditionResult.noMessage(isIronNugget);


    }

    @Override
    public StandRelativeOffset getOffsetFromUser(IStandPower standPower, StandEntity standEntity, StandEntityTask task) {
        if (!standEntity.isArmsOnlyMode()) {
            LivingEntity user = standEntity.getUser();
            if (user.getMainArm() == HandSide.LEFT) {
                return userOffsetLeftArm;
            }
        }
        return super.getOffsetFromUser(standPower, standEntity, task);
    }

    @Override
    protected Action<IStandPower> replaceAction(IStandPower power, ActionTarget target) {
        return power.isActive() && power.getStandManifestation() instanceof StarPlatinumEntity && BearingShot != null && BearingShot.get() != null ? BearingShot.get() : this;
    }

    @Override
    public void standPerform( World world, StandEntity standEntity, IStandPower userPower, StandEntityTask task) {
        if (!world.isClientSide()) {
            LivingEntity user = userPower.getUser();
            if(userPower.canLeap()) {
                if (user == null) return;
                ItemStack item = user.getOffhandItem();
                if (!(item.getItem() instanceof BlockItem)) return;
                TommyGunBulletEntity bullet = new TommyGunBulletEntity(standEntity, world);
                bullet.setShootingPosOf(user);
                standEntity.shootProjectile(bullet, 2.0F, 0.25F);

                // do stuff

                if (!(user instanceof PlayerEntity && ((PlayerEntity) user).abilities.instabuild)) {
                    item.shrink(1);
                }
            }
        }
    }
}
