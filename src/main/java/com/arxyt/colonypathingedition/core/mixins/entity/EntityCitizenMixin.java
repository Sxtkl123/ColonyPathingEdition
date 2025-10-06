package com.arxyt.colonypathingedition.core.mixins.entity;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.entity.ai.statemachine.states.IState;
import com.minecolonies.api.entity.ai.statemachine.tickratestatemachine.ITickRateStateMachine;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.entity.citizen.citizenhandlers.ICitizenColonyHandler;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(EntityCitizen.class)
public abstract class EntityCitizenMixin extends AbstractEntityCitizen {


    @Shadow(remap = false) public abstract ITickRateStateMachine<IState> getCitizenAI();
    @Shadow(remap = false) public abstract ICitizenColonyHandler getCitizenColonyHandler();

    public EntityCitizenMixin(final EntityType<? extends PathfinderMob> type, final Level world)
    {
        super(type, world);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        //市民全局状态信息
        tag.putString("aiState", getCitizenAI().getState().toString());
        if(getCitizenColonyHandler() != null && getCitizenColonyHandler().getColony() != null) {
            tag.putString("owner", getCitizenColonyHandler().getColony().getPermissions().getOwner().toString());
        }
    }
}
