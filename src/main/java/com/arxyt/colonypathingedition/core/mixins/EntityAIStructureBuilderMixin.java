package com.arxyt.colonypathingedition.core.mixins;

import com.minecolonies.api.util.MathUtils;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingBuilder;
import com.minecolonies.core.colony.jobs.JobBuilder;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIStructureWithWorkOrder;
import com.minecolonies.core.entity.ai.workers.builder.EntityAIStructureBuilder;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.minecolonies.api.util.constant.CitizenConstants.MIN_WORKING_RANGE;
import static com.minecolonies.api.util.constant.CitizenConstants.STANDARD_WORKING_RANGE;

@Mixin(EntityAIStructureBuilder.class)
public abstract class EntityAIStructureBuilderMixin extends AbstractEntityAIStructureWithWorkOrder<JobBuilder, BuildingBuilder> {

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

/*    @ModifyConstant(
            method = "walkToConstructionSite(Lnet/minecraft/core/BlockPos;)Z",
            constant = @Constant(doubleValue = 200.0),
            remap = false
    )
    private double modifyDropCost(double original) {
        return 1.5d;
    }*/

    /**
     * 像矿工一样一边走一边工作，而非等到到达目的地，这个步骤需要开启一个寻路代理。
     * @return 是否成功开启寻路代理
     * @author sxtkl
     * @since 2025/7/21
     */
    @Unique
    private boolean minerLikeWalk(final BlockPos currentBlock) {
        if (workFrom == null) {
            workFrom = currentBlock;
        }
        return walkWithProxy(workFrom, STANDARD_WORKING_RANGE)
                || MathUtils.twoDimDistance(worker.blockPosition(), workFrom) < MIN_WORKING_RANGE;
    }

    /**
     * 注入修改，使建筑工可以一边走一边放置方块
     * @param currentBlock 当前工作的方块位置，暂时用不到
     * @param cir 回调信息
     * @author sxtkl
     * @since 2025/7/21
     */
    @Inject(at = @At("HEAD"), method = "walkToConstructionSite", cancellable = true)
    private void injectWalkToConstructionSite(BlockPos currentBlock, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(minerLikeWalk(currentBlock));
    }

}
