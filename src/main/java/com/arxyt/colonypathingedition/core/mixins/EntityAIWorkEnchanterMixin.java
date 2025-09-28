package com.arxyt.colonypathingedition.core.mixins;

import com.minecolonies.core.colony.buildings.workerbuildings.BuildingEnchanter;
import com.minecolonies.core.colony.jobs.JobEnchanter;
import com.minecolonies.core.entity.ai.workers.crafting.AbstractEntityAICrafting;
import com.minecolonies.core.entity.ai.workers.service.EntityAIWorkEnchanter;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;


@Mixin(EntityAIWorkEnchanter.class)
public abstract class EntityAIWorkEnchanterMixin extends AbstractEntityAICrafting<JobEnchanter, BuildingEnchanter>
{
    public EntityAIWorkEnchanterMixin(@NotNull final JobEnchanter job)
    {
        super(job);
    }

    @Override
    public boolean hasWorkToDo()
    {
        return true;
    }
}
