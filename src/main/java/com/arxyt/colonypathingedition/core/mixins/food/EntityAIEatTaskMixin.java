package com.arxyt.colonypathingedition.core.mixins.food;

import com.arxyt.colonypathingedition.core.api.BuildingCookExtra;
import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.interactionhandling.ChatPriority;
import com.minecolonies.api.colony.jobs.IJob;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.entity.citizen.citizenhandlers.ICitizenFoodHandler;
import com.minecolonies.api.util.*;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingCook;
import com.minecolonies.core.colony.interactionhandling.StandardInteraction;
import com.minecolonies.core.colony.jobs.JobCook;
import com.minecolonies.core.entity.ai.minimal.EntityAIEatTask;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecolonies.core.entity.other.SittingEntity;
import com.minecolonies.core.entity.pathfinding.navigation.EntityNavigationUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.spongepowered.asm.mixin.*;

import java.util.Objects;

import static com.minecolonies.api.util.constant.CitizenConstants.FULL_SATURATION;
import static com.minecolonies.api.util.constant.CitizenConstants.NIGHT;
import static com.minecolonies.api.util.constant.Constants.SECONDS_A_MINUTE;
import static com.minecolonies.api.util.constant.Constants.TICKS_SECOND;
import static com.minecolonies.api.util.constant.TranslationConstants.NO_RESTAURANT;
import static com.minecolonies.core.colony.buildings.modules.BuildingModules.RESTAURANT_MENU;
import static com.minecolonies.core.entity.ai.minimal.EntityAIEatTask.EatingState.*;
import static com.arxyt.colonypathingedition.core.costants.AdditionalContants.JOBS_EAT_IMMEDIATELY;

@Mixin(EntityAIEatTask.class)
public abstract class EntityAIEatTaskMixin {
    @Final @Shadow(remap = false) private EntityCitizen citizen;
    @Shadow(remap = false) private IBuilding restaurant;
    @Shadow(remap = false) private BlockPos restaurantPos;
    @Shadow(remap = false) private BlockPos eatPos;
    @Shadow(remap = false) private int timeOutWalking;
    @Shadow(remap = false) private int waitingTicks;

    @Shadow(remap = false) protected abstract boolean hasFood();
    @Shadow(remap = false) protected abstract void reset();

    @Unique private boolean forceEatAtHut = false;
    @Unique public int STOP_EATING_SATURATION = 18;

    @Unique private final double WAITING_MINUTES = PathingConfig.RESTAURANT_WAITING_TIME.get();

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
        // 在餐厅一段时间后如果发现餐厅没有食物，会再次触发"Force Eat At Hut"状态，此时市民会无视丰富度和质量要求尝试在自己的工作岗位尝试食用一次食物，然后聚集回餐厅，并触发警告(警告暂时没做)
        if ( forceEatAtHut || bestRestaurantPos == null || BlockPosUtil.dist(citizenPos,buildingPos) < BlockPosUtil.dist(citizenPos,bestRestaurantPos) || (citizenData.getJob() != null && JOBS_EAT_IMMEDIATELY.contains(citizenData.getJob().getClass()))){
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
            MobEffectInstance effectInstance = citizen.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
            if(effectInstance != null){
                citizen.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,effectInstance.getDuration(),3));
            }
            restaurant = null;
            return GO_TO_HUT;
        }
        return SEARCH_RESTAURANT;
    }

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
     * @reason 之前的问题比较多，而且暂时没有人修改AI，直接重写比较方便
     */
    @Overwrite(remap = false)
    private EntityAIEatTask.EatingState goToRestaurant()
    {
        if (restaurantPos != null)
        {
            final IBuilding building = Objects.requireNonNull(citizen.getCitizenColonyHandler().getColonyOrRegister()).getBuildingManager().getBuilding(restaurantPos);
            if (building != null)
            {
                if (building.isInBuilding(citizen.blockPosition()))
                {
                    ((BuildingCookExtra)building).tryRegisterCustomer(citizen.getCivilianID());
                    return WAIT_FOR_FOOD;
                }
                else if (!EntityNavigationUtils.walkToBuilding(citizen, building))
                {
                    MobEffectInstance effectInstance = citizen.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
                    if(effectInstance != null){
                        citizen.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,effectInstance.getDuration(),3));
                    }
                    return GO_TO_RESTAURANT;
                }
            }
        }
        return SEARCH_RESTAURANT;
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

    /**
     * @author ARxyt
     * @reason 函数太短了，还是重写方便，这是一个防止村民走到餐厅外的地方等待送餐的修改
     */
    @Overwrite(remap = false)
    private BlockPos findPlaceToEat()
    {
        if (restaurantPos != null)
        {
            final IBuilding restaurant = citizen.getCitizenData().getColony().getBuildingManager().getBuilding(restaurantPos);
            if (restaurant instanceof BuildingCook)
            {
                final BlockPos sitting = ((BuildingCook) restaurant).getNextSittingPosition();
                if(restaurant.isInBuilding(sitting)){
                    return sitting;
                }
            }
        }
        return null;
    }

    /**
     * @author ARxyt
     * @reason 要改好多地方，重写了吧
     */
    @Overwrite(remap = false)
    private EntityAIEatTask.EatingState goToEatingPlace()
    {
        IJob<?> jobCitizen = citizen.getCitizenData().getJob();
        BuildingCookExtra restaurantExtra = ((BuildingCookExtra)restaurant);
        if(!restaurantExtra.checkCustomerRegistry(citizen.getCivilianID()) || !WorldUtil.isPastTime(citizen.level(), NIGHT - 2100) || (jobCitizen != null && JOBS_EAT_IMMEDIATELY.contains(jobCitizen.getClass())) || eatPos == null){
            restaurantExtra.deleteCustomer(citizen.getCivilianID());
            return GET_FOOD_YOURSELF;
        }

        if ( timeOutWalking++ > 400 )
        {
            if (hasFood())
            {
                timeOutWalking = 0;
                restaurantExtra.deleteCustomer(citizen.getCivilianID());
                return EAT;
            }
            else {
                restaurantExtra.deleteCustomer(citizen.getCivilianID());
                return GET_FOOD_YOURSELF;
            }
        }

        if (EntityNavigationUtils.walkToPos(citizen, eatPos, 2, true))
        {
            SittingEntity.sitDown(eatPos, citizen, TICKS_SECOND * SECONDS_A_MINUTE);
            if (!hasFood())
            {
                waitingTicks++;
                if (waitingTicks > SECONDS_A_MINUTE * WAITING_MINUTES)
                {
                    waitingTicks = 0;
                    restaurantExtra.deleteCustomer(citizen.getCivilianID());
                    return GET_FOOD_YOURSELF;
                }
            }
            else {
                timeOutWalking = 0;
                restaurantExtra.deleteCustomer(citizen.getCivilianID());
                return EAT;
            }
        }
        return GO_TO_EAT_POS;
    }
}
