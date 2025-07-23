package com.arxyt.colonypathingedition.core.mixins;

import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.minecolonies.api.util.MathUtils;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingBuilder;
import com.minecolonies.core.colony.jobs.JobBuilder;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIStructureWithWorkOrder;
import com.minecolonies.core.entity.ai.workers.builder.EntityAIStructureBuilder;
import com.minecolonies.core.entity.pathfinding.navigation.MinecoloniesAdvancedPathNavigate;
import com.minecolonies.core.entity.pathfinding.pathjobs.PathJobMoveCloseToXNearY;
import com.minecolonies.core.entity.pathfinding.pathresults.PathResult;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.minecolonies.api.util.constant.CitizenConstants.MIN_WORKING_RANGE;
import static com.minecolonies.api.util.constant.CitizenConstants.STANDARD_WORKING_RANGE;

@Mixin(EntityAIStructureBuilder.class)
public abstract class EntityAIStructureBuilderMixin extends AbstractEntityAIStructureWithWorkOrder<JobBuilder, BuildingBuilder> {

    @Shadow(remap = false)
    PathResult<?> gotoPath;

    /**
     * 这是为了通过 abstract 语法才创建的实例化方法，实际上任何情况下这个类都不应该被实例化。
     * @param job 工作
     * @author sxtkl
     * @since 2025/7/21
     */
    public EntityAIStructureBuilderMixin(@NotNull JobBuilder job) {
        super(job);
        throw new RuntimeException("EntityAIStructureBuilderMixin 类不应被实例化！");
    }

    @ModifyConstant(
            method = "walkToConstructionSite(Lnet/minecraft/core/BlockPos;)Z",
            constant = @Constant(doubleValue = 200.0),
            remap = false
    )
    private double modifyDropCost(double original) {
        return 1.5d;
    }

    /**
     * 像矿工一样一边走一边工作，而非等到到达目的地，这个步骤需要开启一个寻路代理。
     * @return 是否可以放置方块
     * @author sxtkl
     * @since 2025/7/21
     */
    @Unique
    private boolean formalist(final BlockPos currentBlock) {
        workFrom = currentBlock;
        return walkWithProxy(workFrom, STANDARD_WORKING_RANGE)
                || MathUtils.twoDimDistance(worker.blockPosition(), workFrom) < MIN_WORKING_RANGE;
    }

    /**
     * 像哨兵一样站在工地的某个位置开始工作。
     * @return 是否走到了工作地点
     * @author sxtkl
     * @since 2025/7/22
     */
    @Unique
    private boolean sentry(final BlockPos ignored) {
        // 有时候土木工人因为垂直位移会导致没办法继续正常干活，需要重置一下寻路，原理别问，看不懂他本来的代码。
        BlockPos workerPos = worker.getLocation().getInDimensionLocation();
        if (workFrom != null && workerPos.getX() == workFrom.getX() && workerPos.getZ() == workFrom.getZ() && workerPos.getY() != workFrom.getY()) {
            workFrom = null;
        }

        if (workFrom == null) {
            BlockPos orderLocation = job.getWorkOrder().getLocation();
            if (gotoPath == null || gotoPath.isCancelled()) {
                final PathJobMoveCloseToXNearY pathJob = new PathJobMoveCloseToXNearY(world,
                        orderLocation,
                        orderLocation,
                        4,
                        worker);
                gotoPath = ((MinecoloniesAdvancedPathNavigate) worker.getNavigation()).setPathJob(pathJob, orderLocation, 1.0, false);
                pathJob.getPathingOptions().dropCost = 1.5d;
                pathJob.extraNodes = 0;
            } else if (gotoPath.isDone()) {
                if (gotoPath.getPath() != null) {
                    workFrom = gotoPath.getPath().getTarget();
                }
                gotoPath = null;
            }
            return false;
        }

        if (!walkToSafePos(workFrom)) {
            if (worker.getNavigation() instanceof MinecoloniesAdvancedPathNavigate pathNavigate && pathNavigate.getStuckHandler().getStuckLevel() > 0) {
                workFrom = null;
            }
            return false;
        }

        return true;
    }

    /**
     * 你的建筑工人会和神一样，无视物理法则直接在小屋平地起高楼。
     * @return 只要有材料，一直都可以放置方块
     * @author sxtkl
     * @since 2025/7/22
     */
    @Unique
    private boolean god(final BlockPos ignored) {
        return true;
    }

    /**
     * 你的建筑工人会像长臂猿一样，一边在工地上蹿下跳，一边无限距离得建造，当然前提是他们在工地附近。
     * @param currentBlock 当前处理的方块
     * @return 待实现
     * @author sxtkl
     * @since 2025/7/22
     */
    @Unique
    private boolean gibbon(final BlockPos currentBlock) {
        workFrom = currentBlock;
        walkWithProxy(workFrom, STANDARD_WORKING_RANGE);
        return MathUtils.twoDimDistance(worker.blockPosition(), workFrom) < 20;
    }

    /**
     * 注入修改，使建筑工可以一边走一边放置方块
     * @param currentBlock 当前工作的方块位置，暂时用不到
     * @param cir 回调信息
     * @author sxtkl
     * @since 2025/7/21
     */
    @Inject(at = @At("HEAD"), method = "walkToConstructionSite", cancellable = true, remap = false)
    private void injectWalkToConstructionSite(BlockPos currentBlock, CallbackInfoReturnable<Boolean> cir) {
        switch (PathingConfig.BUILDER_MODE.get()) {
            case FORMALIST: cir.setReturnValue(formalist(currentBlock)); break;
            case SENTRY: cir.setReturnValue(sentry(currentBlock)); break;
            case GOD: cir.setReturnValue(god(currentBlock)); break;
            case GIBBON: cir.setReturnValue(gibbon(currentBlock)); break;
        }
    }

}
