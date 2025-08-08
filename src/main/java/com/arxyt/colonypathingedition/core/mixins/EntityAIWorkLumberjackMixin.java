package com.arxyt.colonypathingedition.core.mixins;

import com.arxyt.colonypathingedition.core.api.BuildingLumberjackExtra;
import com.arxyt.colonypathingedition.core.mixins.accessor.AbstractAISkeletonAccessor;
import com.arxyt.colonypathingedition.core.mixins.accessor.AbstractEntityAIBasicAccessor;
import com.arxyt.colonypathingedition.core.mixins.accessor.AbstractEntityAIInteractAccessor;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.inventory.InventoryCitizen;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingLumberjack;
import com.minecolonies.core.colony.jobs.JobLumberjack;
import com.minecolonies.core.entity.ai.workers.production.EntityAIWorkLumberjack;
import com.minecolonies.core.entity.ai.workers.util.Tree;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.*;
import static com.minecolonies.core.entity.ai.workers.production.EntityAIWorkLumberjack.RANGE_HORIZONTAL_PICKUP;
import static com.minecolonies.core.entity.ai.workers.production.EntityAIWorkLumberjack.RANGE_VERTICAL_PICKUP;

@Mixin(EntityAIWorkLumberjack.class)
public abstract class EntityAIWorkLumberjackMixin implements AbstractAISkeletonAccessor<JobLumberjack>,AbstractEntityAIBasicAccessor<BuildingLumberjack>, AbstractEntityAIInteractAccessor {

    @Unique BlockPos thisTree = null;
    @Unique BlockPos lastTree = null;
    @Unique int delayTimes = 0;

    /**
     * 在放置树苗前注入物品检查逻辑
     */
    @Inject(method = "placeSaplings", at = @At("HEAD"), cancellable = true, remap = false)
    private void onPlaceSaplings(
            int saplingSlot,
            @NotNull ItemStack stack,
            @NotNull Block block,
            CallbackInfo ci
    ) {
        // 访问 job 字段（通过父类 Accessor）
        Tree tree = getJob().getTree();

        // 访问 getInventory() 方法（通过父类 Invoker）
        InventoryCitizen inventory = invokeGetInventory();

        // 后续逻辑...
        assert tree != null;
        int required = tree.getStumpLocations().size();
        ItemStack targetSapling = tree.getSapling();

        // 寻找第一个存放目标树苗的槽位作为主槽位
        int mainSaplingSlot = -1;
        for (int i = 0; i < inventory.getSlots(); i++)
        {
            ItemStack stackInSlot = inventory.getStackInSlot(i);
            if (ItemStack.isSameItemSameTags(stackInSlot, targetSapling))
            {
                mainSaplingSlot = i;
                break;
            }
        }

        // 没有找到任何树苗槽位，取消补种
        if (mainSaplingSlot == -1)
        {
            ci.cancel();
            return;
        }

        // 计算总需求并尝试合并
        ItemStack mainStack = inventory.getStackInSlot(mainSaplingSlot);
        int currentCount = mainStack.getCount();
        int needed = required - currentCount;

        if (needed > 0)
        {
            // 遍历其他槽位转移树苗到主槽位
            for (int i = mainSaplingSlot+1; i < inventory.getSlots(); i++)
            {
                ItemStack stackInSlot = inventory.getStackInSlot(i);
                if (ItemStack.isSameItemSameTags(stackInSlot, targetSapling))
                {
                    int transferAmount = Math.min(needed, stackInSlot.getCount());
                    mainStack.grow(transferAmount);
                    stackInSlot.shrink(transferAmount);
                    needed -= transferAmount;

                    if (needed <= 0) break;
                }
            }

            // 最终检查数量是否足够
            if (mainStack.getCount() < required)
            {
                ci.cancel(); // 合并后仍然不足
            }
        }
    }

    /**
     * 存储当前树木位置
     */
    @Inject(method = "findTrees", at = @At("RETURN"), remap = false)
    private void storeTrees(CallbackInfoReturnable<IAIState> cir){
        if(cir.getReturnValue() == LUMBERJACK_CHOP_TREE && getJob().getTree() != null){
            ((BuildingLumberjackExtra)getBuilding()).thisTreeToLast();
            ((BuildingLumberjackExtra)getBuilding()).setThisTree(getJob().getTree().getLocation());
        }
    }

    /**
     * 补充搜索上个树木附近的掉落物
     */
    @Inject(method = "gathering", at = @At("HEAD"), remap = false)
    private void additionalGather(CallbackInfoReturnable<IAIState> cir){
        if( delayTimes == 0){
            lastTree = ((BuildingLumberjackExtra)getBuilding()).getLastTree();
            thisTree = ((BuildingLumberjackExtra)getBuilding()).getThisTree();
            if(lastTree != null){
                invokeSearchForItems(new AABB(lastTree)
                        .expandTowards(RANGE_HORIZONTAL_PICKUP * 1.5, RANGE_VERTICAL_PICKUP * 8, RANGE_HORIZONTAL_PICKUP * 1.5)
                        .expandTowards(-RANGE_HORIZONTAL_PICKUP * 1.5, -RANGE_VERTICAL_PICKUP, -RANGE_HORIZONTAL_PICKUP * 1.5));
                delayTimes = 5;
            }
            if(invokeGetItemsForPickUp() == null && thisTree != null){
                invokeSearchForItems(new AABB(thisTree)
                        .expandTowards(RANGE_HORIZONTAL_PICKUP * 1.5, RANGE_VERTICAL_PICKUP * 8, RANGE_HORIZONTAL_PICKUP * 1.5)
                        .expandTowards(-RANGE_HORIZONTAL_PICKUP * 1.5, -RANGE_VERTICAL_PICKUP, -RANGE_HORIZONTAL_PICKUP * 1.5));
                delayTimes = 5;
            }
        }
        delayTimes --;
    }

    @Inject(method = "gathering", at = @At("RETURN"), remap = false)
    private void afterGather(CallbackInfoReturnable<IAIState> cir){
        if(cir.getReturnValue() != LUMBERJACK_GATHERING ){
            delayTimes = 0;
        }
    }

    @Inject(method = "prepareForWoodcutting", at = @At("RETURN"), remap = false, cancellable = true)
    private void remasterPrepareOrderForWoodcutting(CallbackInfoReturnable<IAIState> cir){
        if(cir.getReturnValue() == LUMBERJACK_SEARCHING_TREE ){
            cir.setReturnValue(LUMBERJACK_GATHERING);
        }
    }


}
