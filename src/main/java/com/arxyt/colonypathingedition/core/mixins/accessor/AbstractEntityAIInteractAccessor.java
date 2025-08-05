package com.arxyt.colonypathingedition.core.mixins.accessor;

import com.minecolonies.core.entity.ai.workers.AbstractEntityAIInteract;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractEntityAIInteract.class)
public interface AbstractEntityAIInteractAccessor {
    @Invoker(value = "searchForItems",remap = false) void InvokeSearchForItems(final AABB boundingBox);
}
