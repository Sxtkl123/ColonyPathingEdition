package com.arxyt.colonypathingedition.core.mixins;

import com.arxyt.colonypathingedition.core.mixins.accessor.AbstractAISkeletonAccessor;
import com.arxyt.colonypathingedition.core.mixins.accessor.AbstractEntityAIBasicAccessor;
import com.minecolonies.api.inventory.InventoryCitizen;
import com.minecolonies.core.colony.jobs.JobLumberjack;
import com.minecolonies.core.entity.ai.workers.production.EntityAIWorkLumberjack;
import com.minecolonies.core.entity.ai.workers.util.Tree;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityAIWorkLumberjack.class)
public abstract class EntityAIWorkLumberjackMixin implements AbstractAISkeletonAccessor<JobLumberjack>,AbstractEntityAIBasicAccessor {
    /**
     * 在放置树苗前注入物品检查逻辑
     */
    @Inject(
            method = "placeSaplings",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
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
}
