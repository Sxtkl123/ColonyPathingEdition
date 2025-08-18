package com.arxyt.colonypathingedition.core.mixins.lumberjack;

import com.arxyt.colonypathingedition.api.AbstractEntityAIInteractExtra;
import com.arxyt.colonypathingedition.api.workersetting.BuildingLumberjackExtra;
import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.arxyt.colonypathingedition.core.mixins.accessor.AbstractEntityAIInteractAccessor;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.inventory.InventoryCitizen;
import com.minecolonies.api.items.ModTags;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingLumberjack;
import com.minecolonies.core.colony.jobs.JobLumberjack;
import com.minecolonies.core.entity.ai.workers.crafting.AbstractEntityAICrafting;
import com.minecolonies.core.entity.ai.workers.production.EntityAIWorkLumberjack;
import com.minecolonies.core.entity.ai.workers.util.Tree;
import com.minecolonies.core.entity.pathfinding.navigation.EntityNavigationUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.*;

@Mixin(EntityAIWorkLumberjack.class)
public abstract class EntityAIWorkLumberjackMixin extends AbstractEntityAICrafting<JobLumberjack, BuildingLumberjack> implements AbstractEntityAIInteractAccessor, AbstractEntityAIInteractExtra {

    @Shadow(remap = false) protected abstract boolean mineIfEqualsBlockTag(List<BlockPos> blockPositions, TagKey<Block> tag);

    @Unique BlockPos thisTree = null;
    @Unique BlockPos lastTree = null;
    @Unique int gatherState = 0;
    @Unique BlockPos itemPos = null;

    public EntityAIWorkLumberjackMixin(@NotNull JobLumberjack job) {
        super(job);
        throw new RuntimeException("EntityAIWorkLumberjackMixin 类不应被实例化！");
    }

    @Inject(method = "findSaplingSlot", at = @At("RETURN"), cancellable = true, remap = false)
    private void findSaplingSlotAlwaysTrue(CallbackInfoReturnable<Integer> cir){
        if(PathingConfig.LUMBERJACK_PLANT_WITHOUT_SAPLINGS.get() && cir.getReturnValue() == -1){
            Tree tree = job.getTree();
            InventoryCitizen inventory = getInventory();
            assert tree != null;
            ItemStack targetSapling = tree.getSapling();
            for (int i = 0; i < inventory.getSlots(); i++) {
                ItemStack slotStack = inventory.getStackInSlot(i);
                if (slotStack.isEmpty()) {
                    inventory.insertItem(i, targetSapling, false);
                    cir.setReturnValue(i);
                    return;
                }
            }
        }
    }

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
        Tree tree = job.getTree();

        // 访问 getInventory() 方法（通过父类 Invoker）
        InventoryCitizen inventory = getInventory();

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
            if(PathingConfig.LUMBERJACK_PLANT_WITHOUT_SAPLINGS.get() && worker.getInventoryCitizen().hasSpace()){
                ItemStack sapling = new ItemStack(targetSapling.getItem(), required);
                for (int i = 0; i < inventory.getSlots(); i++) {
                    ItemStack slotStack = inventory.getStackInSlot(i);
                    if (slotStack.isEmpty()) {
                        inventory.insertItem(i, sapling, false);
                        break;
                    }
                }
            }
            else{
                ci.cancel();
                return;
            }
        }

        // 计算总需求并尝试合并
        ItemStack mainStack = inventory.getStackInSlot(mainSaplingSlot);
        int currentCount = mainStack.getCount();
        int needed = required - currentCount;

        if (needed > 0)
        {
            // 遍历其他槽位转移树苗到主槽位
            for (int i = mainSaplingSlot + 1; i < inventory.getSlots(); i++)
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
                if(PathingConfig.LUMBERJACK_PLANT_WITHOUT_SAPLINGS.get()){
                    inventory.getStackInSlot(mainSaplingSlot).setCount(required);
                }
                ci.cancel(); // 合并后仍然不足
            }
        }
    }

    /**
     * 存储当前树木位置
     */
    @Inject(method = "findTrees", at = @At("RETURN"), remap = false)
    private void storeTrees(CallbackInfoReturnable<IAIState> cir){
        if(cir.getReturnValue() == LUMBERJACK_CHOP_TREE && job.getTree() != null){
            ((BuildingLumberjackExtra)building).thisTreeToLast();
            ((BuildingLumberjackExtra)building).setThisTree(job.getTree().getLocation());
        }
    }

    /**
     * 补充搜索上个树木附近的掉落物
     */
    @Inject(method = "gathering", at = @At("HEAD"), remap = false)
    private void additionalGather(CallbackInfoReturnable<IAIState> cir){
        switch (gatherState){
            case 0: {
                lastTree = ((BuildingLumberjackExtra) building).getLastTree();
                if ((getItemsForPickUp() == null || getItemsForPickUp().isEmpty()) && lastTree != null) {
                    searchForItems(new AABB(lastTree)
                            .expandTowards(RANGE_HORIZONTAL_PICKUP * 2, RANGE_VERTICAL_PICKUP * 4, RANGE_HORIZONTAL_PICKUP * 2)
                            .expandTowards(-RANGE_HORIZONTAL_PICKUP * 2, -RANGE_VERTICAL_PICKUP, -RANGE_HORIZONTAL_PICKUP * 2));
                    gatherState = 1;
                }
            }
            case 1: {
                if(getItemsForPickUp() != null && !getItemsForPickUp().isEmpty()){
                    break;
                }
            }
            case 2: {
                thisTree = ((BuildingLumberjackExtra) building).getThisTree();
                if((getItemsForPickUp() == null || getItemsForPickUp().isEmpty()) && thisTree != null) {
                    searchForItems(new AABB(thisTree)
                            .expandTowards(RANGE_HORIZONTAL_PICKUP * 2, RANGE_VERTICAL_PICKUP * 2, RANGE_HORIZONTAL_PICKUP * 2)
                            .expandTowards(-RANGE_HORIZONTAL_PICKUP * 2, -RANGE_VERTICAL_PICKUP, -RANGE_HORIZONTAL_PICKUP * 2));
                    gatherState = 3;
                }
            }
            default: break;
        }

    }

    @Override
    public void gatherItems()
    {
        worker.setCanPickUpLoot(true);
        if (worker.getNavigation().isDone() || worker.getNavigation().getPath() == null)
        {
            final BlockPos pos = invokeGetAndRemoveClosestItemPosition();
            itemPos = pos;
            EntityNavigationUtils.walkToPos(worker, pos, 2, false);
            return;
        }

        final int currentIndex = worker.getNavigation().getPath().getNextNodeIndex();
        //We moved a bit, not stuck
        if (tryMoveForward(currentIndex))
        {
            return;
        }

        //break leaves below, as few as we can.
        if(itemPos != null) {
            List<BlockPos> BlockPosList = new ArrayList<>();
            BlockPos nowPos = itemPos;
            for(int y = nowPos.getY(); y >= worker.getBlockY(); y--){
                if(world.getBlockState(nowPos).is(BlockTags.LEAVES)){
                    BlockPosList.add(nowPos);
                    BlockPosList.add(nowPos.below());
                    break;
                }
                nowPos = nowPos.below();
            }
            if(!BlockPosList.isEmpty()){
                if(mineIfEqualsBlockTag(BlockPosList, BlockTags.LEAVES)){
                    resetStillTick();
                    return;
                }
            }
        }

        //Stuck for too long
        if (isStillTicksExceeded(PathingConfig.LUMBERJACK_GATHER_WAITING_TIME.get()))
        {
            //Skip this item
            worker.getNavigation().stop();
            if (checkPuckUpItems())
            {
                resetPickUpItems();
            }
        }
    }

    private boolean turnToCutTree = false;

    @Inject(method = "gathering", at = @At("RETURN"), remap = false, cancellable = true)
    private void afterGather(CallbackInfoReturnable<IAIState> cir){
        if(cir.getReturnValue() != LUMBERJACK_GATHERING ){
            gatherState = 0;
            if(turnToCutTree){
                turnToCutTree = false;
                cir.setReturnValue(LUMBERJACK_SEARCHING_TREE);
            }
        }
    }

    @Inject(method = "prepareForWoodcutting", at = @At("RETURN"), remap = false, cancellable = true)
    private void remasterPrepareOrderForWoodcutting(CallbackInfoReturnable<IAIState> cir){
        if(cir.getReturnValue() == LUMBERJACK_SEARCHING_TREE ){
            turnToCutTree = true;
            cir.setReturnValue(LUMBERJACK_GATHERING);
        }
    }


}
