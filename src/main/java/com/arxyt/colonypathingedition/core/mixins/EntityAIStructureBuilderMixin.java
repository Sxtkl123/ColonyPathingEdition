package com.arxyt.colonypathingedition.core.mixins;

import com.minecolonies.core.entity.ai.workers.builder.EntityAIStructureBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(EntityAIStructureBuilder.class)
public class EntityAIStructureBuilderMixin {
    @ModifyConstant(
            method = "walkToConstructionSite(Lnet/minecraft/core/BlockPos;)Z",
            constant = @Constant(doubleValue = 200.0),
            remap = false
    )
    private double modifyDropCost(double original)
    {
        return 1.5d;
    }
}
