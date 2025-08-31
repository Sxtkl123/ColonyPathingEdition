package com.arxyt.colonypathingedition.core.mixins.pathfinding;

import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.minecolonies.api.entity.ModEntities;
import com.minecolonies.api.entity.other.MinecoloniesMinecart;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecolonies.core.entity.pathfinding.PathPointExtended;
import com.minecolonies.core.entity.pathfinding.navigation.AbstractAdvancedPathNavigate;
import com.minecolonies.core.entity.pathfinding.navigation.MinecoloniesAdvancedPathNavigate;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(MinecoloniesAdvancedPathNavigate.class)
public abstract class MinecoloniesAdvancedPathNavigateMixin extends AbstractAdvancedPathNavigate
{
    @Unique private MinecoloniesAdvancedPathNavigate asNavigator() {
        return (MinecoloniesAdvancedPathNavigate)(Object)this;
    }

    public MinecoloniesAdvancedPathNavigateMixin(@NotNull final Mob entity, final Level world) {
        super(entity, world);
    }

    /**
     * 将 900 * 900 替换为 (maxDistance)^2
     */
    @ModifyConstant(
            method = "setPathJob",
            constant = @Constant(doubleValue = 900 * 900),
            remap = false
    )
    private double modifyMaxDistanceSqr(double original)
    {
        return PathingConfig.MAX_PATHING_DISTANCE.get() * PathingConfig.MAX_PATHING_DISTANCE.get();
    }

    @Inject(method = "handleRails",at = @At("RETURN"),remap = false)
    private void getOffRailsAndTpToNextNode(CallbackInfoReturnable<Boolean> cir) {
        if (ourEntity.getVehicle() != null) {
            final Entity entity = ourEntity.getVehicle();
            if ( this.path == null || this.path.isDone() ){
                ourEntity.stopRiding();
                entity.remove(Entity.RemovalReason.DISCARDED);
            }
            else if (!asNavigator().isDone()) {
                @NotNull final PathPointExtended pEx = (PathPointExtended) Objects.requireNonNull(this.getPath()).getNode(this.getPath().getNextNodeIndex());
                if (!pEx.isOnRails()) {
                    ourEntity.stopRiding();
                    entity.remove(Entity.RemovalReason.DISCARDED);
                    ourEntity.teleportTo(pEx.x + 0.5, pEx.y, pEx.z + 0.5);
                }
                else{
                    if(entity instanceof MinecoloniesMinecart minecoloniesMinecart) {
                        int k = Mth.floor(minecoloniesMinecart.getX());
                        int i = Mth.floor(minecoloniesMinecart.getY());
                        int j = Mth.floor(minecoloniesMinecart.getZ());
                        if (minecoloniesMinecart.level().getBlockState(new BlockPos(k, i - 1, j)).is(BlockTags.RAILS)) {
                            --i;
                        }
                        BlockPos blockpos = new BlockPos(k, i, j);
                        BlockState blockstate = minecoloniesMinecart.level().getBlockState(blockpos);
                        if(!BaseRailBlock.isRail(blockstate)) {
                            ourEntity.stopRiding();
                            entity.remove(Entity.RemovalReason.DISCARDED);
                            double yOffset = 0.0D;
                            RailShape railshape = blockstate.getBlock() instanceof BaseRailBlock
                                    ? ((BaseRailBlock) blockstate.getBlock()).getRailDirection(blockstate, level, blockpos, null)
                                    : RailShape.NORTH_SOUTH;
                            if (railshape.isAscending())
                            {
                                yOffset = 0.5D;
                            }
                            MinecoloniesMinecart minecart = ModEntities.MINECART.create(level);
                            final double x = pEx.x + 0.5D;
                            final double y = pEx.y + 0.625D + yOffset;
                            final double z = pEx.z + 0.5D;
                            assert minecart != null;
                            minecart.setPos(x, y, z);
                            minecart.setDeltaMovement(Vec3.ZERO);
                            minecart.xo = x;
                            minecart.yo = y;
                            minecart.zo = z;
                            level.addFreshEntity(minecart);
                            minecart.setHurtDir(1);
                            mob.startRiding(minecart, true);
                        }
                    }
                }
            }
        }
    }

}
