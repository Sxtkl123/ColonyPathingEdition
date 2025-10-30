package com.arxyt.colonypathingedition.mixins.minecolonies.farm;

import com.arxyt.colonypathingedition.core.data.farmlandmap.SpecialSeedManager;
import com.arxyt.colonypathingedition.core.data.tag.ModTag;
import com.arxyt.colonypathingedition.mixins.minecolonies.accessor.AbstractAISkeletonAccessor;
import com.arxyt.colonypathingedition.mixins.minecolonies.accessor.AbstractEntityAIBasicAccessor;
import com.google.common.reflect.TypeToken;
import com.minecolonies.api.advancements.AdvancementTriggers;
import com.minecolonies.api.colony.buildingextensions.IBuildingExtension;
import com.minecolonies.api.colony.interactionhandling.ChatPriority;
import com.minecolonies.api.colony.requestsystem.requestable.StackList;
import com.minecolonies.api.entity.ai.JobStatus;
import com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.entity.citizen.VisibleCitizenStatus;
import com.minecolonies.api.equipment.ModEquipmentTypes;
import com.minecolonies.api.items.ModItems;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.Log;
import com.minecolonies.api.util.Tuple;
import com.minecolonies.api.util.constant.translation.RequestSystemTranslationConstants;
import com.minecolonies.core.Network;
import com.minecolonies.core.blocks.BlockScarecrow;
import com.minecolonies.core.blocks.MinecoloniesCropBlock;
import com.minecolonies.core.blocks.MinecoloniesFarmland;
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
import com.minecolonies.core.util.citizenutils.CitizenItemUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
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
import static com.minecolonies.api.util.constant.EquipmentLevelConstants.TOOL_LEVEL_WOOD_OR_GOLD;
import static com.minecolonies.api.util.constant.StatisticsConstants.LAND_TILLED;
import static com.minecolonies.api.util.constant.TranslationConstants.NO_FREE_FIELDS;


@Mixin(EntityAIWorkFarmer.class)
public abstract class EntityAIWorkFarmerMixin extends AbstractEntityAICrafting<JobFarmer, BuildingFarmer> implements AbstractAISkeletonAccessor<JobFarmer>, AbstractEntityAIBasicAccessor<AbstractBuilding> {
    @Shadow(remap = false) private int skippedState;
    @Shadow(remap = false) private boolean didWork;
    @Shadow(remap = false) private boolean shouldDumpInventory;

    @Final @Shadow(remap = false) private static VisibleCitizenStatus FARMING_ICON;

    @Shadow(remap = false) protected abstract BlockPos getSurfacePos(final BlockPos position);
    @Shadow(remap = false) protected abstract boolean isCompost(final ItemStack itemStack);
    @Shadow(remap = false) protected abstract IAIState canGoPlanting(@NotNull FarmField farmField);
    @Shadow(remap = false) protected abstract boolean checkIfShouldExecute(@NotNull FarmField farmField, @NotNull Predicate<BlockPos> predicate);
    @Shadow(remap = false) protected abstract BlockPos findHarvestableSurface(@NotNull BlockPos position);
    @Shadow(remap = false) protected abstract boolean harvestIfAble(BlockPos position);
    @Shadow(remap = false) protected abstract int getLevelDelay();
    @Shadow(remap = false) protected abstract BlockPos nextValidCell(FarmField farmField);
    @Shadow(remap = false) protected abstract boolean isRightFarmLandForCrop(FarmField farmField, BlockState blockState);
    @Shadow(remap = false) protected abstract void equipHoe();
    @Shadow(remap = false) protected abstract void createCorrectFarmlandForSeed(ItemStack seed, BlockPos pos);

    /**
     * Methods to clarify special seed by Tags
     */
    @Unique private static boolean isUnderWater(@NotNull ItemStack stack) {
        return stack.is(ModTag.SEEDS_UNDERWATER);
    }
    @Unique private static boolean isNoFarmland(@NotNull ItemStack stack) {
        return stack.is(ModTag.SEEDS_NOFARMLAND);
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

    private AIWorkerState defaultState;

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
            if(isMissingFarmland && SpecialSeedManager.isSpecialSeed(farmField.getSeed().getItem())){
                final Block farmland = SpecialSeedManager.getRequiredSoil(farmField.getSeed().getItem());
                Item itemFarmland = Item.BY_BLOCK.get(farmland);
                final int amountOfFarmlandInBuilding = InventoryUtils.hasBuildingEnoughElseCount(building, itemStack -> isItemOfFarmland(itemStack,farmland), 0);
                final int amountOfFarmlandInInv = InventoryUtils.getItemCountInItemHandler(getWorker().getInventoryCitizen(), itemStack -> isItemOfFarmland(itemStack,farmland));
                if (amountOfFarmlandInInv + amountOfFarmlandInBuilding <= 8)
                {
                    ItemStack stack = new ItemStack(itemFarmland);
                    stack.setCount(stack.getMaxStackSize());
                    checkIfRequestForItemExistOrCreateAsync(stack, stack.getMaxStackSize(), 1);
                }
                else if (amountOfFarmlandInInv <= 0 && amountOfFarmlandInBuilding > 0)
                {
                    needsCurrently = new Tuple<>(itemStack -> isItemOfFarmland(itemStack,farmland), STACKSIZE);
                    isMissingFarmland = false;
                    cir.setReturnValue(GATHERING_REQUIRED_MATERIALS);
                    return;
                }
            }

            if (checkForToolOrWeapon(ModEquipmentTypes.hoe.get()))
            {
                worker.getCitizenData().setJobStatus(JobStatus.STUCK);
                cir.setReturnValue(PREPARING);
                return;
            }
            ItemStack seeds = farmField.getSeed();
            final int count = worker.getCitizenInventoryHandler().getItemCountInInventory(seeds.getItem());
            if (count < seeds.getMaxStackSize() / 2)
            {
                seeds.setCount(seeds.getMaxStackSize());
                checkIfRequestForItemExistOrCreateAsync(seeds, seeds.getMaxStackSize(), 1);
            }
            worker.getCitizenData().setVisibleStatus(FARMING_ICON);
            worker.getCitizenData().setJobStatus(JobStatus.WORKING);
            FarmField.Stage stage = farmField.getFieldStage();
            switch (stage) {
                case EMPTY : {
                    if (checkIfShouldExecute(farmField, pos -> this.newFindHoeableSurface(pos, farmField) != null)) {
                        if(SpecialSeedManager.isSpecialSeed(farmField.getSeed().getItem())){
                            final Block farmland = SpecialSeedManager.getRequiredSoil(farmField.getSeed().getItem());
                            Item itemFarmland = Item.BY_BLOCK.get(farmland);
                            final int amountOfFarmlandInBuilding = InventoryUtils.hasBuildingEnoughElseCount(building, itemStack -> isItemOfFarmland(itemStack,farmland), 0);
                            final int amountOfFarmlandInInv = InventoryUtils.getItemCountInItemHandler(getWorker().getInventoryCitizen(), itemStack -> isItemOfFarmland(itemStack,farmland));
                            if (amountOfFarmlandInInv + amountOfFarmlandInBuilding <= 8)
                            {
                                ItemStack stack = new ItemStack(itemFarmland);
                                stack.setCount(stack.getMaxStackSize());
                                checkIfRequestForItemExistOrCreateAsync(stack, stack.getMaxStackSize(), 1);
                            }
                            else if (amountOfFarmlandInInv <= 0 && amountOfFarmlandInBuilding > 0)
                            {
                                needsCurrently = new Tuple<>(itemStack -> isItemOfFarmland(itemStack,farmland), STACKSIZE);
                                cir.setReturnValue(GATHERING_REQUIRED_MATERIALS);
                                return;
                            }
                        }
                        defaultState = FARMER_HOE;
                        cir.setReturnValue(FARMER_HOE);
                        return;
                    }
                    farmField.nextState();
                    // continue
                }
                case HOED : // continue
                case PLANTED : {
                    if (checkIfShouldExecute(farmField, pos -> this.findHarvestableSurface(pos) != null)){
                        defaultState = FARMER_HARVEST;
                        cir.setReturnValue(FARMER_HARVEST);
                        return;
                    }
                    if (farmField.getFieldStage() == FarmField.Stage.HOED)
                    {
                        defaultState = FARMER_PLANT;
                        cir.setReturnValue(canGoPlanting(farmField));
                        return;
                    }
                    farmField.nextState();
                }
                default : // no action
            }
            module.resetCurrentExtension();
            cir.setReturnValue(IDLE);
            return;
        }
        else if (fieldToWork != null)
        {
            Log.getLogger().warn("Farmer found non-FarmField extension: {}", fieldToWork.getClass());
        }
        cir.setReturnValue(IDLE);
    }

    private boolean isMissingFarmland = false;

    @Inject(method = "workAtField", at = @At("HEAD"), remap = false, cancellable = true)
    private void remasteredWorkAtField(CallbackInfoReturnable<IAIState> cir){
        final BuildingExtensionsModule module = building.getFirstModuleOccurance(BuildingExtensionsModule.class);
        final IBuildingExtension field = module.getCurrentExtension();

        worker.getCitizenData().setVisibleStatus(FARMING_ICON);
        if (field instanceof FarmField farmField)
        {
            if (building.getWorkingOffset() != null)
            {
                final BlockPos position = farmField.getPosition().below().south(building.getWorkingOffset().getZ()).east(building.getWorkingOffset().getX());

                // Still moving to the block
                if (!walkToSafePos(position.above()))
                {
                    cir.setReturnValue(getState());
                    return;
                }

                switch ((AIWorkerState) getState())
                {
                    case FARMER_HARVEST :
                    {
                        BlockPos pos = findHarvestableSurface(position);
                        if (pos != null) {
                            if (newHarvestIfAble(pos,farmField)) {
                                didWork = true;
                                cir.setReturnValue(FARMER_HOE);
                                return;
                            }
                            if (harvestIfAble(position)) {
                                didWork = true;
                                cir.setReturnValue(FARMER_HOE);
                            } else {
                                cir.setReturnValue(getState());
                            }
                            return;
                        }
                    }
                    case FARMER_HOE :
                    {
                        BlockPos pos = newFindHoeableSurface(position,farmField);
                        if(pos != null){
                            if (newHoeIfAble(pos, farmField))
                            {
                                didWork = true;
                                cir.setReturnValue(FARMER_PLANT);
                            }
                            else if(!isMissingFarmland){
                                cir.setReturnValue(getState());

                            }
                            else{
                                cir.setReturnValue(PREPARING);
                            }
                            return;
                        }
                    }
                    case FARMER_PLANT :
                    {
                        BlockPos pos = newFindPlantableSurface(position,farmField);
                        if( pos != null ) {
                            if(!newPlantCrop(farmField.getSeed(), position)){
                                didWork = true;
                                cir.setReturnValue(PREPARING);
                                return;
                            }
                        }
                        break;
                    }
                    default :
                    {
                        cir.setReturnValue(PREPARING);
                        return;
                    }
                }
                building.setPrevPos(position);
                setDelay(getLevelDelay());
            }

            building.setWorkingOffset(nextValidCell(farmField));
            if (building.getWorkingOffset() == null)
            {
                shouldDumpInventory = true;
                farmField.nextState();
                module.markDirty();
                if (didWork)
                {
                    module.resetCurrentExtension();
                    skippedState = 0;
                }
                didWork = false;
                building.setPrevPos(null);
                cir.setReturnValue(IDLE);
                return;
            }
        }
        else
        {
            cir.setReturnValue(IDLE);
            return;
        }
        cir.setReturnValue(defaultState);
    }

    // Try right click first
    private boolean newHarvestIfAble(BlockPos pos, FarmField farmField){
        BlockState state = world.getBlockState(pos.above());
        if(state.getBlock() instanceof BushBlock || state.getBlock() instanceof CropBlock){
            BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(pos.above()), Direction.UP, pos.above(), false);
            FakePlayer fakePlayer = FakePlayerFactory.getMinecraft((ServerLevel) world);
            ItemStack seedStack = state.getCloneItemStack(hitResult, world, pos.above(),fakePlayer);
            if(farmField.getSeed().getItem() == seedStack.getItem()){
                InteractionResult result = state.use(world, fakePlayer, InteractionHand.MAIN_HAND, hitResult);
                return result.consumesAction();
            }
        }
        return false;
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
        if (isNoFarmland(seed)) {
            cir.setReturnValue(null);
        }
    }

    private boolean isItemOfFarmland(final ItemStack itemStack, final Block Farmland){
        if(itemStack.getItem() instanceof BlockItem blockItem){
            return blockItem.getBlock() == Farmland;
        }
        return false;
    }

    private boolean newHoeIfAble(BlockPos position, final FarmField farmField)
    {
        if (!checkForToolOrWeapon(ModEquipmentTypes.hoe.get()))
        {
            if (mineBlock(position.above()))
            {
                final ItemStack seed = farmField.getSeed();
                equipHoe();
                worker.swing(worker.getUsedItemHand());
                if(SpecialSeedManager.isSpecialSeed(seed.getItem())){
                    final Block farmland = SpecialSeedManager.getRequiredSoil(seed.getItem());
                    final int amountOfFarmlandInInv = InventoryUtils.getItemCountInItemHandler(getWorker().getInventoryCitizen(), itemStack -> isItemOfFarmland(itemStack,farmland));
                    if (amountOfFarmlandInInv == 0 || !InventoryUtils.shrinkItemCountInItemHandler(getWorker().getInventoryCitizen(), itemStack -> isItemOfFarmland(itemStack,farmland)))
                    {
                        isMissingFarmland = true;
                        return false;
                    }
                }
                createCorrectFarmlandForSeed(seed, position);
                CitizenItemUtils.damageItemInHand(worker, InteractionHand.MAIN_HAND, 1);
                worker.decreaseSaturationForContinuousAction();
                worker.getCitizenColonyHandler().getColonyOrRegister().getStatisticsManager().increment(LAND_TILLED, worker.getCitizenColonyHandler().getColonyOrRegister().getDay());
                return true;
            }
            return false;
        }
        return true;
    }

    private BlockPos newFindHoeableSurface(@NotNull BlockPos position, @NotNull final FarmField farmField)
    {
        position = getSurfacePos(position);
        if (position == null)
        {
            return null;
        }
        ItemStack seed = farmField.getSeed();
        final BlockState blockState = world.getBlockState(position);
        boolean shouldGenerateWater = false;
        // 若种子标记为无需耕地，直接跳过锄地
        if (isUnderWater(seed)) {
            FluidState fluid = blockState.getFluidState();
            shouldGenerateWater = !Fluids.WATER.isSource(fluid);
            position = position.below();
        }
        if(SpecialSeedManager.isSpecialSeed(seed.getItem())){
            final Block farmland = SpecialSeedManager.getRequiredSoil(seed.getItem());
            if (farmField.isNoPartOfField(world, position)
                    || (world.getBlockState(position.above()).getBlock() instanceof CropBlock)
                    || (world.getBlockState(position.above()).getBlock() instanceof BushBlock)
                    || (world.getBlockState(position.above()).getBlock() instanceof BlockScarecrow)
                    || (blockState.getBlock() == farmland)
                    || (!(blockState.is(BlockTags.DIRT) || blockState.getFluidState().is(Fluids.WATER) || SpecialSeedManager.isSpecialSoil(blockState.getBlock())) && !(blockState.getBlock() instanceof MinecoloniesFarmland) && !(blockState.getBlock() instanceof FarmBlock))
                    || (world.getBlockState(position.above()).getBlock() instanceof MinecoloniesCropBlock)
            )
            {
                return null;
            }
            final BlockState aboveState = world.getBlockState(position.above());
            if (aboveState.canBeReplaced() && !(aboveState.getBlock() instanceof MinecoloniesCropBlock))
            {
                world.destroyBlock(position.above(), true);
            }
            return position;
        }
        if (isNoFarmland(seed)) {
            if (farmField.isNoPartOfField(world, position)
                    || (world.getBlockState(position.above()).getBlock() instanceof CropBlock)
                    || (world.getBlockState(position.above()).getBlock() instanceof BushBlock)
                    || (world.getBlockState(position.above()).getBlock() instanceof BlockScarecrow)
                    || (!(blockState.is(BlockTags.DIRT) || blockState.getFluidState().is(Fluids.WATER) || SpecialSeedManager.isSpecialSoil(blockState.getBlock())) && !(blockState.getBlock() instanceof MinecoloniesFarmland) && !(blockState.getBlock() instanceof FarmBlock))
                    || (world.getBlockState(position.above()).getBlock() instanceof MinecoloniesCropBlock)
            )
            {
                return null;
            }
            final BlockState aboveState = world.getBlockState(position.above());
            if (aboveState.canBeReplaced() && !(aboveState.getBlock() instanceof MinecoloniesCropBlock))
            {
                world.destroyBlock(position.above(), true);
            }
            if (!blockState.is(BlockTags.DIRT))
            {
                return position;
            }
            return shouldGenerateWater ? position : null;
        }
        if (farmField.isNoPartOfField(world, position)
                || (world.getBlockState(position.above()).getBlock() instanceof CropBlock)
                || (world.getBlockState(position.above()).getBlock() instanceof BushBlock)
                || (world.getBlockState(position.above()).getBlock() instanceof BlockScarecrow)
                || (!(blockState.is(BlockTags.DIRT) || blockState.getFluidState().is(Fluids.WATER) || SpecialSeedManager.isSpecialSoil(blockState.getBlock())) && !(blockState.getBlock() instanceof MinecoloniesFarmland) && !(blockState.getBlock() instanceof FarmBlock))
                || (world.getBlockState(position.above()).getBlock() instanceof MinecoloniesCropBlock)
        )
        {
            return null;
        }

        if(isRightFarmLandForCrop(farmField, blockState)) {
            return shouldGenerateWater ? position : null;
        }

        final BlockState aboveState = world.getBlockState(position.above());
        if (aboveState.canBeReplaced() && !(aboveState.getBlock() instanceof MinecoloniesCropBlock))
        {
            world.destroyBlock(position.above(), true);
        }

        if (!isRightFarmLandForCrop(farmField, blockState))
        {
            return position;
        }

        final BlockHitResult blockHitResult = new BlockHitResult(Vec3.ZERO, Direction.UP, position, false);
        final UseOnContext useOnContext = new UseOnContext(world,
                null,
                InteractionHand.MAIN_HAND,
                getInventory().getStackInSlot(InventoryUtils.getFirstSlotOfItemHandlerContainingEquipment(getInventory(), ModEquipmentTypes.hoe.get(), TOOL_LEVEL_WOOD_OR_GOLD, building.getMaxEquipmentLevel())),
                blockHitResult);
        final BlockState toolModifiedState = blockState.getToolModifiedState(useOnContext, ToolActions.HOE_TILL, true);
        if (toolModifiedState == null || !toolModifiedState.is(Blocks.FARMLAND))
        {
            return null;
        }

        return position;
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
        if (isUnderWater(seed)){
            final BlockState blockState = world.getBlockState(pos.above());
            FluidState fluid = blockState.getFluidState();
            if(!Fluids.WATER.isSource(fluid)){
                if(SpecialSeedManager.isSpecialSoil(blockState.getBlock())){
                    ItemStack stack = new ItemStack(blockState.getBlock(),1);
                    InventoryUtils.addItemStackToItemHandler(worker.getItemHandlerCitizen(),stack);
                }
                world.setBlock(pos.above(), Blocks.WATER.defaultBlockState(), 3);
            }
        }
        else{
            final BlockState blockState = world.getBlockState(pos);
            FluidState fluid = blockState.getFluidState();
            if(!fluid.isEmpty()){
                if(worker.getCitizenData().getEntity().isPresent() && worker.getBlockY() <= pos.getY()) {
                    Vec3 vec = worker.blockPosition().above().getCenter();
                    worker.getCitizenData().getEntity().get().teleportTo(vec.x, pos.getY() + 1.5, vec.z);
                }
                if(SpecialSeedManager.isSpecialSeed(seed.getItem())){
                    world.setBlock(pos, SpecialSeedManager.getRequiredSoil(seed.getItem()).defaultBlockState(), 3);
                    ci.cancel();
                    return;
                }
                else {
                    world.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
                }
            }
        }
        if(SpecialSeedManager.isSpecialSeed(seed.getItem())){
            if(worker.getCitizenData().getEntity().isPresent() && worker.getBlockY() <= pos.getY()) {
                Vec3 vec = worker.blockPosition().above().getCenter();
                worker.getCitizenData().getEntity().get().teleportTo(vec.x, pos.getY() + 1.5, vec.z);
            }
            world.setBlock(pos, SpecialSeedManager.getRequiredSoil(seed.getItem()).defaultBlockState(), 3);
            ci.cancel();
            return;
        }
        else{
            final BlockState blockState = world.getBlockState(pos);
            if(SpecialSeedManager.isSpecialSoil(blockState.getBlock())){
                ItemStack stack = new ItemStack(blockState.getBlock(),1);
                InventoryUtils.addItemStackToItemHandler(worker.getItemHandlerCitizen(),stack);
            }
        }
        if (isNoFarmland(seed)) {
            final BlockState blockState = world.getBlockState(pos);
            if(!blockState.is(BlockTags.DIRT)){
                if(worker.getCitizenData().getEntity().isPresent() && worker.getBlockY() <= pos.getY()) {
                    Vec3 vec = worker.blockPosition().above().getCenter();
                    worker.getCitizenData().getEntity().get().teleportTo(vec.x, pos.getY() + 1.5, vec.z);
                }
                world.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
            }
            ci.cancel();
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
        }
    }

    /**
     * Add special check for special seeds.
     */
    private BlockPos newFindPlantableSurface(@NotNull BlockPos position, @NotNull final FarmField farmField)
    {
        position = getSurfacePos(position);
        if (position == null || farmField.isNoPartOfField(world, position))
        {
            return null;
        }
        ItemStack seed = farmField.getSeed();
        if(isUnderWater(seed)){
            position = position.below();
        }
        BlockState state = world.getBlockState(position.above());
        BlockState belowState = world.getBlockState(position);
        if(state.getBlock() instanceof CropBlock
                || state.getBlock() instanceof BushBlock
                || state.getBlock() instanceof StemBlock
                || belowState.getBlock() instanceof BlockScarecrow
                || state.getBlock() instanceof MinecoloniesCropBlock)
        {
            return null;
        }
        if(SpecialSeedManager.isSpecialSeed(seed.getItem())){
            if(!(SpecialSeedManager.getRequiredSoil(seed.getItem()) == state.getBlock())){
                return null;
            }
            return position;
        }
        if(isNoFarmland(seed) ){
            if(!belowState.is(BlockTags.DIRT)){
                return null;
            }
        }
        else if(!isRightFarmLandForCrop(farmField, world.getBlockState(position))){
            return null;
        }
        return position;
    }

    private boolean newPlantCrop(final ItemStack item, @NotNull final BlockPos position)
    {
        if (item == null || item.isEmpty())
        {
            return false;
        }
        final int slot = worker.getCitizenInventoryHandler().findFirstSlotInInventoryWith(item.getItem());
        if (slot == -1)
        {
            return false;
        }
        if (item.getItem() instanceof BlockItem blockItem && (blockItem.getBlock() instanceof CropBlock || blockItem.getBlock() instanceof StemBlock || blockItem.getBlock() instanceof MinecoloniesCropBlock || blockItem.getBlock() instanceof BushBlock)
                && blockItem.getBlock().defaultBlockState().canSurvive(worker.level(), position.above()))
        {
            @NotNull final Item seed = item.getItem();
            if ((seed == Items.MELON_SEEDS || seed == Items.PUMPKIN_SEEDS) && building.getPrevPos() != null && !world.isEmptyBlock(building.getPrevPos().above()))
            {
                return true;
            }

            world.setBlockAndUpdate(position.above(), ((BlockItem) item.getItem()).getBlock().defaultBlockState());
            worker.decreaseSaturationForContinuousAction();
            getInventory().extractItem(slot, 1, false);
        }
        return true;
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
            if (surfaceBlock instanceof BonemealableBlock bonemealable) {
                if (!isBoneMealAble(position.above(),bonemealable)) {
                    cir.setReturnValue(position);
                    return;
                }
                if (surfaceBlock instanceof FungusBlock){
                    cir.setReturnValue(null);
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
                    cir.setReturnValue(isBoneMealAble(position.above(),bonemealable) ? null : position);
                    return;
                }
            }
            cir.setReturnValue(null);
        }
    }
}
