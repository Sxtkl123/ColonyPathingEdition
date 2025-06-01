package com.arxyt.colonypathingedition.core.mixins.accessor;

import com.minecolonies.api.inventory.InventoryCitizen;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIBasic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = AbstractEntityAIBasic.class,remap = false)
public interface AbstractEntityAIBasicAccessor<B extends AbstractBuilding> {
    @Invoker(value = "getInventory",remap = false)
    InventoryCitizen invokeGetInventory();
    @Accessor(value = "building",remap = false)
    B getBuilding();
    @Invoker(value = "getSecondarySkillLevel",remap = false )
    int invokeGetSecondarySkillLevel();
}
