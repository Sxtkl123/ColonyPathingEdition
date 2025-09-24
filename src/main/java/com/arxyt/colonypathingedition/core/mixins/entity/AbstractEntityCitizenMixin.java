package com.arxyt.colonypathingedition.core.mixins.entity;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.entity.citizen.AbstractCivilianEntity;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AbstractEntityCitizen.class)
public abstract class AbstractEntityCitizenMixin extends AbstractCivilianEntity {

    @Shadow(remap = false) public abstract ICitizenData getCitizenData();

    public AbstractEntityCitizenMixin(final EntityType<? extends PathfinderMob> type, final Level world)
    {
        super(type, world);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        //市民职业信息
        if(getCitizenData() != null && getCitizenData().getJob() != null) {
            tag.putString("citizenJob", getCitizenData().getJob().getModel().getPath());
        }
        else{
            tag.putString("citizenJob", "unemployed");
        }
    }
}
