package com.arxyt.colonypathingedition.core.mixins.lumberjack;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.core.entity.ai.workers.util.Tree;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.LinkedList;

@Mixin(Tree.class)
public abstract class MangroveTreeMixin {
    @Shadow(remap = false) private LinkedList<BlockPos> woodBlocks;
    @Shadow(remap = false) private ArrayList<BlockPos> stumpLocations;
    @Shadow(remap = false) private ItemStack sapling;
    @Shadow(remap = false) private BlockPos topLog;
    @Shadow(remap = false) private BlockPos location;

    public void fillMangroveTreeStumps(final int yLevel,Level world)
    {
        for (@NotNull final BlockPos pos : woodBlocks)
        {
            if (pos.getY() == yLevel && !sapling.is(Items.MANGROVE_PROPAGULE))
            {
                stumpLocations.add(pos);
            }
            else if (pos.getY() <= topLog.getY() && pos.getY() >= yLevel && sapling.is(Items.MANGROVE_PROPAGULE) &&
                    world.getBlockState(pos).is(Blocks.MANGROVE_LOG) )
            {
                stumpLocations.add(pos);
            }
        }

        // 红树树桩处理
        if (stumpLocations.size() > 0 && sapling.is(Items.MANGROVE_PROPAGULE))
        {
            BlockPos.MutableBlockPos acc = BlockPos.ZERO.mutable();
            acc.move(0,320,0);
            for (final BlockPos stump : stumpLocations)
            {
                if ( acc.getY() > stump.getY() )
                {
                    acc = BlockPos.ZERO.mutable().move(stump);
                }
            }
            if (stumpLocations.size() > 0 && sapling.is(Items.MANGROVE_PROPAGULE))
            {
                int targetX = acc.getX();
                int targetZ = acc.getZ();

                // search from Y axis of topLog to yLevel（Y axis of lowest stumps）
                BlockPos plantPos = null;
                for (int y = topLog.getY(); y >= yLevel; y--)
                {
                    BlockPos checkPos = new BlockPos(targetX, y, targetZ);
                    BlockState checkBlock = world.getBlockState(checkPos);
                    BlockState belowBlock = world.getBlockState(checkPos.below());

                    // check solid base plantable
                    if ( (belowBlock.is(BlockTags.DIRT)||belowBlock.is(Blocks.MUD)) &&
                            (checkBlock.canBeReplaced()||checkBlock.is(Blocks.WATER))) {
                        plantPos = checkPos; // above solid base
                        break;
                    }
                }
                stumpLocations.clear();
                if (plantPos != null)
                {
                    stumpLocations.add(plantPos);
                }
                else
                {
                    BlockPos defaultPos = new BlockPos(targetX, yLevel, targetZ);
                    stumpLocations.add(defaultPos);
                }

            }
        }

    }

    /**
     * @author ARxyt
     * @reason When planting mangrove, function is reset to ensure stability。
     */
    @Inject(
            method = "findLogs",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/minecolonies/core/entity/ai/workers/util/Tree;fillTreeStumps(I)V",
                    shift = At.Shift.BEFORE  // 在 fillTreeStumps 调用之前注入
            ),
            cancellable = true,  // 允许取消原方法调用
            remap = false
    )
    private void beforeFillTreeStumps(Level world, IColony colony, CallbackInfo ci) {
        if (sapling.is(Items.MANGROVE_PROPAGULE)) {
            // 调用你的 fillTreeStumps 逻辑
            fillMangroveTreeStumps(location.getY(), world);
            // 取消原方法的 fillTreeStumps 调用
            ci.cancel();
        }
        // 否则继续执行原方法
    }
}
