package com.arxyt.colonypathingedition.mixins.minecolonies;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.colony.buildings.AbstractBuildingContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The purpose of this Mixin is to modify the pickUp mechanism.
 * 1. Locally store pickUpDay to prevent all hut clearance timers from resetting
 *    every time the save is loaded.
 * 2. After attempting to issue a pickUp process, do not require the next pickUp
 *    request to solely resolve the process blockage caused by the "attempted pickUp".
 */
@Mixin(AbstractBuilding.class)
public abstract class AbstractBuildingMixin extends AbstractBuildingContainer {

    @Shadow(remap = false) public int pickUpDay;

    protected AbstractBuildingMixin(@NotNull final IColony colony, final BlockPos pos) {
        super(pos, colony);
        throw new RuntimeException("AbstractBuildingMixin 类不应被实例化！");
    }

    @Inject(method = "deserializeNBT(Lnet/minecraft/nbt/CompoundTag;)V",at = @At("RETURN"),remap = false)
    private void deserializeNBTAddition (CompoundTag compound, CallbackInfo cir) {
        if(compound.contains("pick_up_day")) {
            pickUpDay = compound.getInt("pick_up_day");
        }
    }

    @Inject(method = "serializeNBT()Lnet/minecraft/nbt/CompoundTag;",at = @At("RETURN"),remap = false,cancellable = true)
    private void serializeNBTAddition (CallbackInfoReturnable<CompoundTag> cir) {
        CompoundTag tag = cir.getReturnValue();
        tag.putInt("pick_up_day", pickUpDay);
        cir.setReturnValue(tag);
    }

    @Inject(method = "createPickupRequest",at = @At("RETURN"),remap = false)
    private void resetPickUpDay (final int pickUpPrio, CallbackInfoReturnable<Boolean> cir) {
        if(pickUpDay == -1){
            int daysToPickup = 10 - pickUpPrio;
            pickUpDay = colony.getDay() + daysToPickup;
        }
    }
}
