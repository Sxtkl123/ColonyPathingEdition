package com.arxyt.colonypathingedition.core.mixins;

import com.arxyt.colonypathingedition.core.mixins.accessor.AbstractAISkeletonAccessor;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.jobs.IJob;
import com.minecolonies.api.colony.permissions.Action;
import com.minecolonies.api.colony.requestsystem.manager.IRequestManager;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.resolver.player.IPlayerRequestResolver;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIBasic;
import com.minecolonies.core.entity.pathfinding.navigation.EntityNavigationUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.*;
import java.util.stream.Collectors;

@Mixin(value = AbstractEntityAIBasic.class, remap = false)
public abstract class AbstractEntityAIBasicMixin<B extends AbstractBuilding> implements AbstractAISkeletonAccessor<IJob<?>>
{
    @Final
    @Shadow(remap = false)
    public B building;

    @Shadow(remap = false)
    protected abstract boolean walkToBuilding();

    @Shadow protected abstract boolean walkToSafePos(BlockPos pos);

    private ImmutableList<IRequest<?>> getRequestCannotBeDone(){
        final ArrayList<IRequest<?>> requests = Lists.newArrayList();
        final IRequestManager requestManager = getWorker().getCitizenData().getColony().getRequestManager();
        final IPlayerRequestResolver resolver = requestManager.getPlayerResolver();
        final Set<IToken<?>> requestTokens = new HashSet<>(resolver.getAllAssignedRequests());
        for (final IToken<?> token : requestTokens)
        {
            IRequest<?> request = requestManager.getRequestForToken(token);

            while (request != null && request.hasParent())
            {
                request = requestManager.getRequestForToken(Objects.requireNonNull(request.getParent()));
            }

            if (request != null && !requests.contains(request))
            {
                requests.add(request);
            }
        }

        return ImmutableList.copyOf(requests);
    }

    private boolean checkRequestCannotBeDone()
    {
        ImmutableList<IRequest<?>> requests = getRequestCannotBeDone();
        for(IRequest<?> request : requests){
            if (request.getRequester().getLocation().equals(building.getLocation())&&!getWorker().getCitizenData().isRequestAsync(request.getId())){
                return true;
            }
        }
        return false;
    }

    @Redirect(
            method = "lookForRequests",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/minecolonies/core/entity/ai/workers/AbstractEntityAIBasic;walkToBuilding()Z",
                    remap = false
            ),
            remap = false
    )
    private boolean redirectWalkToBuilding(AbstractEntityAIBasic<?, ?> instance)
    {
        AbstractEntityCitizen worker = getWorker();
        ICitizenData citizenData = worker.getCitizenData();
        if(checkRequestCannotBeDone()) {
            IColony colony = citizenData.getColony();
            if (colony.hasTownHall()) {
                IBuilding townHall= colony.getBuildingManager().getTownHall();
                BlockPos townHallPos = townHall.getPosition();
                double radius = 20.0D; // 搜索半径
                if (worker.blockPosition().closerThan(townHallPos, radius))
                {
                    // 在level中查找玩家实体
                    List<Player> players = getWorld().getEntitiesOfClass(
                            Player.class,
                            new AABB(
                                    townHallPos.getX() - radius,
                                    townHallPos.getY() - radius,
                                    townHallPos.getZ() - radius,
                                    townHallPos.getX() + radius,
                                    townHallPos.getY() + radius,
                                    townHallPos.getZ() + radius
                            ),
                            player -> !player.isSpectator()
                    );
                    if (!players.isEmpty())
                    {
                        List<Player> officers = players.stream()
                                .filter(p -> {
                                    // 获取该玩家在该 Colony 中的角色
                                    return colony.getPermissions().hasPermission(p,Action.RIGHTCLICK_ENTITY);
                                })
                                .collect(Collectors.toList());
                        if (!officers.isEmpty())
                        {
                            Player nearestOfficer = officers.stream()
                                    .min(Comparator.comparingDouble(p -> p.distanceTo(worker)))
                                    .orElse(officers.get(0));
                            return walkToSafePos(nearestOfficer.blockPosition());
                        }
                    }
                }
                return EntityNavigationUtils.walkToBuilding(worker,townHall);
            }
        }
        // 2) 调用原方法行为：
        return walkToBuilding();
    }
}
