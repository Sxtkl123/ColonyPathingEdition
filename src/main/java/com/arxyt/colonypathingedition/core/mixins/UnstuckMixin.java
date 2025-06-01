package com.arxyt.colonypathingedition.core.mixins;

import com.arxyt.colonypathingedition.core.mixins.accessor.MinecoloniesAdvancedPathNavigateAccessor;
import com.arxyt.colonypathingedition.core.util.DistanceUtils;
import com.minecolonies.api.colony.ICitizen;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.entity.pathfinding.IMinecoloniesNavigator;
import com.minecolonies.api.util.BlockPosUtil;
import com.minecolonies.api.util.Log;
import com.minecolonies.api.util.constant.ColonyConstants;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecolonies.core.entity.pathfinding.navigation.MinecoloniesAdvancedPathNavigate;
import com.minecolonies.core.entity.pathfinding.navigation.PathingStuckHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Objects;
import java.util.Random;

@Mixin(PathingStuckHandler.class)
public abstract class UnstuckMixin<NAV extends PathNavigation & IMinecoloniesNavigator> {

    @Shadow (remap = false) private int delayToNextUnstuckAction;
    @Shadow (remap = false) private int stuckLevel;
    @Shadow (remap = false) private Random rand;
    @Shadow (remap = false) private BlockPos moveAwayStartPos;
    @Shadow (remap = false) private boolean hadPath;
    @Shadow (remap = false) private boolean canBreakBlocks;
    @Shadow (remap = false) private boolean canPlaceLadders;
    @Shadow (remap = false) private boolean canBuildLeafBridges;
    @Shadow (remap = false) private int teleportRange;
    @Shadow (remap = false) private double chanceToByPassMovingAway;
    @Shadow (remap = false) private Direction movingAwayDir;

    @Shadow (remap = false) protected abstract void placeLadders(NAV navigator);
    @Shadow (remap = false) protected abstract void placeLeaves(NAV navigator);
    @Shadow (remap = false) protected abstract void breakBlocks(NAV navigator);
    @Shadow (remap = false) protected abstract void resetStuckTimers();
    @Shadow (remap = false) protected abstract void completeStuckAction(NAV navigator);

    @Shadow (remap = false) private BlockPos prevDestination;

    private int stuckLevelRecorder = 0;
    private boolean needReset = false;

    /**
     * @author ARxyt
     * @reason 我不太想写啥原因，我已经气笑了
     */
    @Overwrite(remap = false)
    private void tryUnstuck(final NAV navigator)
    {
        if (delayToNextUnstuckAction > 0)
        {
            return;
        }
        //试图阻止一下在工位的随机游走
        if ((prevDestination == null || prevDestination.equals(BlockPos.ZERO)) && stuckLevel == 0 && stuckLevelRecorder != 0 ){
            stuckLevelRecorder = 5;
            return;
        }

        delayToNextUnstuckAction = 50;

        boolean teleported = false;
        // Skip ahead
        if (teleportRange > 0 && hadPath)
        {
            int index = Math.min(Objects.requireNonNull(navigator.getPath()).getNextNodeIndex() + teleportRange, navigator.getPath().getNodeCount() - 1);
            final Node togo = navigator.getPath().getNode(index);
            navigator.getOurEntity().teleportTo(togo.x + 0.5d, togo.y, togo.z + 0.5d);
            delayToNextUnstuckAction = 20;
            teleported = true;
        }
        if (teleported || needReset) {
            navigator.getOurEntity().stopRiding();
            navigator.recalc();
            if(needReset){
                needReset = false;
                return;
            }
        }
        else{
            stuckLevel=Math.max(stuckLevelRecorder,stuckLevel);
            stuckLevel++;
        }

        // Move away, if have teleported will skip this.
        if ((stuckLevel <= 4 || !teleported) && prevDestination != null && !prevDestination.equals(BlockPos.ZERO)) {
            int range;
            if (navigator.getPath() != null) {
                moveAwayStartPos = navigator.getPath().getNodePos(navigator.getPath().getNextNodeIndex());
            } else {
                moveAwayStartPos = navigator.getOurEntity().blockPosition().above();
            }
            range = 20 * stuckLevel;
            navigator.setPauseTicks(0);
            BlockPos startPos = navigator.getOurEntity().blockPosition();
            //获取一个可能是通路的方向(因为同向遍历已近似广度优先，绕道到另一侧可以最大化收益)
            BlockPos dPos = new BlockPos(prevDestination.getX()-startPos.getX(),0,prevDestination.getZ()-startPos.getZ());
            double distance =BlockPosUtil.distSqr(dPos,BlockPos.ZERO);
            if(distance == 0){
                dPos = dPos.relative(movingAwayDir,range);
                movingAwayDir = movingAwayDir.getClockWise();
            }
            else{
                dPos.multiply(Mth.ceil(range/distance));
            }
            ((MinecoloniesAdvancedPathNavigateAccessor) navigator).invokeWalkTowards(prevDestination.offset(dPos.getX(),dPos.getY(),dPos.getZ()), range, 1.0f);
            stuckLevelRecorder = stuckLevel;
            navigator.setPauseTicks( range * TICKS_PER_BLOCK + 200);
            delayToNextUnstuckAction = range * TICKS_PER_BLOCK;
            needReset = true;
            return;
        }

        // Place ladders & leaves
        if (stuckLevel >= 5 && stuckLevel <= 6)
        {
            if (canPlaceLadders && rand.nextBoolean())
            {
                delayToNextUnstuckAction = 50;
                placeLadders(navigator);
                stuckLevelRecorder = stuckLevel;
                return;
            }
            else if (canBuildLeafBridges)
            {
                delayToNextUnstuckAction = 30;
                placeLeaves(navigator);
                stuckLevelRecorder = stuckLevel;
                return;
            }
        }

        // break blocks
        if (stuckLevel >= 7 && canBreakBlocks)
        {
            delayToNextUnstuckAction = 100;
            breakBlocks(navigator);
            stuckLevelRecorder = 5;
            stuckLevel = 5;
            return;
        }

        if (stuckLevel >= 5)
        {
            completeStuckAction(navigator);
            resetStuckTimers();
            stuckLevelRecorder = 0;
        }
    }

    // Shadow constants
    private static final int TICKS_PER_BLOCK = 20;
}
