package com.arxyt.colonypathingedition.core.mixins.food;

import com.google.common.collect.EvictingQueue;
import com.minecolonies.api.entity.citizen.citizenhandlers.ICitizenFoodHandler;
import com.minecolonies.api.items.IMinecoloniesFoodItem;
import com.minecolonies.core.entity.citizen.citizenhandlers.CitizenFoodHandler;
import net.minecraft.util.Mth;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Set;

@Mixin(CitizenFoodHandler.class)
public class CitizenFoodHandlerMixin {
    @Final @Shadow(remap = false) private EvictingQueue<Item> lastEatenFoods;
    @Shadow(remap = false) private ICitizenFoodHandler.CitizenFoodStats foodStatCache;
    @Shadow(remap = false) private boolean dirty;

    @Inject(remap = false,method ="getFoodHappinessStats" ,at = @At("HEAD"), cancellable = true)
    public void rewriteGetFoodHappinessStats(CallbackInfoReturnable<ICitizenFoodHandler.CitizenFoodStats> cir)
    {
        if (foodStatCache == null || dirty)
        {
            float qualityFoodCounter = 0;
            float diversityFoodCounter = 0;
            Set<Item> uniqueFoods = new HashSet<>();
            for (final Item foodItem : lastEatenFoods)
            {
                if (foodItem instanceof IMinecoloniesFoodItem)
                {
                    qualityFoodCounter += 1;
                }
                else{
                    FoodProperties foodProperties=foodItem.getFoodProperties(new ItemStack(foodItem),null);
                    if(foodProperties != null){
                        qualityFoodCounter += foodProperties.getSaturationModifier();
                    }
                }
                uniqueFoods.add(foodItem);
            }
            for (final Item foodItem : uniqueFoods){
                if (foodItem instanceof IMinecoloniesFoodItem)
                {
                    diversityFoodCounter += 1;
                }
                else{
                    FoodProperties foodProperties=foodItem.getFoodProperties(new ItemStack(foodItem),null);
                    if(foodProperties != null){
                        diversityFoodCounter += Math.max(foodProperties.getSaturationModifier() * 2.0f , 1.0f);
                    }
                }
            }
            foodStatCache = new ICitizenFoodHandler.CitizenFoodStats(Mth.ceil(qualityFoodCounter), Mth.ceil(diversityFoodCounter));
        }
        cir.setReturnValue(foodStatCache);
    }

}
