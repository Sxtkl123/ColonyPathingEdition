package com.arxyt.colonypathingedition.core.mixins.accessor;

import com.minecolonies.api.colony.jobs.IJob;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.core.entity.ai.workers.AbstractAISkeleton;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = AbstractAISkeleton.class,remap = false)
public interface AbstractAISkeletonAccessor<J extends IJob<?>> {
    @Accessor(value = "job",remap = false) J getJob();
    @Accessor(value = "worker",remap = false) AbstractEntityCitizen getWorker();
    @Accessor(value = "world",remap = false) Level getWorld();

    @Invoker(value = "getState",remap = false) IAIState invokeGetState();
}
