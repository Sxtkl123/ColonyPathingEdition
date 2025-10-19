package com.arxyt.colonypathingedition.core.mixins.citizen;

import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.core.colony.CitizenData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CitizenData.class)
public class CitizenDataMixinForLeisure {

    // No more punishments on leisure time as building upgrades.
    @Redirect(
            method = "update",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/minecolonies/api/colony/buildings/IBuilding;getBuildingLevel()I"
            ),
            remap = false
    )
    private int redirectGetBuildingLevel(IBuilding instance) {
        return 1;
    }
}
