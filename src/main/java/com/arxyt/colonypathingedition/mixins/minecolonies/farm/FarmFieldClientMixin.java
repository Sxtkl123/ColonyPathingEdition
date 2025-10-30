package com.arxyt.colonypathingedition.mixins.minecolonies.farm;

import com.arxyt.colonypathingedition.api.FarmFieldExtra;
import com.arxyt.colonypathingedition.core.message.CropRotationAdvanceDayMessage;
import com.minecolonies.api.colony.IColonyView;
import com.minecolonies.api.colony.buildingextensions.registry.BuildingExtensionRegistries;
import com.minecolonies.core.Network;
import com.minecolonies.core.colony.buildingextensions.AbstractBuildingExtensionModule;
import com.minecolonies.core.colony.buildingextensions.FarmField;
import com.minecolonies.core.tileentities.TileEntityScarecrow;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.minecraft.world.level.Level.TICKS_PER_DAY;


@Mixin(FarmField.class)
public abstract class FarmFieldClientMixin extends AbstractBuildingExtensionModule {
    @Shadow(remap = false)  private ItemStack seed;

    private FarmField asFarmField(){
        return ((FarmField)(Object)this);
    }

    public FarmFieldClientMixin(final BuildingExtensionRegistries.BuildingExtensionEntry fieldType, final BlockPos position)
    {
        super(fieldType, position);
    }

    @Inject(method = "getSeed",at = @At("HEAD"),remap = false ,cancellable = true)
    public void getSeed(CallbackInfoReturnable<ItemStack> cir)
    {
        FarmFieldExtra farmField = (FarmFieldExtra) asFarmField();

        if(farmField.isSeasonalSeedsEmpty() && !seed.isEmpty()){
            farmField.setSeasonSeed(1,seed);
        }
        Level world = Minecraft.getInstance().level;
        if(world != null){
            final int nowDay = (int)(world.getDayTime() / TICKS_PER_DAY);
            if(nowDay != farmField.getDate()) {
                farmField.advanceDay(nowDay);
                BlockState state = world.getBlockState(getPosition());
                if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)){
                    final BlockEntity entity = world.getBlockEntity(state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER ? getPosition().below() : getPosition());
                    if (entity instanceof TileEntityScarecrow scarecrow && scarecrow.getCurrentColony() instanceof IColonyView colony) {
                        Network.getNetwork().sendToServer(new CropRotationAdvanceDayMessage(colony, getPosition(), nowDay, farmField.getCurrentDay(), farmField.getCurrentSeason()));
                    }
                }
            }
        }
        seed = farmField.getSeasonSeed(farmField.getCurrentSeason()).copy();
        seed.setCount(1);
        if(seed.isEmpty()){
            asFarmField().setBuilding(null);
        }
        cir.setReturnValue(seed);
    }
}
