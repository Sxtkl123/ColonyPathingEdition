package com.arxyt.colonypathingedition.core.mixins.food;

import com.minecolonies.api.items.IMinecoloniesFoodItem;
import com.minecolonies.api.util.FoodUtils;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FoodUtils.class)
public class FoodUtilsMixin {

    @Inject(remap = false,method = "getFoodValue(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/food/FoodProperties;D)D",at = @At("RETURN"), cancellable = true)
    private static void resetFoodBonus(ItemStack foodStack, FoodProperties itemFood, double researchBonus, CallbackInfoReturnable<Double> cir){
        double noPunish = 1;
        if (!ModList.get().isLoaded("easycolony")) {
            noPunish = foodStack.getItem() instanceof IMinecoloniesFoodItem ? 1 : 4;
        }
        double bonus = foodStack.getItem() instanceof IMinecoloniesFoodItem ? itemFood.getSaturationModifier() * itemFood.getNutrition() * 0.5 : itemFood.getSaturationModifier() * itemFood.getNutrition() * 0.2;
        cir.setReturnValue(cir.getReturnValueD() * noPunish + bonus);
    }
}
