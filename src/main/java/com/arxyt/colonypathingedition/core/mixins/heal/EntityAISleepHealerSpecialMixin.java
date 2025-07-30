package com.arxyt.colonypathingedition.core.mixins.heal;

import com.arxyt.colonypathingedition.core.api.BuildingHospitalExtra;
import com.minecolonies.api.entity.ai.statemachine.states.IState;
import com.minecolonies.api.entity.citizen.VisibleCitizenStatus;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingHospital;
import com.minecolonies.core.entity.ai.minimal.EntityAISleep;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecolonies.core.entity.pathfinding.navigation.EntityNavigationUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.START_WORKING;
import static com.minecolonies.core.entity.ai.minimal.EntityAISleep.SleepState.SLEEPING;
import static com.minecolonies.core.entity.ai.minimal.EntityAISleep.SleepState.WALKING_HOME;

@Mixin(EntityAISleep.class)
public class EntityAISleepHealerSpecialMixin {
    @Final @Shadow(remap = false) private EntityCitizen citizen;

    private boolean onDuty = false;
    private BlockPos workPos = null;

    @Inject(method = "checkSleep", at=@At("HEAD"), remap = false)
    private void specialCheckSleepForHealer(CallbackInfoReturnable<IState> cir)
    {
        if(citizen.getCitizenData().getWorkBuilding() instanceof BuildingHospital hospital){
            if(((BuildingHospitalExtra)hospital).getOnDutyCitizen(citizen.getCivilianID()) == citizen.getCivilianID()){
                workPos = hospital.getPosition();
                onDuty = true;
            }
            else{
                onDuty = false;
            }
        }
    }

    @Inject(method = "walkHome", at=@At("HEAD"), remap = false, cancellable = true)
    private void specialWalkHomeForHealer(CallbackInfoReturnable<IState> cir)
    {
        if(onDuty){
            if(!EntityNavigationUtils.walkToPos(citizen, workPos, 4, true)){
                cir.setReturnValue(WALKING_HOME);
                return;
            }
            citizen.getCitizenData().setVisibleStatus(VisibleCitizenStatus.SLEEP);
            citizen.getCitizenData().getColony().getCitizenManager().onCitizenSleep();
            citizen.setPose(Pose.SNIFFING);
            cir.setReturnValue(SLEEPING);
        }
    }

    @Inject(method = "sleep", at=@At("HEAD"), remap = false, cancellable = true)
    private void specialSleepForHealer(CallbackInfoReturnable<IState> cir)
    {
        if(onDuty){
            if(citizen.getCitizenData().getWorkBuilding() instanceof BuildingHospital hospital){
                if(!hospital.getPatients().isEmpty()){
                    citizen.getCitizenSleepHandler().onWakeUp();
                    ((BuildingHospitalExtra)hospital).setCitizenInactive();
                    ((BuildingHospitalExtra)hospital).citizenShouldWork();
                    cir.setReturnValue(START_WORKING);
                }
            }
        }
    }
}
