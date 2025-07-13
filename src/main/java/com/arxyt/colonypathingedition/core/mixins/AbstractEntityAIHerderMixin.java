package com.arxyt.colonypathingedition.core.mixins;

import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.arxyt.colonypathingedition.core.mixins.accessor.AbstractEntityAIBasicAccessor;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.entity.ai.workers.production.herders.AbstractEntityAIHerder;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import java.util.List;

@Mixin(AbstractEntityAIHerder.class)
public abstract class AbstractEntityAIHerderMixin implements AbstractEntityAIBasicAccessor<AbstractBuilding>
{
    @Shadow(remap = false) public abstract int getMaxAnimalMultiplier();

    @Unique final private boolean isMaxAnimalChange = PathingConfig.MAX_ANIMAL_MODIFIER.get();

    // 修改 registerTargets 中使用的 DECIDING_DELAY
    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 80))
    private static int modifyDecidingDelayInInit(int original) {
        return 10;
    }

    // 修改 registerTargets 中使用的 BREEDING_DELAY
    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 40))
    private static int modifyBreedingDelayInInit(int original) {
        return 10;
    }

    // 修改 registerTargets 中使用的 BUTCHER_DELAY
    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 20))
    private static int modifyButcherDelayInInit(int original) {
        return 5;
    }

    // 修改 decideWhatToDo 中减少 breedTimeOut 的 DECIDING_DELAY
    @ModifyConstant(method = "decideWhatToDo", constant = @Constant(intValue = 80),remap = false)
    private static int modifyDecidingDelayInDecideWhatToDo(int original) {
        return 10;
    }

    /**
     * @author ARxyt
     * @reason 写的太狗屎，重构一下
     */
    @Overwrite(remap = false)
    public double chanceToButcher(final List<? extends Animal> allAnimals)
    {
        // 用幂运算替代原先的乘法
        int maxAnimals = (int) Math.pow(getMaxAnimalMultiplier(),getBuilding().getBuildingLevel());
        int minAnimals = getMaxAnimalMultiplier() * getBuilding().getBuildingLevel();
        if (!isMaxAnimalChange){
            maxAnimals = minAnimals;
        }
        // 如果没开启繁殖设置，且动物总数未超过上限，则不屠宰
        if (!getBuilding().getSetting(AbstractBuilding.BREEDING).getValue()
                && allAnimals.size() <= maxAnimals)
        {
            return 0;
        }

        // 统计所有成年动物
        int grownUp = 0;
        for (Animal animal : allAnimals)
        {
            if (!animal.isBaby())
            {
                grownUp++;
            }
        }

        // 成年动物太少时不屠宰
        if (grownUp <= minAnimals)
        {
            return 0;
        }

        return Math.pow(grownUp - minAnimals, 4) / Math.pow(maxAnimals - minAnimals + 1, 4);
    }
}
