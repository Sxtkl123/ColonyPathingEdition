package com.arxyt.colonypathingedition.mixins.minecolonies.raider;

import com.minecolonies.api.entity.ai.combat.threat.IThreatTableEntity;
import com.minecolonies.api.entity.ai.statemachine.states.IState;
import com.minecolonies.api.entity.ai.statemachine.tickratestatemachine.ITickRateStateMachine;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.entity.mobs.AbstractEntityMinecoloniesMonster;
import com.minecolonies.core.colony.jobs.AbstractJobGuard;
import com.minecolonies.core.entity.ai.combat.AttackMoveAI;
import com.minecolonies.core.entity.mobs.aitasks.RaiderRangedAI;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.GUARD_FLEE;

@Mixin(value = RaiderRangedAI.class, remap = false)
public class RaiderRangedAIMixin<T extends AbstractEntityMinecoloniesMonster & IThreatTableEntity> extends AttackMoveAI<T> {
    public RaiderRangedAIMixin(
            final T owner,
            final ITickRateStateMachine<IState> stateMachine)
    {
        super(owner, stateMachine);
    }

    /**
     * @author ARxyt
     * Reduce the threat level of fleeing guards.
     */
    @Override
    protected boolean checkForTarget()
    {
        if (target != null && target.isAlive())
        {
            if(target instanceof AbstractEntityCitizen citizen){
                if(citizen.getCitizenJobHandler().getColonyJob() instanceof AbstractJobGuard guard && guard.getWorkerAI().getState() == GUARD_FLEE){
                    user.getThreatTable().addThreat(target, 20 - user.getThreatTable().getThreatFor(target));
                }
            }
            else if(target instanceof Player player){
                if(player.isCreative()||player.isSpectator()){
                    resetTarget();
                    return false;
                }
            }
        }
        return super.checkForTarget();
    }

    /**
     * @author ARxyt
     * Initialize part of the threat levels so guards can preferentially draw aggro; raiders can now dynamically determine threat targets.
     */
    @Override
    protected boolean searchNearbyTarget()
    {
        final List<LivingEntity> entities = user.level().getEntitiesOfClass(LivingEntity.class, getSearchArea());

        if (entities.isEmpty())
        {
            return false;
        }

        boolean foundTarget = false;
        for (final LivingEntity entity : entities)
        {
            if (!entity.isAlive())
            {
                continue;
            }

            if (skipSearch(entity))
            {
                return false;
            }

            if (isEntityValidTarget(entity) )
            {
                if (entity instanceof AbstractEntityCitizen citizen && citizen.getCitizenJobHandler().getColonyJob() instanceof AbstractJobGuard guard && guard.getWorkerAI().getState() != GUARD_FLEE){
                    user.getThreatTable().addThreat(entity, 40);
                }
                else if( user.getSensing().hasLineOfSight(entity)) {
                    if (entity instanceof AbstractEntityCitizen citizen && citizen.getCitizenJobHandler().getColonyJob() instanceof AbstractJobGuard){
                        user.getThreatTable().addThreat(entity, 20);
                    }
                    else if (entity instanceof Player player && !(player.isCreative() || player.isSpectator())){
                        user.getThreatTable().addThreat(entity, 20);
                    }
                    else{
                        user.getThreatTable().addThreat(entity, 0);
                    }
                }
                else
                    foundTarget = true;
            }
        }

        return foundTarget;
    }
}
