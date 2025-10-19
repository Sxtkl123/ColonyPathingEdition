package com.arxyt.colonypathingedition.core.mixins.farm;

import com.arxyt.colonypathingedition.api.FarmFieldExtra;
import com.arxyt.colonypathingedition.core.mixins.accessor.AbstractAISkeletonAccessor;
import com.arxyt.colonypathingedition.core.mixins.accessor.AbstractEntityAIBasicAccessor;
import com.arxyt.colonypathingedition.core.tag.ModTag;
import com.google.common.reflect.TypeToken;
import com.minecolonies.api.advancements.AdvancementTriggers;
import com.minecolonies.api.colony.buildingextensions.IBuildingExtension;
import com.minecolonies.api.colony.interactionhandling.ChatPriority;
import com.minecolonies.api.colony.requestsystem.requestable.StackList;
import com.minecolonies.api.entity.ai.JobStatus;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.entity.citizen.VisibleCitizenStatus;
import com.minecolonies.api.equipment.ModEquipmentTypes;
import com.minecolonies.api.items.ModItems;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.Log;
import com.minecolonies.api.util.Tuple;
import com.minecolonies.api.util.constant.translation.RequestSystemTranslationConstants;
import com.minecolonies.core.Network;
import com.minecolonies.core.colony.buildingextensions.FarmField;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.colony.buildings.modules.BuildingExtensionsModule;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingFarmer;
import com.minecolonies.core.colony.interactionhandling.StandardInteraction;
import com.minecolonies.core.colony.jobs.JobFarmer;
import com.minecolonies.core.entity.ai.workers.crafting.AbstractEntityAICrafting;
import com.minecolonies.core.entity.ai.workers.production.agriculture.EntityAIWorkFarmer;
import com.minecolonies.core.network.messages.client.CompostParticleMessage;
import com.minecolonies.core.util.AdvancementUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.*;
import static com.minecolonies.api.util.constant.CitizenConstants.BLOCK_BREAK_SOUND_RANGE;
import static com.minecolonies.api.util.constant.Constants.STACKSIZE;
import static com.minecolonies.api.util.constant.TranslationConstants.NO_FREE_FIELDS;


@Mixin(EntityAIWorkFarmer.class)
public abstract class EntityAIWorkFarmerMixin extends AbstractEntityAICrafting<JobFarmer, BuildingFarmer> implements AbstractAISkeletonAccessor<JobFarmer>, AbstractEntityAIBasicAccessor<AbstractBuilding> {

    @Shadow(remap = false) protected abstract BlockPos getSurfacePos(final BlockPos position);
    @Shadow(remap = false) protected abstract boolean isCompost(final ItemStack itemStack);

    @Shadow(remap = false) protected abstract IAIState canGoPlanting(@NotNull FarmField farmField);

    @Shadow(remap = false) private int skippedState;

    @Shadow(remap = false) private boolean didWork;

    @Shadow(remap = false) @Final private static VisibleCitizenStatus FARMING_ICON;

    @Shadow(remap = false) protected abstract boolean checkIfShouldExecute(@NotNull FarmField farmField, @NotNull Predicate<BlockPos> predicate);

    @Shadow(remap = false) protected abstract BlockPos findHarvestableSurface(@NotNull BlockPos position);

    @Shadow(remap = false) protected abstract BlockPos findHoeableSurface(@NotNull BlockPos position, @NotNull FarmField farmField);

    /**
     * Methods to clarify special seed by Tags
     */
    @Unique private static boolean isUnderWater(@NotNull ItemStack stack) {
        return stack.is(ModTag.SEEDS_UNDERWATER);
    }
    @Unique private static boolean isNoFarmland(@NotNull ItemStack stack) {
        return stack.is(ModTag.SEEDS_NOFARMLAND);   //目前暂未对相关内容进行处理，预计过几个版本后会尝试完成对浆果类的作物的适配。
    }

    /**
     * Bonemeal target check with fewer param
     */
    @Unique private boolean isBoneMealAble(BlockPos position,BonemealableBlock bonemealable) {
        BlockState state = getWorld().getBlockState(position);
        return bonemealable.isValidBonemealTarget(
                getWorld(),
                position,
                state,
                false // pIsClient 应为 false（服务端逻辑）
        );
    }

    public EntityAIWorkFarmerMixin(@NotNull final JobFarmer job)
    {
        super(job);
    }

    @Inject(method = "prepareForFarming",at = @At("HEAD"),remap = false,cancellable = true)
    private void remasteredPrepareForFarming(CallbackInfoReturnable<IAIState> cir){
        worker.getCitizenData().setJobStatus(JobStatus.IDLE);
        if (building == null || building.getBuildingLevel() < 1)
        {
            worker.getCitizenData().setJobStatus(JobStatus.STUCK);
            cir.setReturnValue(PREPARING);
            return;
        }

        final BuildingExtensionsModule module = building.getFirstModuleOccurance(BuildingExtensionsModule.class);
        if (module.getOwnedExtensions().size() == building.getMaxBuildingLevel())
        {
            AdvancementUtils.TriggerAdvancementPlayersForColony(building.getColony(), AdvancementTriggers.MAX_FIELDS::trigger);
        }

        final int amountOfCompostInBuilding = InventoryUtils.hasBuildingEnoughElseCount(building, this::isCompost, 1);
        final int amountOfCompostInInv = InventoryUtils.getItemCountInItemHandler(worker.getInventoryCitizen(), this::isCompost);

        if (amountOfCompostInBuilding + amountOfCompostInInv <= 0)
        {
            if (building.requestFertilizer() && !building.hasWorkerOpenRequestsOfType(worker.getCitizenData().getId(), TypeToken.of(StackList.class)))
            {
                final List<ItemStack> compostAbleItems = new ArrayList<>();
                compostAbleItems.add(new ItemStack(ModItems.compost, 1));
                compostAbleItems.add(new ItemStack(Items.BONE_MEAL, 1));
                worker.getCitizenData().createRequestAsync(new StackList(compostAbleItems, RequestSystemTranslationConstants.REQUEST_TYPE_FERTILIZER, STACKSIZE, 1));
            }
        }
        else if (amountOfCompostInInv <= 0 && amountOfCompostInBuilding > 0)
        {
            needsCurrently = new Tuple<>(this::isCompost, STACKSIZE);
            cir.setReturnValue(GATHERING_REQUIRED_MATERIALS);
            return;
        }

        if (module.hasNoExtensions())
        {
            if (worker.getCitizenData() != null)
            {
                worker.getCitizenData().triggerInteraction(new StandardInteraction(Component.translatable(NO_FREE_FIELDS), ChatPriority.BLOCKING));
            }
            worker.getCitizenData().setJobStatus(JobStatus.STUCK);
            cir.setReturnValue(IDLE);
            return;
        }

        final IBuildingExtension fieldToWork = module.getExtensionToWorkOn();
        if (fieldToWork instanceof FarmField farmField)
        {
            if (checkForToolOrWeapon(ModEquipmentTypes.hoe.get()))
            {
                worker.getCitizenData().setJobStatus(JobStatus.STUCK);
                cir.setReturnValue(PREPARING);
            }
            worker.getCitizenData().setVisibleStatus(FARMING_ICON);
            worker.getCitizenData().setJobStatus(JobStatus.WORKING);
            if (farmField.getFieldStage() == FarmField.Stage.PLANTED && checkIfShouldExecute(farmField, pos -> this.findHarvestableSurface(pos) != null))
            {
                cir.setReturnValue(FARMER_HARVEST);
                return;
            }
            else if (farmField.getFieldStage() == FarmField.Stage.HOED)
            {
                cir.setReturnValue(canGoPlanting(farmField));
                return;
            }
            else if (farmField.getFieldStage() == FarmField.Stage.EMPTY && checkIfShouldExecute(farmField, pos -> this.findHoeableSurface(pos, farmField) != null))
            {
                cir.setReturnValue(FARMER_HOE);
                return;
            }
            farmField.nextState();
            if (++skippedState >= 4)
            {
                skippedState = 0;
                didWork = true;
                module.resetCurrentExtension();
            }
            cir.setReturnValue(IDLE);
            return;
        }
        else if (fieldToWork != null)
        {
            Log.getLogger().warn("Farmer found non-FarmField extension: {}", fieldToWork.getClass());
        }
        cir.setReturnValue(IDLE);
    }


    /**
     * Skip hoeing if seeds plant directly on dirt.
     */
    @Inject(
            method = "findHoeableSurface",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void noNeedToHoeSurface(BlockPos position, @NotNull FarmField farmField, CallbackInfoReturnable<Boolean> cir) {
        ItemStack seed = farmField.getSeed();
        // 若种子标记为无需耕地，直接跳过锄地
        if (isUnderWater(seed)||isNoFarmland(seed)) {
            cir.setReturnValue(null);
        }
    }

    /**
     * Skip creating farmland if seeds plant directly on dirt.
     */
    @Inject(
            method = "createCorrectFarmlandForSeed",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void onCreateFarmland(ItemStack seed, BlockPos pos, CallbackInfo ci) {
        if (isUnderWater(seed)||isNoFarmland(seed)) {
            ci.cancel(); // 跳过原方法（不创建耕地）
        }
    }

    /**
     * Add special check for special seeds.
     */
    @Inject(
            method = "isRightFarmLandForCrop",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void onIsRightFarmLandForCrop(@NotNull FarmField farmField, BlockState blockState, CallbackInfoReturnable<Boolean> cir) {
        ItemStack seed = farmField.getSeed();
        if (isUnderWater(seed)) {
            if (blockState.is(Blocks.WATER)) {
                cir.setReturnValue(true);
            } else {
                cir.setReturnValue(false);
            }
            return;
        }
        if (isNoFarmland(seed) && blockState.is(Blocks.AIR)) {
            cir.setReturnValue(true);
        }
    }

    /**
     * Add special check for special seeds.
     */
    @Inject(
            method = "findPlantableSurface",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void onFindPlantableSurface(@NotNull BlockPos position, @NotNull FarmField farmField, CallbackInfoReturnable<BlockPos> cir) {
        ItemStack seed = farmField.getSeed();
        if (isUnderWater(seed)) {
            position = getSurfacePos(position);
            if (position == null
                    || (!getWorld().getBlockState(position).is(Blocks.WATER) && !(getWorld().getBlockState(position).getBlock() instanceof BushBlock))
            )
            {
                cir.setReturnValue(null);
                return;
            }
            cir.setReturnValue(position.below());
        }
    }

    /**
     * Special plant action for special seeds.
     */
    @Inject(
            method = "plantCrop",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void onPlantCrop(ItemStack item, BlockPos position, CallbackInfoReturnable<Boolean> cir) {
        if (isUnderWater(item)) {
            BlockPos cropPos = position.above();
            BlockState belowState = getWorld().getBlockState(position);
            if (belowState.is(Blocks.DIRT)) {
                // 查找物品栏中的种子槽位
                final int slot = getWorker().getCitizenInventoryHandler().findFirstSlotInInventoryWith(item.getItem());
                if (slot == -1) {
                    cir.setReturnValue(false); // 无种子时返回失败
                    return;
                }
                if (item.getItem() instanceof BlockItem) {
                    // 种植并消耗种子
                    @NotNull final Item seed = item.getItem();
                    getWorld().setBlockAndUpdate(cropPos, ((BlockItem) seed).getBlock().defaultBlockState());
                    invokeGetInventory().extractItem(slot, 1, false); // 手动消耗种子
                    getWorker().decreaseSaturationForContinuousAction(); // 减少饱食度
                    cir.setReturnValue(true);
                    return;
                }
                cir.setReturnValue(true);
                return;
            }
            cir.setReturnValue(true);
        }
    }

    /**
     * Special harvest action for special seeds.
     */
    @Inject(
            method = "findHarvestableSurface",
            at = @At(
                    value = "TAIL"
            ),
            cancellable = true,
            remap = false
    )
    private void onFinalReturnCheck(BlockPos position, CallbackInfoReturnable<BlockPos> cir) {
        if( cir.getReturnValue() != null ){
            return;
        }
        BlockState surfaceState = getWorld().getBlockState(position.above());
        Block surfaceBlock = surfaceState.getBlock();
        if (surfaceBlock instanceof BushBlock){
            // Not valid bonemeal target means crops are mature.
            if (surfaceBlock instanceof BonemealableBlock bonemealable) {
                Block upWaterBlock = getWorld().getBlockState(position.above().above()).getBlock();
                if (!isBoneMealAble(position.above(),bonemealable)) {
                    // harvest above for rice, reduce steps to grow.
                    cir.setReturnValue(upWaterBlock instanceof BushBlock ? position.above() : position);
                    return;
                }
                final int amountOfCompostInInv = InventoryUtils.getItemCountInItemHandler(getWorker().getInventoryCitizen(), this::isCompost);
                if (amountOfCompostInInv == 0)
                {
                    cir.setReturnValue(null);
                    return;
                }

                if (InventoryUtils.shrinkItemCountInItemHandler(getWorker().getInventoryCitizen(), this::isCompost))
                {
                    Network.getNetwork().sendToPosition(new CompostParticleMessage(position.above()),
                            new PacketDistributor.TargetPoint(position.getX(), position.getY(), position.getZ(), BLOCK_BREAK_SOUND_RANGE, getWorld().dimension()));
                    bonemealable.performBonemeal((ServerLevel)getWorld(), getWorld().getRandom(), position.above(), surfaceState);
                    surfaceState = getWorld().getBlockState(position.above());
                    surfaceBlock = surfaceState.getBlock();
                    if (!(surfaceBlock instanceof BushBlock && surfaceBlock instanceof BonemealableBlock))
                    {
                        cir.setReturnValue(null);
                        return;
                    }
                    bonemealable = (BonemealableBlock) surfaceBlock;
                    // harvest above for rice, reduce steps to grow.
                    cir.setReturnValue(isBoneMealAble(position.above(),bonemealable) ? null : upWaterBlock instanceof BushBlock ? position.above() : position);
                    return;
                }
            }
            cir.setReturnValue(null);
        }
    }
}
