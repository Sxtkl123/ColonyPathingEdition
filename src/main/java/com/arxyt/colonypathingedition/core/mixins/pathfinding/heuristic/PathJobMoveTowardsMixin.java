package com.arxyt.colonypathingedition.core.mixins.pathfinding.heuristic;

import com.arxyt.colonypathingedition.core.mixins.accessor.AbstractPathJobAccessor;
import com.arxyt.colonypathingedition.core.util.DistanceUtils;
import com.minecolonies.core.entity.pathfinding.MNode;
import com.minecolonies.core.entity.pathfinding.pathjobs.PathJobMoveTowards;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin( PathJobMoveTowards.class)
public abstract class PathJobMoveTowardsMixin implements AbstractPathJobAccessor{
    @Final @Shadow(remap = false) protected BlockPos target;
    @Final @Shadow(remap = false) protected int minDistance;

    @Inject(method = "computeHeuristic(III)D", at = @At("HEAD"), cancellable = true,remap = false)
    protected void computeHeuristic(int x, int y, int z, CallbackInfoReturnable<Double> cir) {
        double trueDistance = DistanceUtils.dist(x, y, z, target);
        double dist = DistanceUtils.dist2(getStartPos(), target);
        double multiplier =1+2*dist/(dist+DistanceUtils.dist2(x,y,z,getStartPos()));
        double heuristic =trueDistance * multiplier;
        cir.setReturnValue((heuristic));
    }

    /**
     * @author ARxyt
     * @reason 修改一下
     */
    @Overwrite(remap = false)
    protected boolean isAtDestination(@NotNull final MNode n)
    {
        return (DistanceUtils.dist(n.x, n.y, n.z,getStartPos()) > minDistance && DistanceUtils.manhattanDistance(n.x, n.y, n.z,target) < minDistance / 2.0) ||
                DistanceUtils.manhattanDistance(n.x, n.y, n.z,target) < 3;
    }
}
