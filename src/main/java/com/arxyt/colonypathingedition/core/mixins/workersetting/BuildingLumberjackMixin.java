package com.arxyt.colonypathingedition.core.mixins.workersetting;

import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingLumberjack;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(BuildingLumberjack.class)
public abstract class BuildingLumberjackMixin implements IBuilding
{
    @Override
    public boolean canAssignCitizens()
    {
        if(PathingConfig.LUMBERJACK_WORK_WHEN_UNCONSTRUCTED.get()){
            return true;
        }
        return getBuildingLevel() > 0 && isBuilt();
    }
}
