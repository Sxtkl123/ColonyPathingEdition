package com.arxyt.colonypathingedition.core.mixins.raider;

import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.entity.mobs.AbstractEntityMinecoloniesMonster;
import com.minecolonies.api.entity.mobs.AbstractEntityMinecoloniesRaider;
import com.minecolonies.core.colony.jobs.AbstractJobGuard;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.GUARD_FLEE;

@Mixin(AbstractEntityMinecoloniesRaider.class)
public abstract class AbstractEntityMinecoloniesRaiderMixin extends AbstractEntityMinecoloniesMonster {

    public AbstractEntityMinecoloniesRaiderMixin(final EntityType<? extends AbstractEntityMinecoloniesRaider> type, final Level world)
    {
        super(type, world);
        throw new RuntimeException("AbstractEntityMinecoloniesRaiderMixin 类不应被实例化！");
    }

    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;I)V", at = @At("RETURN"), remap = false)
    public void attackTargetHandler(CallbackInfo ci){
        this.targetSelector.addGoal(6,
                new NearestAttackableTargetGoal<>(
                        this,
                        AbstractEntityCitizen.class,
                        false,
                        e -> e instanceof AbstractEntityCitizen citizen && citizen.getCitizenJobHandler().getColonyJob() instanceof AbstractJobGuard guard && guard.getWorkerAI().getState() != GUARD_FLEE)
        );
        this.targetSelector.addGoal(7,
                new NearestAttackableTargetGoal<>(
                        this,
                        AbstractEntityCitizen.class,
                        true,
                        e -> e instanceof AbstractEntityCitizen citizen && citizen.getCitizenJobHandler().getColonyJob() instanceof AbstractJobGuard guard && guard.getWorkerAI().getState() == GUARD_FLEE)
        );
        this.targetSelector.addGoal(7,
                new NearestAttackableTargetGoal<>(
                        this,
                        Player.class,
                        true,
                        e -> e instanceof Player player && !player.isSpectator() && !player.isCreative()
                )
        );
        this.targetSelector.addGoal(8,
                new NearestAttackableTargetGoal<>(
                        this,
                        AbstractEntityCitizen.class,
                        true)
        );
    }
}
