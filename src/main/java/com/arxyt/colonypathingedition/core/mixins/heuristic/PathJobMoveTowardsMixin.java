package com.arxyt.colonypathingedition.core.mixins.heuristic;

import com.arxyt.colonypathingedition.core.util.DistanceUtils;
import com.minecolonies.core.entity.pathfinding.pathjobs.PathJobMoveTowards;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin( PathJobMoveTowards.class)
public class PathJobMoveTowardsMixin {
    @Final
    @Shadow(remap = false)
    protected BlockPos target;

    @Inject(method = "computeHeuristic(III)D", at = @At("HEAD"), cancellable = true,remap = false)
    protected void computeHeuristic(int x, int y, int z, CallbackInfoReturnable<Double> cir) {
        double heuristic = DistanceUtils.dist(x, y, z, target) ;
        cir.setReturnValue((heuristic * heuristic + 99 * heuristic) / 100);
    }
}
