package com.arxyt.colonypathingedition.core.mixins.netherwork;

import com.arxyt.colonypathingedition.api.AbstractEntityAIBasicExtra;
import com.minecolonies.api.colony.buildings.modules.ICraftingBuildingModule;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.requestable.Tool;
import com.minecolonies.api.crafting.IRecipeStorage;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.entity.ai.JobStatus;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.entity.ai.workers.util.GuardGear;
import com.minecolonies.api.equipment.ModEquipmentTypes;
import com.minecolonies.api.util.EntityUtils;
import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.api.util.Log;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingNetherWorker;
import com.minecolonies.core.colony.jobs.JobNetherWorker;
import com.minecolonies.core.entity.ai.workers.crafting.AbstractEntityAICrafting;
import com.minecolonies.core.entity.ai.workers.production.EntityAIWorkNether;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.*;

import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.*;

@Mixin(EntityAIWorkNether.class)
public abstract class EntityAIWorkNetherMixin extends AbstractEntityAICrafting<JobNetherWorker, BuildingNetherWorker> implements AbstractEntityAIBasicExtra {
    @Shadow(remap = false) protected abstract void checkAndRequestArmor();
    @Shadow(remap = false) protected abstract IAIState checkAndRequestFood();
    @Final @Shadow(remap = false) public List<List<GuardGear>> itemsNeeded ;
    @Final @Shadow(remap = false) private Map<EquipmentSlot, ItemStack> virtualEquipmentSlots;

    int timeOutCounter = 0;

    public EntityAIWorkNetherMixin(@NotNull JobNetherWorker job) {
        super(job);
        throw new RuntimeException("EntityAIWorkNetherMixin 类不应被实例化！");
    }

    @Override
    public boolean hasWorkToDo()
    {
        if(getState() == DECIDE){
            return super.hasWorkToDo();
        }
        return true;
    }

    private boolean checkEmptyEquipmentAvailable(List<IRequest<?>> requests){
        for (final List<GuardGear> itemList : itemsNeeded) {
            for (final GuardGear item : itemList) {
                // 如果槽位已经有装备，跳过
                if (virtualEquipmentSlots.containsKey(item.getType())
                        && !ItemStackUtils.isEmpty(virtualEquipmentSlots.get(item.getType())))
                {
                    continue;
                }

                // 检查请求列表中是否包含该物品
                boolean matched = requests.stream().anyMatch(r ->
                        r.getRequest() instanceof Tool tool && tool.getEquipmentType().equals(item.getItemNeeded())
                );

                if (!matched) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkAndRequestArmorWithAvailableCheck(){
        checkAndRequestArmor();
        List<IRequest<?>> requests = getRequestCannotBeDone().stream().filter(r ->
                          r.getRequester().getLocation().equals(building.getLocation())
                ).toList();
        return checkEmptyEquipmentAvailable(requests);
    }

    /**
     * @author ARxyt
     * @reason 重写算法，以求其能够正常穿装备/拿取食物前往下界。
     */
    @Overwrite(remap = false)
    protected IAIState decide()
    {
        //Check if we are traveling, we don't spawn an entity if we are traveling.
        if (worker.getCitizenData().getColony().getTravellingManager().isTravelling(worker.getCitizenData()) || job.isInNether())
        {
            return NETHER_AWAY;
        }

        //Now check if travelling finished.
        final Optional<BlockPos> travelingTarget = worker.getCitizenData().getColony().getTravellingManager().getTravellingTargetFor(worker.getCitizenData());
        if (travelingTarget.isPresent())
        {
            worker.getCitizenData().setNextRespawnPosition(EntityUtils.getSpawnPoint(job.getColony().getWorld(), travelingTarget.get()));
            worker.getCitizenData().updateEntityIfNecessary();
        }

        job.setInNether(false);

        IAIState crafterState = super.decide();

        if (crafterState != IDLE && crafterState != START_WORKING)
        {
            return crafterState;
        }

        // Get Armor if available.
        // This is async, but we'll wait extra time for it if it's craftable.
        boolean isArmorCraftable = !checkAndRequestArmorWithAvailableCheck();

        // Get food if available. We just ignore extra time waiting for it as armor is much more complex to craft.
        final IAIState tempState = checkAndRequestFood();
        if (tempState != getState())
        {
            return tempState;
        }

        // Check for materials needed to go to the Nether:
        IRecipeStorage rs = building.getFirstModuleOccurance(BuildingNetherWorker.CraftingModule.class).getFirstRecipe(ItemStack::isEmpty);
        boolean hasItemsAvailable = true;
        if (rs != null)
        {
            for (ItemStorage item : rs.getInput())
            {
                if (!checkIfRequestForItemExistOrCreateAsync(new ItemStack(item.getItem(), 1), item.getAmount(), item.getAmount()))
                {
                    hasItemsAvailable = false;
                }
            }
        }

        if (!hasItemsAvailable)
        {
            setDelay(60);
            return IDLE;
        }

        final BlockPos portal = building.getPortalLocation();
        if (portal == null)
        {
            Log.getLogger().warn("--- Missing Portal Tag In Nether Worker Building! Aborting Operation! ---");
            setDelay(120);
            return IDLE;
        }

        // Get other adventuring supplies. These are required.
        // Done this way to get all the requests in parallel
        boolean missingAxe = checkForToolOrWeapon(ModEquipmentTypes.axe.get());
        boolean missingPick = checkForToolOrWeapon(ModEquipmentTypes.pickaxe.get());
        boolean missingShovel = checkForToolOrWeapon(ModEquipmentTypes.shovel.get());
        boolean missingSword = checkForToolOrWeapon(ModEquipmentTypes.sword.get());
        boolean missingLighter = checkForToolOrWeapon(ModEquipmentTypes.flint_and_steel.get());
        if (missingAxe || missingPick || missingShovel || missingSword || missingLighter)
        {
            worker.getCitizenData().setJobStatus(JobStatus.STUCK);
            setDelay(60);
            return IDLE;
        }

        // We should wait for armor for extra 2 minutes if it's craftable.
        if(isArmorCraftable){
            if(timeOutCounter++ < 12){
                setDelay(200);
                return getState();
            }
        }
        else{
            timeOutCounter = 0;
        }

        if (currentRecipeStorage == null)
        {
            final ICraftingBuildingModule module = building.getFirstModuleOccurance(BuildingNetherWorker.CraftingModule.class);
            currentRecipeStorage = module.getFirstFulfillableRecipe(ItemStackUtils::isEmpty, 1, false);
            if (building.isReadyForTrip())
            {
                worker.getCitizenData().setJobStatus(JobStatus.STUCK);
            }

            if (currentRecipeStorage == null && building.shallClosePortalOnReturn())
            {
                final BlockState block = world.getBlockState(portal);
                if (block.is(Blocks.NETHER_PORTAL))
                {
                    return NETHER_CLOSEPORTAL;
                }
            }

            return getState();
        }
        else
        {
            if (!building.isReadyForTrip())
            {
                worker.getCitizenData().setJobStatus(JobStatus.IDLE);
                setDelay(120);
                return IDLE;
            }
            if (walkTo != null || !walkToBuilding())
            {
                return getState();
            }
            if (!worker.getInventoryCitizen().hasSpace())
            {
                return INVENTORY_FULL;
            }

            IAIState checkResult = checkForItems(currentRecipeStorage);

            if (checkResult == GET_RECIPE)
            {
                currentRecipeStorage = null;
                worker.getCitizenData().setJobStatus(JobStatus.STUCK);
                setDelay(60);
                return IDLE;
            }
            if (checkResult != CRAFT)
            {
                return checkResult;
            }
        }
        timeOutCounter = 0;
        return NETHER_LEAVE;
    }
}
