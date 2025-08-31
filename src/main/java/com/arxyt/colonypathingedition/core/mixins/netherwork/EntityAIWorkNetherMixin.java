package com.arxyt.colonypathingedition.core.mixins.netherwork;

import com.minecolonies.core.colony.buildings.workerbuildings.BuildingNetherWorker;
import com.minecolonies.core.colony.jobs.JobNetherWorker;
import com.minecolonies.core.entity.ai.workers.crafting.AbstractEntityAICrafting;
import com.minecolonies.core.entity.ai.workers.production.EntityAIWorkNether;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.*;

@Mixin(EntityAIWorkNether.class)
public abstract class EntityAIWorkNetherMixin extends AbstractEntityAICrafting<JobNetherWorker, BuildingNetherWorker> {

    public EntityAIWorkNetherMixin(@NotNull JobNetherWorker job) {
        super(job);
        throw new RuntimeException("EntityAIWorkNetherMixin 类不应被实例化！");
    }


}
