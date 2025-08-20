package com.arxyt.colonypathingedition.core.mixins.entity;

import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.entity.citizen.Skill;
import com.minecolonies.api.entity.other.MinecoloniesMinecart;
import com.minecolonies.core.colony.jobs.JobDeliveryman;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

import static com.minecolonies.api.research.util.ResearchConstants.WALKING;


@Mixin(MinecoloniesMinecart.class)
public class MinecoloniesMinecartMixin extends Minecart {

    public MinecoloniesMinecartMixin(final EntityType<?> type, final Level world)
    {
        super(type, world);
    }

    @Override
    public double getMaxSpeedWithRail() { //Non-default because getMaximumSpeed is protected
        if (!canUseRail()) return getMaxSpeed();
        BlockPos pos = this.getCurrentRailPosition();
        BlockState state = this.level().getBlockState(pos);
        if (!state.is(BlockTags.RAILS)) return getMaxSpeed();

        float railMaxSpeed = ((BaseRailBlock)state.getBlock()).getRailMaxSpeed(state, this.level(), pos, this);

        double speedFactor = 1;
        if(!this.getPassengers().isEmpty() && this.getPassengers().get(0) instanceof AbstractEntityCitizen citizen){
            MobEffectInstance effect = citizen.getEffect(MobEffects.MOVEMENT_SPEED);
            if (effect != null) {
                speedFactor += effect.getAmplifier() * 0.3;
            }
            if(citizen.getCitizenColonyHandler() != null && citizen.getCitizenColonyHandler().getColony() != null && citizen.getCitizenColonyHandler().getColony().getResearchManager().getResearchEffects().getEffectStrength(WALKING) > 0){
                speedFactor += citizen.getCitizenColonyHandler().getColony().getResearchManager().getResearchEffects().getEffectStrength(WALKING) * 2;
            }
            if (citizen.getCitizenData() != null && citizen.getCitizenData().getJob() instanceof JobDeliveryman){
                speedFactor += citizen.getCitizenData().getCitizenSkillHandler().getLevel(Skill.Agility) * 0.03;
            }
        }

        return Math.min(railMaxSpeed * speedFactor, getCurrentCartSpeedCapOnRail());
    }
}
