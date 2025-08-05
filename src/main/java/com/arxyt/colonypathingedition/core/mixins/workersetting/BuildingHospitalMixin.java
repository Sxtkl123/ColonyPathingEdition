package com.arxyt.colonypathingedition.core.mixins.workersetting;

import com.arxyt.colonypathingedition.core.api.BuildingHospitalExtra;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingHospital;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;


@Mixin(BuildingHospital.class)
public abstract class BuildingHospitalMixin implements BuildingHospitalExtra {

    @Unique private BuildingHospital asHospital() {
        return (BuildingHospital)(Object)this;
    }

    @Unique private int healerOnDuty = -1;

    @Unique private boolean workerActive = false;

    @Unique private boolean shouldWork = false;

    @Unique
    public int getOnDutyCitizen(int citizenId){
        final BuildingHospital hospital = asHospital();
        if (!workerActive) {
            Set<ICitizenData> citizens = hospital.getAllAssignedCitizen();
            if(citizens.contains(hospital.getColony().getCitizenManager().getCivilian(citizenId))){
                healerOnDuty = citizenId;
                workerActive = true;
            }
        }
        return healerOnDuty;
    }

    @Unique public void setCitizenInactive(){
        workerActive = false;
    }

    @Unique
    public void citizenShouldWork() {
        shouldWork = true;
    }

    @Unique
    public void citizenShouldNotWork() {
        shouldWork = false;
    }

    @Unique
    public boolean checkCitizenOnDuty(int citizenId) {
        return shouldWork && citizenId == healerOnDuty;
    }

    @Inject(method = "deserializeNBT(Lnet/minecraft/nbt/CompoundTag;)V",at=@At("RETURN"),remap = false)
    private void deserializeNBTAddition (CompoundTag compound,CallbackInfo cir){
        if(compound.contains("on_duty_worker")){
            healerOnDuty = compound.getInt("on_duty_worker");
        }
    }

    @Inject(method = "serializeNBT()Lnet/minecraft/nbt/CompoundTag;",at=@At("RETURN"),remap = false,cancellable = true)
    private void serializeNBTAddition (CallbackInfoReturnable<CompoundTag> cir){
        CompoundTag tag = cir.getReturnValue();
        tag.putInt("on_duty_worker", healerOnDuty);
        cir.setReturnValue(tag);
    }
}
