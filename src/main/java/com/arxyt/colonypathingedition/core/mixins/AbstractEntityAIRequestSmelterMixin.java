package com.arxyt.colonypathingedition.core.mixins;

import com.arxyt.colonypathingedition.core.api.FurnaceBlockEntityExtras;
import com.arxyt.colonypathingedition.core.mixins.accessor.AbstractEntityAIBasicAccessor;
import com.minecolonies.api.colony.interactionhandling.ChatPriority;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.util.*;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.colony.buildings.modules.BuildingModules;
import com.minecolonies.core.colony.buildings.modules.FurnaceUserModule;
import com.minecolonies.core.colony.interactionhandling.StandardInteraction;
import com.minecolonies.core.colony.jobs.AbstractJobCrafter;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIBasic;
import com.minecolonies.core.entity.ai.workers.crafting.AbstractEntityAIRequestSmelter;
import com.minecolonies.core.util.citizenutils.CitizenItemUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Objects;
import java.util.function.Predicate;

import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.*;
import static com.minecolonies.api.util.ItemStackUtils.*;
import static com.minecolonies.api.util.constant.Constants.*;
import static com.minecolonies.api.util.constant.TranslationConstants.BAKER_HAS_NO_FURNACES_MESSAGE;

@Mixin( AbstractEntityAIRequestSmelter.class )
public abstract class AbstractEntityAIRequestSmelterMixin<AI extends AbstractEntityAIBasic<J, ? extends AbstractBuilding>, J extends AbstractJobCrafter<AI, J>>
        extends AbstractEntityAICraftingMixin<AbstractBuilding,J> implements AbstractEntityAIBasicAccessor<AbstractBuilding> {
    @Shadow(remap = false) protected abstract int getMaxUsableFurnaces();

    @Unique private static final int STANDARD_DELAY = 5;
    @Unique private static final int BASE_XP_GAIN = 5;
    @Unique private int randomFurnace = -1;

    @Unique
    private boolean isFurnaceNotOccupied(FurnaceBlockEntity furnace){
        FurnaceBlockEntityExtras furnaceExtra = (FurnaceBlockEntityExtras) furnace;
        return furnaceExtra.getFurnaceWorker() < 0 || furnaceExtra.getFurnaceWorker() == getWorker().getCivilianID();
    }

    @Unique
    private boolean isFurnaceOccupiedBy(FurnaceBlockEntity furnace, int civilianID){
        return ((FurnaceBlockEntityExtras)furnace).getFurnaceWorker() == civilianID;
    }

    @Unique
    private void resetFurnaceOccupy(FurnaceBlockEntity furnace){
        ((FurnaceBlockEntityExtras)furnace).setFurnaceWorker(-1);
    }

    @Unique
    private boolean isFurnaceCanReoccupied(FurnaceBlockEntity furnace){
        return !(((FurnaceBlockEntityExtras) furnace).atProtectTime());
    }

    @Unique
    private void setFurnaceOccupy(FurnaceBlockEntity furnace,int civilianID){
        ((FurnaceBlockEntityExtras)furnace).setFurnaceWorker(civilianID);
    }

    @Unique
    private int countOfUsableFurnaces()
    {
        int count = 0;
        final Level world = building.getColony().getWorld();
        for (final BlockPos pos : building.getFirstModuleOccurance(FurnaceUserModule.class).getFurnaces())
        {
            if (WorldUtil.isBlockLoaded(world, pos))
            {
                final BlockEntity entity = world.getBlockEntity(pos);
                if (entity instanceof final FurnaceBlockEntity furnace)
                {
                    if (furnace.getItem(SMELTABLE_SLOT).isEmpty() && (isFurnaceNotOccupied(furnace) || isFurnaceCanReoccupied(furnace)))
                    {
                        count += 1;
                    }
                }
            }
        }
        return count;
    }

    @Unique
    private void accelerateRandomFurnaces(FurnaceUserModule module) {
        final int size = module.getFurnaces().size();
        if (randomFurnace < 0 || randomFurnace >= size) {
            randomFurnace = getWorker().getRandom().nextInt(size);
        }
        final Level world = building.getColony().getWorld();
        final BlockPos pos = module.getFurnaces().get(randomFurnace);
        if (WorldUtil.isBlockLoaded(world, pos)) {
            final BlockEntity entity = world.getBlockEntity(pos);
            if (entity instanceof final FurnaceBlockEntity furnace && furnace.getBlockState().getValue(BlockStateProperties.LIT)) {
                FurnaceBlockEntityExtras extrasFurnace = (FurnaceBlockEntityExtras) furnace;
                if (!(furnace.getItem(SMELTABLE_SLOT).isEmpty())) {
                    int addProgress = getWorker().getCitizenData().getCitizenSkillHandler().getLevel(invokeGetModuleForJob().getPrimarySkill()) / 2;
                    while (addProgress > 0 && !furnace.getItem(SMELTABLE_SLOT).isEmpty()) {
                        addProgress = extrasFurnace.addProgress(addProgress);
                        AbstractFurnaceBlockEntity.serverTick(world, pos, world.getBlockState(pos), furnace);
                    }
                }
            }
        }
    }

    /**
     * @author ARxyt
     * @reason 工人分开计算
     */
    @Overwrite(remap = false)
    protected int getExtendedCount(final ItemStack stack)
    {
        if (getCurrentRecipeStorage() != null && getCurrentRecipeStorage().getIntermediate() == Blocks.FURNACE)
        {
            int count = 0;
            for (final BlockPos pos : building.getFirstModuleOccurance(FurnaceUserModule.class).getFurnaces())
            {
                if (WorldUtil.isBlockLoaded(getWorld(), pos))
                {
                    final BlockEntity entity = getWorld().getBlockEntity(pos);
                    if (entity instanceof final FurnaceBlockEntity furnace && isFurnaceOccupiedBy(furnace,getWorker().getCivilianID()))
                    {
                        final ItemStack smeltableSlot = furnace.getItem(SMELTABLE_SLOT);
                        final ItemStack resultSlot = furnace.getItem(RESULT_SLOT);
                        if (ItemStackUtils.compareItemStacksIgnoreStackSize(stack, smeltableSlot))
                        {
                            count += smeltableSlot.getCount();
                        }
                        else if (ItemStackUtils.compareItemStacksIgnoreStackSize(stack, resultSlot))
                        {
                            count += resultSlot.getCount();
                        }
                    }
                }
            }
            return count;
        }
        return 0;
    }

    /**
     * @author ARxyt
     * @reason isEmpty && isLit -> isEmpty || isLit
     */
    @Overwrite(remap = false)
    private BlockPos getPositionOfOvenToRetrieveFrom()
    {
        for (final BlockPos pos : building.getFirstModuleOccurance(FurnaceUserModule.class).getFurnaces())
        {
            final BlockEntity entity = getWorld().getBlockEntity(pos);
            if (entity instanceof final FurnaceBlockEntity furnace)
            {
                int countInResultSlot = 0;
                boolean fullResult = false;
                if (!isEmpty(furnace.getItem(RESULT_SLOT)))
                {
                    countInResultSlot = furnace.getItem(RESULT_SLOT).getCount();
                    fullResult = countInResultSlot >= furnace.getItem(RESULT_SLOT).getMaxStackSize();
                }

                if (fullResult || (countInResultSlot > 0 && (furnace.getItem(SMELTABLE_SLOT).isEmpty() || !furnace.getBlockState().getValue(BlockStateProperties.LIT))))
                {
                    FurnaceBlockEntityExtras furnaceExtra = (FurnaceBlockEntityExtras) furnace;
                    if(furnaceExtra.getFurnacePicker() == getWorker().getCivilianID() || !(furnaceExtra.atProtectTime())){
                        return pos;
                    }
                }
            }
        }
        return null;
    }
    /**
     * @author ARxyt
     * @reason 加强加速幅度，增加燃烧时长的同时降低servertick调用量。
     */
    @Overwrite(remap = false)
    private boolean accelerateFurnaces()
    {
        final Level world = building.getColony().getWorld();
        for (final BlockPos pos : building.getModule(BuildingModules.FURNACE).getFurnaces())
        {
            if (WorldUtil.isBlockLoaded(world, pos))
            {
                final BlockEntity entity = world.getBlockEntity(pos);
                if (entity instanceof final FurnaceBlockEntity furnace && furnace.getBlockState().getValue(BlockStateProperties.LIT))
                {
                    if(!isFurnaceOccupiedBy(furnace,getWorker().getCivilianID())){
                        continue;
                    }
                    FurnaceBlockEntityExtras extrasFurnace = (FurnaceBlockEntityExtras) furnace;
                    extrasFurnace.addLitTime((int)Math.ceil(Math.sqrt(getWorker().getCitizenData().getCitizenSkillHandler().getLevel(invokeGetModuleForJob().getSecondarySkill())) * 1.71));
                    if (!(furnace.getItem(SMELTABLE_SLOT).isEmpty()))
                    {
                        int addProgress = (int) (getWorker().getCitizenData().getCitizenSkillHandler().getLevel(invokeGetModuleForJob().getPrimarySkill()) * 1.8);
                        while (addProgress > 0 && !furnace.getItem(SMELTABLE_SLOT).isEmpty()){
                            addProgress = extrasFurnace.addProgress(addProgress);
                            AbstractFurnaceBlockEntity.serverTick(world, pos, world.getBlockState(pos), furnace);
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * @author ARxyt
     * @reason 重写计算部分，使得每个村民使用的熔炉分开计算
     */
    @Overwrite(remap = false)
    private int countOfBurningFurnaces()
    {
        int count = 0;
        final Level world = building.getColony().getWorld();
        for (final BlockPos pos : building.getFirstModuleOccurance(FurnaceUserModule.class).getFurnaces())
        {
            if (WorldUtil.isBlockLoaded(world, pos))
            {
                final BlockEntity entity = world.getBlockEntity(pos);
                if (entity instanceof final FurnaceBlockEntity furnace)
                {
                    if (isFurnaceOccupiedBy(furnace,getWorker().getCivilianID()) && !furnace.getItem(SMELTABLE_SLOT).isEmpty())
                    {
                        count += 1;
                    }
                }
            }
        }
        return count;
    }

    /**
     * @author ARxyt
     * @reason 拿取时尝试直接给予熔炉占用者，没有占用者时再尝试直接拿取。
     */
    @Overwrite(remap = false)
    private void extractFromFurnaceSlot(final FurnaceBlockEntity furnace, final int slot)
    {
        if(isFurnaceNotOccupied(furnace) || slot != RESULT_SLOT){
            InventoryUtils.transferItemStackIntoNextFreeSlotInItemHandler(
                    new InvWrapper(furnace), slot,
                    getWorker().getInventoryCitizen());
        }
        else {
            if(!InventoryUtils.transferItemStackIntoNextFreeSlotInItemHandler(
                    new InvWrapper(furnace), slot,
                    getWorker().getCitizenColonyHandler().getColony().getCitizen(((FurnaceBlockEntityExtras)furnace).getFurnaceWorker()).getInventory())
            ){
                InventoryUtils.transferItemStackIntoNextFreeSlotInItemHandler(
                        new InvWrapper(furnace), slot,
                        getWorker().getInventoryCitizen());
            }
        }

        if (slot == RESULT_SLOT)
        {
            furnace.getRecipesToAwardAndPopExperience((ServerLevel) Objects.requireNonNull(furnace.getLevel()),  Vec3.atCenterOf(furnace.getBlockPos()));
            if(furnace.getItem(SMELTABLE_SLOT).isEmpty()){
                resetFurnaceOccupy(furnace);
            }
            getWorker().getCitizenExperienceHandler().addExperience(BASE_XP_GAIN);
        }
    }

    @Redirect(
            method = "isFuelNeeded",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/FurnaceBlockEntity;isLit()Z"
            ),
            remap = false
    )
    private boolean redirectIsLitForIsFuelNeeded(FurnaceBlockEntity furnace)
    {
        return false;
    }

    @Redirect(
            method = "checkFurnaceFuel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/FurnaceBlockEntity;isLit()Z"
            ),
            remap = false
    )
    private boolean redirectIsLitForCheckFurnaceFuel(FurnaceBlockEntity furnace)
    {
        return false;
    }

    boolean checkRecipeFinish = false;

    /**
     * @author ARxyt
     * @reason 涉及替取后的结果检测，此时 walkTo 为 null，需要大量修改代码。
     */
    @Overwrite(remap = false)
    private IAIState retrieveSmeltableFromFurnace() {
        if ((walkTo == null && !checkRecipeFinish) || getCurrentRequest() == null) {
            return START_WORKING;
        }
        checkRecipeFinish = false;

        int preExtractCount = 0;
        if (walkTo != null){
            final BlockEntity entity = getWorld().getBlockEntity(walkTo);
            if (!(entity instanceof FurnaceBlockEntity) || (isEmpty(((FurnaceBlockEntity) entity).getItem(RESULT_SLOT)))) {
                walkTo = null;
                return START_WORKING;
            }

            FurnaceBlockEntityExtras furnace = (FurnaceBlockEntityExtras) entity;
            if(furnace.getFurnacePicker() != getWorker().getCivilianID() && furnace.atProtectTime()){
                return START_WORKING;
            }

            if (!walkToWorkPos(walkTo)) {
                furnace.setFurnacePicker(getWorker().getCivilianID());
                return invokeGetState();
            }
            walkTo = null;

            preExtractCount = InventoryUtils.getItemCountInItemHandler(getWorker().getInventoryCitizen(),
                    stack -> ItemStackUtils.compareItemStacksIgnoreStackSize(getCurrentRequest().getRequest().getStack(), stack));

            extractFromFurnaceSlot((FurnaceBlockEntity) entity, RESULT_SLOT);
            furnace.setFurnacePicker(-1);
        }
        //Do we have the requested item in the inventory now?
        final int resultCount = InventoryUtils.getItemCountInItemHandler(getWorker().getInventoryCitizen(),
                stack -> ItemStackUtils.compareItemStacksIgnoreStackSize(getCurrentRequest().getRequest().getStack(), stack)) - preExtractCount;
        if (resultCount > 0)
        {
            final ItemStack stack = getCurrentRequest().getRequest().getStack().copy();
            stack.setCount(resultCount);
            getCurrentRequest().addDelivery(stack);

            getJob().setCraftCounter(getJob().getCraftCounter() + resultCount);
            getJob().setProgress(getJob().getProgress() - resultCount);
            if (getJob().getMaxCraftingCount() == 0)
            {
                getJob().setMaxCraftingCount(getCurrentRequest().getRequest().getCount());
            }
            if (getJob().getCraftCounter() >= getJob().getMaxCraftingCount() && getJob().getProgress() <= 0)
            {
                getJob().finishRequest(true);
                invokeResetValues();
                resetCurrentRecipeStorage();
                incrementActionsDoneAndDecSaturation();
                return INVENTORY_FULL;
            }
        }

        invokeSetDelay(STANDARD_DELAY);
        return START_WORKING;
    }

    /**
     * @author ARxyt
     * @reason 涉及分发策略/熔炉使用策略的修改以及熔炉占用策略的补充。
     */
    @Overwrite(remap = false)
    private IAIState fillUpFurnace()
    {
        //Log.getLogger().debug(" {} now in fillUpFurnace", getWorker().getName().getString());
        final FurnaceUserModule module = building.getFirstModuleOccurance(FurnaceUserModule.class);
        if (module.getFurnaces().isEmpty())
        {
            if (getWorker().getCitizenData() != null)
            {
                getWorker().getCitizenData().triggerInteraction(new StandardInteraction(Component.translatable(BAKER_HAS_NO_FURNACES_MESSAGE), ChatPriority.BLOCKING));
            }
            invokeSetDelay(STANDARD_DELAY);
            //Log.getLogger().debug(" {} not a furnace in fillUpFurnace", getWorker().getName().getString());
            return START_WORKING;
        }

        if (walkTo == null || getWorld().getBlockState(walkTo).getBlock() != Blocks.FURNACE)
        {
            walkTo = null;
            invokeSetDelay(STANDARD_DELAY);
            //Log.getLogger().debug(" {} wrong walkTo in fillUpFurnace", getWorker().getName().getString());
            return START_WORKING;
        }

        final int burningCount = countOfBurningFurnaces();
        final BlockEntity entity = getWorld().getBlockEntity(walkTo);
        if (entity instanceof final FurnaceBlockEntity furnace && getCurrentRecipeStorage() != null)
        {
            if(!isFurnaceNotOccupied(furnace) && !isFurnaceCanReoccupied(furnace))
            {
                //Log.getLogger().debug(" {} furnace is occupied in fillUpFurnace", getWorker().getName().getString());
                return START_WORKING;
            }
            final int maxFurnaces = getMaxUsableFurnaces();
            final int maxUsableFurnaces = Math.min(maxFurnaces,countOfUsableFurnaces());
            final Predicate<ItemStack> smeltable = stack -> ItemStackUtils.compareItemStacksIgnoreStackSize(getCurrentRecipeStorage().getCleanedInput().get(0).getItemStack(), stack);
            final int smeltableInFurnaces = getExtendedCount(getCurrentRecipeStorage().getCleanedInput().get(0).getItemStack());
            final int resultInFurnaces = getExtendedCount(getCurrentRecipeStorage().getPrimaryOutput());
            final int resultInCitizenInv = InventoryUtils.getItemCountInItemHandler(getWorker().getInventoryCitizen(),
                    stack -> ItemStackUtils.compareItemStacksIgnoreStackSize(stack, getCurrentRecipeStorage().getPrimaryOutput()));

            final int targetCount = getCurrentRequest().getRequest().getCount() - smeltableInFurnaces - resultInFurnaces - resultInCitizenInv;

            if (targetCount <= 0)
            {
                //Log.getLogger().debug(" {} wrong target count :{} in fillUpFurnace", getWorker().getName().getString(),targetCount);
                if(smeltableInFurnaces + resultInFurnaces == 0){
                    walkTo = null;
                    checkRecipeFinish = true;
                    return RETRIEVING_END_PRODUCT_FROM_FURNACE;
                }
                return START_WORKING;
            }
            final int amountOfSmeltableInBuilding = InventoryUtils.getCountFromBuilding(building, smeltable);
            final int amountOfSmeltableInInv = InventoryUtils.getItemCountInItemHandler(getWorker().getInventoryCitizen(), smeltable);

            if (getWorker().getItemInHand(InteractionHand.MAIN_HAND).isEmpty())
            {
                getWorker().setItemInHand(InteractionHand.MAIN_HAND, getCurrentRecipeStorage().getCleanedInput().get(0).getItemStack().copy());
            }
            if (amountOfSmeltableInInv > 0)
            {
                if (hasFuelInFurnaceAndNoSmeltable(furnace) || hasNeitherFuelNorSmeltAble(furnace))
                {
                    int toTransfer = 0;
                    if (burningCount < maxFurnaces)
                    {
                        int availableFurnaces = maxUsableFurnaces - burningCount;

                        if (targetCount > STACKSIZE * availableFurnaces)
                        {
                            toTransfer = STACKSIZE;
                        }
                        else
                        {
                            if(targetCount <= 8 * (availableFurnaces - 1)){
                                availableFurnaces =(targetCount - 1) / 8 + 1;
                            }
                            toTransfer = (targetCount / availableFurnaces);
                            if(targetCount % availableFurnaces > 0){
                                toTransfer ++;
                            }
                        }
                    }
                    if (toTransfer > 0) {
                        setFurnaceOccupy(furnace,getWorker().getCivilianID());
                        if (!invokeWalkToSafePos(walkTo)) {
                            //Log.getLogger().debug(" {} pathing in fillUpFurnace", getWorker().getName().getString());
                            return invokeGetState();
                        }
                        CitizenItemUtils.hitBlockWithToolInHand(getWorker(), walkTo);
                        InventoryUtils.transferXInItemHandlerIntoSlotInItemHandler(
                                getWorker().getInventoryCitizen(),
                                smeltable,
                                toTransfer,
                                new InvWrapper(furnace),
                                SMELTABLE_SLOT);
                        if(!furnace.getItem(RESULT_SLOT).isEmpty()){
                            extractFromFurnaceSlot(furnace, RESULT_SLOT);
                        }
                    }
                }
            }
            else if (amountOfSmeltableInBuilding >= targetCount - amountOfSmeltableInInv
                    && getCurrentRecipeStorage().getIntermediate() == Blocks.FURNACE)
            {
                needsCurrently = new Tuple<>(smeltable, targetCount);
                resetFurnaceOccupy(furnace);
                return GATHERING_REQUIRED_MATERIALS;
            }
            else
            {
                //This is a safety net for the AI getting way out of sync with it's tracking. It shouldn't happen.
                getJob().finishRequest(false);
                invokeResetValues();
                walkTo = null;
                //Log.getLogger().debug(" {} went wrong place in fillUpFurnace", getWorker().getName().getString());
                return IDLE;
            }
        }
        else if (!(getWorld().getBlockState(walkTo).getBlock() instanceof FurnaceBlock))
        {
            module.removeFromFurnaces(walkTo);
        }
        walkTo = null;
        invokeSetDelay(STANDARD_DELAY);
        //Log.getLogger().debug(" {} success? |{}| in fillUpFurnace", getWorker().getName().getString(),success);
        return START_WORKING;
    }

    /**
     * @author ARxyt
     * @reason 目前只是修改了进入的判定条件。
     */
    @Overwrite(remap = false)
    private IAIState checkIfAbleToSmelt()
    {
        //Log.getLogger().debug(" {} now in checkIfAbleToSmelt", getWorker().getName().getString());
        // We're fully committed currently, try again later.
        final int burning = countOfBurningFurnaces();
        final int canUse = countOfUsableFurnaces();
        if (canUse == 0 || (burning > 0 && (burning >= getMaxUsableFurnaces() || (getJob().getCraftCounter() + getJob().getProgress()) >= getJob().getMaxCraftingCount())))
        {
            //Log.getLogger().debug(" {} reach usage: {}, can use: {}", getWorker().getName().getString(), burning, canUse);
            if(canUse == 0){
                walkTo = null;
                checkRecipeFinish = true;
                return RETRIEVING_END_PRODUCT_FROM_FURNACE;
            }
            invokeSetDelay(TICKS_SECOND);
            return invokeGetState();
        }

        final FurnaceUserModule module = building.getFirstModuleOccurance(FurnaceUserModule.class);
        for (final BlockPos pos : module.getFurnaces())
        {
            final BlockEntity entity = getWorld().getBlockEntity(pos);

            if (entity instanceof FurnaceBlockEntity furnace)
            {
                //Log.getLogger().debug(" {} furnace state: smelt slot empty |{}|, furnace occupied |{}|, furnace can reset |{}|", getWorker().getName().getString(),furnace.getItem(SMELTABLE_SLOT).isEmpty(),isFurnaceNotOccupied(furnace),isFurnaceCanReoccupied(furnace));
                if (furnace.getItem(SMELTABLE_SLOT).isEmpty() && (isFurnaceNotOccupied(furnace) || isFurnaceCanReoccupied(furnace)))
                {
                    randomFurnace = -1;
                    walkTo = pos;
                    return START_USING_FURNACE;
                }
            }
            else
            {
                if (!(getWorld().getBlockState(pos).getBlock() instanceof FurnaceBlock))
                {
                    module.removeFromFurnaces(pos);
                }
            }
        }

        if (burning > 0)
        {
            invokeSetDelay(TICKS_SECOND);
        }
        else{
            accelerateRandomFurnaces(module);
        }

        //Log.getLogger().debug(" {} failed to get furnace", getWorker().getName().getString());
        return invokeGetState();
    }


}
