package com.arxyt.colonypathingedition.core.mixins.concretemixer;

import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.api.util.StatsUtil;
import com.minecolonies.api.util.Tuple;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingConcreteMixer;
import com.minecolonies.core.colony.jobs.JobConcreteMixer;
import com.minecolonies.core.entity.ai.workers.crafting.AbstractEntityAICrafting;
import com.minecolonies.core.entity.ai.workers.crafting.EntityAIConcreteMixer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.GATHERING_REQUIRED_MATERIALS;
import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.START_WORKING;
import static com.minecolonies.api.util.constant.Constants.STACKSIZE;
import static com.minecolonies.api.util.constant.Constants.UPDATE_FLAG;
import static com.minecolonies.api.util.constant.StatisticsConstants.ITEMS_CRAFTED_DETAIL;

@Mixin(EntityAIConcreteMixer.class)
public abstract class EntityAIConcreteMixerMixin extends AbstractEntityAICrafting<JobConcreteMixer, BuildingConcreteMixer> {

    @Shadow(remap = false) @Final private static Predicate<ItemStack> CONCRETE;
    @Shadow(remap = false) protected abstract int getSlotWithPowder();

    @Unique BlockPos posToMine = null;

    public EntityAIConcreteMixerMixin(@NotNull final JobConcreteMixer job) {
        super(job);
    }

    Map<BlockPos, Integer> simulatedBlocks = new HashMap<>();

    /**
     * @author ARxyt
     * @reason Accelerate work by combine several action at once.
     */
    @Overwrite(remap = false)
    private IAIState placePowder()
    {
        final BlockPos posToPlace = building.getBlockToPlace();
        if (posToPlace == null)
        {
            return START_WORKING;
        }


        final int slot = getSlotWithPowder();
        if (slot == -1)
        {
            if (InventoryUtils.getCountFromBuilding(building, CONCRETE) > 0)
            {
                needsCurrently = new Tuple<>(CONCRETE, STACKSIZE);
                return GATHERING_REQUIRED_MATERIALS;
            }

            return START_WORKING;
        }

        if (!walkToWorkPos(posToPlace))
        {
            return getState();
        }

        final ItemStack stack = worker.getInventoryCitizen().getStackInSlot(slot);
        final Block block = ((BlockItem) stack.getItem()).getBlock();
        int quantity = Math.min(stack.getCount(), 1 + getSecondarySkillLevel() / 6);
        if (InventoryUtils.attemptReduceStackInItemHandler(worker.getInventoryCitizen(), stack, quantity))
        {
            simulatedBlocks.put(posToPlace,quantity);
            world.setBlock(posToPlace, block.defaultBlockState().updateShape(Direction.DOWN, block.defaultBlockState(), world, posToPlace, posToPlace), UPDATE_FLAG);
        }

        return getState();
    }

    /**
     * @author ARxyt
     * @reason Accelerate work by combine several action at once.
     */
    @Overwrite(remap = false)
    private IAIState harvestConcrete()
    {
        posToMine = building.getBlockToMine();
        if (posToMine == null)
        {
            this.resetActionsDone();
            return START_WORKING;
        }

        if (!walkToWorkPos(posToMine))
        {
            return getState();
        }

        final BlockState blockToMine = world.getBlockState(posToMine);
        if (mineBlock(posToMine))
        {
            int multiplier = simulatedBlocks.getOrDefault(posToMine, 1);

            StatsUtil.trackStatByName(building, ITEMS_CRAFTED_DETAIL, blockToMine.getBlock().getDescriptionId(), multiplier);
            if (currentRequest != null && currentRecipeStorage != null && blockToMine.getBlock().asItem().equals(currentRecipeStorage.getPrimaryOutput().getItem()))
            {
                currentRequest.addDelivery(new ItemStack(blockToMine.getBlock(), multiplier));
                job.setCraftCounter(job.getCraftCounter() + multiplier);
                if (job.getCraftCounter() >= job.getMaxCraftingCount())
                {
                    incrementActionsDone(getActionRewardForCraftingSuccess());
                    worker.decreaseSaturationForAction();
                    job.finishRequest(true);
                    worker.getCitizenExperienceHandler().addExperience(currentRequest.getRequest().getCount() / 2.0);
                    currentRequest = null;
                    currentRecipeStorage = null;
                    resetValues();

                    if (inventoryNeedsDump() && job.getMaxCraftingCount() == 0 && job.getProgress() == 0 && job.getCraftCounter() == 0 && currentRequest != null)
                    {
                        worker.getCitizenExperienceHandler().addExperience(currentRequest.getRequest().getCount() / 2.0);
                    }

                    return START_WORKING;
                }
            }
        }

        return getState();
    }

    @Override
    protected List<ItemStack> increaseBlockDrops(final List<ItemStack> drops)
    {
        int multiplier = simulatedBlocks.getOrDefault(posToMine, 1);

        for (ItemStack stack : drops) {
            if (!stack.isEmpty()) {
                stack.setCount(stack.getCount() * multiplier);
            }
        }

        return drops;
    }

}
