package com.arxyt.colonypathingedition.core.mixins.food;

import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.minecolonies.api.items.IMinecoloniesFoodItem;
import com.minecolonies.api.util.FoodUtils;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
}
