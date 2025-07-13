package com.arxyt.colonypathingedition.core.mixins.heal;

import com.arxyt.colonypathingedition.core.api.PatientExtras;
import com.minecolonies.core.entity.ai.workers.util.Patient;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;


@Mixin(Patient.class)
@Implements(@Interface(iface = PatientExtras.class, prefix = "extra$"))
public abstract class PatientMixin {
    @Unique int employed = -1;

    @Unique public int extra$getEmployed(){
        return employed;
    }
    @Unique public void extra$setEmployed(int employed){
        this.employed = employed;
    }
}
