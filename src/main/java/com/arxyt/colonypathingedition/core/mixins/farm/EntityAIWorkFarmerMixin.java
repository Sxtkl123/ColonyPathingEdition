package com.arxyt.colonypathingedition.core.mixins.farm;

import com.arxyt.colonypathingedition.core.mixins.accessor.AbstractAISkeletonAccessor;
import com.arxyt.colonypathingedition.core.mixins.accessor.AbstractEntityAIBasicAccessor;
import com.arxyt.colonypathingedition.core.tag.ModTag;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.core.Network;
import com.minecolonies.core.colony.buildingextensions.FarmField;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.colony.jobs.JobFarmer;
import com.minecolonies.core.entity.ai.workers.production.agriculture.EntityAIWorkFarmer;
import com.minecolonies.core.network.messages.client.CompostParticleMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.minecolonies.api.util.constant.CitizenConstants.BLOCK_BREAK_SOUND_RANGE;


@Mixin(EntityAIWorkFarmer.class)
public abstract class EntityAIWorkFarmerMixin implements AbstractAISkeletonAccessor<JobFarmer>, AbstractEntityAIBasicAccessor<AbstractBuilding> {

    @Shadow(remap = false) protected abstract BlockPos getSurfacePos(final BlockPos position);
    @Shadow(remap = false) protected abstract boolean isCompost(final ItemStack itemStack);

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
