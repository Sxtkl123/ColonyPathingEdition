package com.arxyt.colonypathingedition.core.mixins.heal;

import com.minecolonies.api.entity.ai.statemachine.states.CitizenAIState;
import com.minecolonies.api.entity.ai.statemachine.states.IState;
import com.minecolonies.core.colony.jobs.JobHealer;
import com.minecolonies.core.entity.ai.workers.CitizenAI;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(CitizenAI.class)
public class CitizenAIMixin {

    @Final
    @Shadow(remap = false)
    private EntityCitizen citizen;

    @Inject(
            method = "calculateNextState",
            at = @At(value = "RETURN"),
            cancellable = true,
            remap = false
    )
    private void redirectDoctorDuringRaid(CallbackInfoReturnable<IState> cir) {
        // 检查原始返回值是否是 SLEEP 且是因为殖民地被袭击
        if (cir.getReturnValue() == CitizenAIState.SLEEP
                && Objects.requireNonNull(citizen.getCitizenColonyHandler().getColonyOrRegister()).getRaiderManager().isRaided()
                && citizen.getCitizenJobHandler().getColonyJob() instanceof JobHealer) {
            if (citizen.getCitizenSleepHandler().isAsleep()) {
                citizen.getCitizenSleepHandler().onWakeUp();
            }
            cir.setReturnValue(CitizenAIState.WORK);
        }
    }

}
