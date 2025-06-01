package com.arxyt.colonypathingedition.core.mixins.food;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.interactionhandling.ChatPriority;
import com.minecolonies.api.colony.jobs.IJob;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.entity.citizen.citizenhandlers.ICitizenFoodHandler;
import com.minecolonies.api.util.BlockPosUtil;
import com.minecolonies.api.util.FoodUtils;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.MathUtils;
import com.minecolonies.api.util.constant.CitizenConstants;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingCook;
import com.minecolonies.core.colony.interactionhandling.StandardInteraction;
import com.minecolonies.core.colony.jobs.JobChef;
import com.minecolonies.core.colony.jobs.JobCook;
import com.minecolonies.core.entity.ai.minimal.EntityAIEatTask;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecolonies.core.entity.pathfinding.navigation.EntityNavigationUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.minecolonies.api.util.constant.CitizenConstants.FULL_SATURATION;
import static com.minecolonies.api.util.constant.Constants.TICKS_SECOND;
import static com.minecolonies.api.util.constant.TranslationConstants.NO_RESTAURANT;
import static com.minecolonies.core.colony.buildings.modules.BuildingModules.RESTAURANT_MENU;
import static com.minecolonies.core.entity.ai.minimal.EntityAIEatTask.EatingState.*;

@Mixin(EntityAIEatTask.class)
public abstract class EntityAIEatTaskMixin {
    @Final
    @Shadow(remap = false) private EntityCitizen citizen;

    @Shadow(remap = false)  private IBuilding restaurant;

    @Shadow(remap = false)  private BlockPos restaurantPos;

    @Shadow(remap = false) protected abstract boolean hasFood();

    @Shadow(remap = false) protected abstract void reset();

    @Inject(remap = false, method = "goToEatingPlace", at = @At("HEAD"), cancellable = true)
    private void redirectChefWaitBehavior(CallbackInfoReturnable<EntityAIEatTask.EatingState> cir) {
        ICitizenData data = citizen.getCitizenData();
        IJob<?> job = data.getJob();
        if (job instanceof JobCook || job instanceof JobChef) {
            cir.setReturnValue(EntityAIEatTask.EatingState.GET_FOOD_YOURSELF);
        }
    }

    @Unique private boolean forceEatAtHut = false;

    /**
     * @author ARxyt
     * @reason 之前的问题比较多，而且暂时没有人修改AI，直接重写比较方便
     */
    @Overwrite(remap = false)
    private EntityAIEatTask.EatingState goToHut()
    {
        final ICitizenData citizenData = citizen.getCitizenData();
        final IBuilding buildingWorker = citizenData.getWorkBuilding();
        if (buildingWorker == null)
        {
            return SEARCH_RESTAURANT;
        }
        final IColony colony = citizenData.getColony();
        final BlockPos bestRestaurantPos = colony.getBuildingManager().getBestBuilding(citizen, BuildingCook.class);
        final BlockPos citizenPos = citizen.blockPosition();
        final BlockPos buildingPos = buildingWorker.getPosition();
        // 对不在小屋附近工作的村民来说，就近吃饭可能更方便，顺便也防止触发村民在工作地点和厨房来回跑的bug,这里跳过chef,因为chef一般来说可以在自己小屋吃饭
        // 在餐厅一段时间后如果发现餐厅没有食物，会再次触发"Force Eat At Hut"状态，此时市民会无视丰富度和质量要求尝试在自己的工作岗位尝试食用一次食物，然后聚集回餐厅，并触发警告
        if ( forceEatAtHut || bestRestaurantPos == null || BlockPosUtil.dist(citizenPos,buildingPos) < BlockPosUtil.dist(citizenPos,bestRestaurantPos) || citizenData.getJob() instanceof JobChef ){
            if (EntityNavigationUtils.walkToBuilding(citizen, buildingWorker))
            {
                final ICitizenFoodHandler foodHandler = citizenData.getCitizenFoodHandler();
                final ICitizenFoodHandler.CitizenFoodStats foodStats = foodHandler.getFoodHappinessStats();
                final int diversityRequirement = FoodUtils.getMinFoodDiversityRequirement(citizenData.getHomeBuilding() == null ? 0 : citizenData.getHomeBuilding().getBuildingLevel());
                final int qualityRequirement = FoodUtils.getMinFoodQualityRequirement(citizenData.getHomeBuilding() == null ? 0 : citizenData.getHomeBuilding().getBuildingLevel());
                if( foodStats.diversity() >= diversityRequirement && foodStats.quality() >= qualityRequirement || forceEatAtHut )
                {
                    final ItemStorage storageToGet = FoodUtils.checkForFoodInBuilding(citizen.getCitizenData(), null, buildingWorker);
                    if (storageToGet != null)
                    {
                        int qty = ((int) ((FULL_SATURATION - citizen.getCitizenData().getSaturation()) / FoodUtils.getFoodValue(storageToGet.getItemStack(), citizen))) + 1;
                        InventoryUtils.transferItemStackIntoNextBestSlotInItemHandler(buildingWorker, storageToGet, qty, citizen.getInventoryCitizen());
                        if(hasFood()){
                            forceEatAtHut = false;
                            restaurant = null;
                            return EAT;
                        }
                    }
                }
                return SEARCH_RESTAURANT;
            }
            restaurant = null;
            return GO_TO_HUT;
        }
        return SEARCH_RESTAURANT;
    }

    public int STOP_EATING_SATURATION = 18;

    /**
     * @author ARxyt
     * @reason 之前的问题比较多，而且暂时没有人修改AI，直接重写比较方便
     */
    @Overwrite(remap = false)
    private EntityAIEatTask.EatingState searchRestaurant()
    {
        final ICitizenData citizenData = citizen.getCitizenData();
        final IColony colony = citizenData.getColony();
        restaurantPos = colony.getBuildingManager().getBestBuilding(citizen, BuildingCook.class);

        final IJob<?> job = citizen.getCitizenJobHandler().getColonyJob();
        if (job != null && citizenData.isWorking())
        {
            citizenData.setWorking(false);
        }

        if (restaurantPos == null)
        {
            if (citizen.getCitizenData().getSaturation() >= STOP_EATING_SATURATION)
            {
                reset();
                citizenData.setJustAte(true);
                return DONE;
            }
            citizenData.triggerInteraction(new StandardInteraction(Component.translatable(NO_RESTAURANT), ChatPriority.BLOCKING));
            return CHECK_FOR_FOOD;
        }
        return GO_TO_RESTAURANT;
    }

    /**
     * @author ARxyt
     * @reason 后面要改算法，这里先+1qty用着
     */
    @Overwrite(remap = false)
    private EntityAIEatTask.EatingState getFoodYourself()
    {
        if (restaurantPos == null)
        {
            return SEARCH_RESTAURANT;
        }

        final IColony colony = citizen.getCitizenColonyHandler().getColonyOrRegister();
        assert colony != null;
        final IBuilding cookBuilding = colony.getBuildingManager().getBuilding(restaurantPos);
        if (cookBuilding instanceof BuildingCook)
        {
            if (!EntityNavigationUtils.walkToBuilding(citizen, cookBuilding))
            {
                return GET_FOOD_YOURSELF;
            }

            final ItemStorage storageToGet = FoodUtils.checkForFoodInBuilding(citizen.getCitizenData(), cookBuilding.getModule(RESTAURANT_MENU).getMenu(), cookBuilding);
            if (storageToGet != null)
            {
                int qty = ((int) ((FULL_SATURATION - citizen.getCitizenData().getSaturation()) / FoodUtils.getFoodValue(storageToGet.getItemStack(), citizen))) + 1;
                InventoryUtils.transferItemStackIntoNextBestSlotInItemHandler(cookBuilding, storageToGet, qty, citizen.getInventoryCitizen());
                return EAT;
            }
            else{
                if(!forceEatAtHut){
                    forceEatAtHut = true;
                    return GO_TO_HUT;
                }
            }

            if (citizen.getCitizenData().getJob() instanceof JobCook jobCook && jobCook.getBuildingPos().equals(restaurantPos) && MathUtils.RANDOM.nextInt(TICKS_SECOND) <= 0)
            {
                reset();
                return DONE;
            }
        }

        return WAIT_FOR_FOOD;
    }
}
