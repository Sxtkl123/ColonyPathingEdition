package com.arxyt.colonypathingedition.core.mixins;

import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.arxyt.colonypathingedition.core.mixins.accessor.AbstractEntityAIBasicAccessor;
import com.arxyt.colonypathingedition.core.util.DistanceUtils;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.equipment.ModEquipmentTypes;
import com.minecolonies.api.equipment.registry.EquipmentTypeEntry;
import com.minecolonies.api.util.BlockPosUtil;
import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.api.util.StatsUtil;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.colony.buildings.modules.AnimalHerdingModule;
import com.minecolonies.core.colony.jobs.AbstractJob;
import com.minecolonies.core.colony.jobs.JobLumberjack;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIInteract;
import com.minecolonies.core.entity.ai.workers.production.herders.AbstractEntityAIHerder;
import com.minecolonies.core.util.citizenutils.CitizenItemUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.*;
import java.util.function.Predicate;

@Mixin(AbstractEntityAIHerder.class)
public abstract class AbstractEntityAIHerderMixin<J extends AbstractJob<?, J>, B extends AbstractBuilding>
        extends AbstractEntityAIInteract<J, B>
{
    @Final @Shadow(remap = false) private Map<UUID, Long> fedRecently;
    @Shadow(remap = false) protected AnimalHerdingModule current_module;


    @Shadow(remap = false) public abstract int getMaxAnimalMultiplier();
    @Shadow(remap = false) public abstract boolean walkingToAnimal(Animal animal);
    @Shadow(remap = false) public abstract List<? extends Animal> searchForAnimals(Predicate<Animal> predicate);
    @Shadow(remap = false) public abstract boolean equipTool(InteractionHand hand, EquipmentTypeEntry toolType);


    @Unique final private boolean isMaxAnimalChange = PathingConfig.MAX_ANIMAL_MODIFIER.get();

    public AbstractEntityAIHerderMixin(@NotNull J job) {
        super(job);
        throw new RuntimeException("AbstractEntityAIHerderMixin 类不应被实例化！");
    }

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
        return 10;
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
        int maxAnimals = (int) Math.pow(getMaxAnimalMultiplier(),building.getBuildingLevel());
        int minAnimals = getMaxAnimalMultiplier() * building.getBuildingLevel();
        if (!isMaxAnimalChange){
            maxAnimals = minAnimals;
        }
        // 如果没开启繁殖设置，且动物总数未超过上限，则不屠宰
        if (!building.getSetting(AbstractBuilding.BREEDING).getValue()
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

    @Unique private Animal toKill = null;

    /**
     * @author ARxyt
     * @reason 屠宰的检测/击杀方式/寻路要求改变，理应没有冲突，方便起见暂时使用 Overwrite.
     */
    @Overwrite(remap = false)
    protected IAIState butcherAnimals() {
        if (this.current_module == null)
        {
            return AIWorkerState.DECIDE;
        }

        Objects.requireNonNull(this.current_module); List<? extends Animal> animals = searchForAnimals(this.current_module::isCompatible);

        if (!equipTool(InteractionHand.MAIN_HAND, (EquipmentTypeEntry) ModEquipmentTypes.axe.get()))
        {
            return AIWorkerState.START_WORKING;
        }

        if (animals.isEmpty())
        {
            return AIWorkerState.DECIDE;
        }

        BlockPos center = this.worker.blockPosition();
        if (toKill == null || !toKill.isAlive()) {
            animals.sort(Comparator.<Animal>comparingDouble(an -> an.blockPosition().distSqr((Vec3i) center)));

            Animal decideToKill = null;
            for (Animal entity : animals) {
                if (!entity.isBaby() && !entity.isInLove() && (decideToKill == null || decideToKill.getHealth() > entity.getHealth())) {
                    decideToKill = entity;
                }
            }
            toKill = decideToKill;
        }

        if (toKill == null ) {
            return AIWorkerState.DECIDE;
        }

        walkingToAnimal(toKill);
        if (BlockPosUtil.getDistance2D(center,toKill.blockPosition()) < 4 && !ItemStackUtils.isEmpty(this.worker.getMainHandItem())) {
            this.worker.swing(InteractionHand.MAIN_HAND);
            DamageSource ds = toKill.level().damageSources().playerAttack((Player)getFakePlayer());
            toKill.hurt(ds, PathingConfig.BUTCHER_INSTANT_KILL.get()? Float.MAX_VALUE : 3.0F * building.getBuildingLevel());
            CitizenItemUtils.damageItemInHand(this.worker, InteractionHand.MAIN_HAND, 1);
        }

        if (!toKill.isAlive()) {
            StatsUtil.trackStat((IBuilding)this.building, "animals_butchered", 1);
            this.worker.getCitizenExperienceHandler().addExperience(0.5D);
            incrementActionsDoneAndDecSaturation();
            this.fedRecently.remove(toKill.getUUID());
            return AIWorkerState.DECIDE;
        }

        return AIWorkerState.HERDER_BUTCHER;
    }

}
