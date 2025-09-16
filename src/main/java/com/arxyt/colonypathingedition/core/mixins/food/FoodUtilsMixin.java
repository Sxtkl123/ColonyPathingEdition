package com.arxyt.colonypathingedition.core.mixins.food;

import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.entity.citizen.citizenhandlers.ICitizenFoodHandler;
import com.minecolonies.api.inventory.InventoryCitizen;
import com.minecolonies.api.items.IMinecoloniesFoodItem;
import com.minecolonies.api.util.FoodUtils;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingCook;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.Set;

@Mixin(FoodUtils.class)
public class FoodUtilsMixin {
    @Unique private static final double foodPunisher = PathingConfig.FOOD_PUNISHER.get();
    @Unique private static final double foodBonusNormal = PathingConfig.FOOD_BONUS_NORMAL.get();
    @Unique private static final double foodBonusMinecolonies = PathingConfig.FOOD_BONUS_MINECOLONIES.get();

    @Inject(remap = false,method = "getFoodValue(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/food/FoodProperties;D)D",at = @At("RETURN"), cancellable = true)
    private static void resetFoodBonus(ItemStack foodStack, FoodProperties itemFood, double researchBonus, CallbackInfoReturnable<Double> cir){
        double noPunish = foodStack.getItem() instanceof IMinecoloniesFoodItem ? 1 : 4 * foodPunisher;
        double bonus = itemFood.getSaturationModifier() * itemFood.getNutrition() * (foodStack.getItem() instanceof IMinecoloniesFoodItem ? foodBonusMinecolonies : foodBonusNormal);
        cir.setReturnValue(cir.getReturnValueD() * noPunish + bonus);
    }

    /**
     * @author ARxyt
     * @reason 和我修改目标出现冲突，取消在此处进行需要在餐厅进食的检测，这里只返回最好的食物。
     */
    @Overwrite(remap = false)
    public static int getBestFoodForCitizen(final InventoryCitizen inventoryCitizen, final ICitizenData citizenData, @Nullable final Set<ItemStorage> menu) {
        // Smaller score is better.
        int bestScore = Integer.MAX_VALUE;
        int bestSlot = -1;

        final ICitizenFoodHandler foodHandler = citizenData.getCitizenFoodHandler();
        final ICitizenFoodHandler.CitizenFoodStats foodStats = foodHandler.getFoodHappinessStats();
        final int diversityRequirement = FoodUtils.getMinFoodDiversityRequirement(citizenData.getHomeBuilding() == null ? 0 : citizenData.getHomeBuilding().getBuildingLevelEquivalent());
        final int qualityRequirement = FoodUtils.getMinFoodQualityRequirement(citizenData.getHomeBuilding() == null ? 0 : citizenData.getHomeBuilding().getBuildingLevelEquivalent());
        for (int i = 0; i < inventoryCitizen.getSlots(); i++)
        {
            final ItemStorage invStack = new ItemStorage(inventoryCitizen.getStackInSlot(i));
            if ((menu == null || menu.contains(invStack)) && FoodUtils.canEat(invStack.getItemStack(), citizenData.getHomeBuilding(), citizenData.getWorkBuilding()))
            {
                final boolean isMinecolfood = invStack.getItem() instanceof IMinecoloniesFoodItem;
                final int localScore = foodHandler.checkLastEaten(invStack.getItem()) * (isMinecolfood ? 1 : 2);
                if (menu == null && foodHandler.getLastEaten() == invStack.getItem())
                {
                    continue;
                }

                // If the quality and diversity requirement would be fulfilled, already go ahead with this food. Don't need to check others.
                if ((localScore < 0 && isMinecolfood)
                        || (localScore < 0 && foodStats.quality() > qualityRequirement * 2)
                        || (isMinecolfood && foodStats.diversity() > diversityRequirement * 2))
                {
                    return i;
                }

                if (localScore < bestScore)
                {
                    bestScore = localScore;
                    bestSlot = i;
                }
            }
        }
        return bestSlot;
    }
}
