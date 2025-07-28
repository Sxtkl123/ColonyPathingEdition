package com.arxyt.colonypathingedition.core.mixins;

import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.minecolonies.core.entity.pathfinding.navigation.MinecoloniesAdvancedPathNavigate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(MinecoloniesAdvancedPathNavigate.class)
public abstract class MinecoloniesAdvancedPathNavigateMixin {
    /**
     * 将 900 * 900 替换为 (maxDistance)^2
     */
    @ModifyConstant(
            method = "setPathJob",
            constant = @Constant(doubleValue = 900 * 900),
            remap = false
    )
    private double modifyMaxDistanceSqr(double original) {
        return PathingConfig.MAX_PATHING_DISTANCE.get() * PathingConfig.MAX_PATHING_DISTANCE.get();
    }

}
